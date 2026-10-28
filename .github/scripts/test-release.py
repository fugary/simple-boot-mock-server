"""Exercise the actual workflow scripts against local temporary Git repositories.

Run with Python, PyYAML and Git Bash (on Windows): python .github/scripts/test-release.py
"""

import io
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
from urllib.error import HTTPError

import yaml


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = yaml.safe_load((ROOT / '.github/workflows/release.yml').read_text(encoding='utf-8'))
STEPS = {step['name']: step for step in WORKFLOW['jobs']['build-and-release']['steps']}
BASH = os.environ.get('RELEASE_TEST_BASH') or (
    str(Path(shutil.which('git')).resolve().parents[1] / 'bin/bash.exe')
    if os.name == 'nt' else shutil.which('bash')
)


class ReleaseTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='release-test-', ignore_cleanup_errors=True)
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.repo = self.base / 'repo'
        self.repo.mkdir()
        self.remote = self.base / 'remote.git'
        self.git('init', '--bare', str(self.remote))
        self.git('init', '-b', 'master')
        self.git('config', 'user.name', 'Release Test')
        self.git('config', 'user.email', 'release-test@example.invalid')
        self.git('config', 'commit.gpgsign', 'false')
        self.git('config', 'tag.gpgsign', 'false')
        self.git('config', 'core.hooksPath', str(self.base / 'hooks'))
        self.commit('feat: initial feature')
        self.git('tag', 'v2.9.5')
        self.commit('feat: current release')
        self.release_sha = self.git('rev-parse', 'HEAD')
        self.git('tag', '-a', 'v2.9.6', '-m', 'Release 2.9.6')
        self.commit('fix: small bug')
        self.source_sha = self.git('rev-parse', 'HEAD')
        self.git('remote', 'add', 'origin', str(self.remote))
        self.git('push', '-u', 'origin', 'master', '--tags')
        self.env = dict(os.environ, INPUT_VERSION='2.9.6', OVERWRITE_EXISTING='true',
                        GITHUB_EVENT_NAME='workflow_dispatch', GITHUB_REF='refs/heads/master',
                        GITHUB_REF_NAME='master', GITHUB_SHA=self.source_sha,
                        GITHUB_RUN_ID='12345', GITHUB_RUN_ATTEMPT='1',
                        GITHUB_REPOSITORY='example/repo', GITHUB_SERVER_URL='https://github.com')
        self.state = {}

    def git(self, *args):
        return subprocess.check_output(['git', *args], cwd=self.repo, stderr=subprocess.PIPE,
                                       encoding='utf-8').strip()

    def commit(self, subject):
        (self.repo / 'feature.txt').write_text(subject, encoding='utf-8')
        self.git('add', '.')
        self.git('commit', '-m', subject)

    def step(self, name, success=True, **overrides):
        output = self.base / 'output'
        output.write_text('')
        env = dict(self.env, GITHUB_OUTPUT=str(output))
        env.update({key.upper(): value for key, value in self.state.items()})
        env.update(overrides)
        python = sys.executable.replace('\\', '/')
        script = STEPS[name]['run'].replace('python3 -', f'"{python}" -')
        result = subprocess.run([BASH, '-e', '-o', 'pipefail', '-c', script], cwd=self.repo,
                                env=env, text=True, encoding='utf-8', capture_output=True)
        self.assertEqual(result.returncode == 0, success, result.stdout + result.stderr)
        values = dict(line.split('=', 1) for line in output.read_text().splitlines())
        if success and name == 'Prepare release':
            self.state = values
        return values

    def publish(self):
        self.step('Prepare release')
        self.assertEqual(self.state['create_tag'], 'false')
        self.step('Configure Git identity')
        published = self.step('Replace existing release tag')
        self.step('Check published tag matches build')
        return published

    def remote_tag(self):
        return self.git('--git-dir', str(self.remote), 'rev-parse', 'refs/tags/v2.9.6')

    def write_versions(self, version, root_version=None):
        for name in ('simple-boot-mock-server', 'simple-boot-mock-newui'):
            (self.repo / name).mkdir(exist_ok=True)
        namespace = 'http://maven.apache.org/POM/4.0.0'
        (self.repo / 'pom.xml').write_text(
            f'<project xmlns="{namespace}"><version>{root_version or version}</version></project>')
        (self.repo / 'simple-boot-mock-server/pom.xml').write_text(
            f'<project xmlns="{namespace}"><parent><version>{version}</version></parent></project>')
        for name in ('package.json', 'package-lock.json'):
            (self.repo / 'simple-boot-mock-newui' / name).write_text(json.dumps({
                'version': version, 'packages': {'': {'version': version}},
            }))

    def test_workflow_syntax_and_default(self):
        # BaseLoader keeps YAML's "on" key as a string.
        workflow = yaml.load((ROOT / '.github/workflows/release.yml').read_text(encoding='utf-8'),
                             Loader=yaml.BaseLoader)
        self.assertEqual(workflow['on']['workflow_dispatch']['inputs']['overwrite_existing']['default'], 'false')
        for step in STEPS.values():
            if 'run' in step:
                result = subprocess.run([BASH, '-n'], input=step['run'], text=True, encoding='utf-8', capture_output=True)
                self.assertEqual(result.returncode, 0, f'{step["name"]}: {result.stderr}')

    def test_overwrite_and_retry_preserve_source_and_audit(self):
        original_oid = self.remote_tag()
        published = self.publish()
        self.assertNotEqual(self.remote_tag(), original_oid)
        self.assertEqual(self.git('--git-dir', str(self.remote), 'rev-parse', 'v2.9.6^{commit}'), self.source_sha)
        self.assertEqual(self.git('--git-dir', str(self.remote), 'rev-parse', 'master'), self.source_sha)
        self.assertEqual(self.state['previous_commit'], self.release_sha)
        result = subprocess.run([BASH, '-e', '-o', 'pipefail', '-c', STEPS['Generate Changelog']['run']],
                                cwd=self.repo, env=dict(self.env, CURRENT_TAG='v2.9.6',
                                                       GITHUB_OUTPUT=str(self.base / 'changelog-output')),
                                text=True, encoding='utf-8', capture_output=True)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        body = (self.repo / 'changelog.txt').read_text(encoding='utf-8')
        self.assertTrue(body.startswith("## What's Changed\n"))
        for text in ('同版本重新发布', '发布时间', '原提交', '构建提交', '本次修复',
                     published['republished_at']):
            self.assertNotIn(text, body)
        self.assertIn('### 🐛 Bug Fixes\n1. [fix: small bug]', body)
        self.assertEqual(body.count('fix: small bug'), 1)
        self.assertIn(self.release_sha, body)
        self.assertIn(self.source_sha, body)
        tag_message = self.git('for-each-ref', '--format=%(contents)', 'refs/tags/v2.9.6')
        self.assertIn(f'Release-Run: {self.env["GITHUB_RUN_ID"]}', tag_message)
        self.assertIn(f'Previous-Commit: {self.release_sha}', tag_message)
        self.assertIn(f'Republished-At: {published["republished_at"]}', tag_message)
        moved_oid = self.remote_tag()
        # master can advance after the tag was moved; a retry still builds the original source.
        self.commit('fix: later change')
        self.git('push', 'origin', 'master')
        self.git('checkout', '--detach', self.source_sha)
        self.env['GITHUB_RUN_ATTEMPT'] = '2'
        self.step('Prepare release')
        self.assertEqual(self.state['replace_tag'], 'false')
        self.assertEqual(self.state['previous_commit'], self.release_sha)
        self.assertEqual(self.step('Replace existing release tag'), published)
        self.assertEqual(self.remote_tag(), moved_oid)

    def test_newer_remote_tag_during_build_blocks_overwrite(self):
        self.step('Prepare release')
        original_oid = self.remote_tag()
        # Simulate an external push: the new tag is absent from this checkout.
        self.git('--git-dir', str(self.remote), 'tag', 'v2.10.0', self.source_sha)
        self.step('Replace existing release tag', success=False)
        self.assertEqual(self.remote_tag(), original_oid)

    def test_invalid_overwrite_requests(self):
        cases = [dict(INPUT_VERSION='2.9.5'), dict(INPUT_VERSION='2.9.7'),
                 dict(GITHUB_REF='refs/heads/feature'), dict(OVERWRITE_EXISTING='false')]
        for values in cases:
            with self.subTest(values=values):
                self.step('Prepare release', success=False, **values)

    def test_version_mismatch_does_not_change_tag(self):
        self.step('Prepare release')
        self.write_versions('2.9.6', root_version='2.9.7')
        oid = self.remote_tag()
        self.step('Check version consistency', success=False)
        self.assertEqual(self.remote_tag(), oid)

    def test_new_version_and_manual_tag_still_work(self):
        self.env.update(OVERWRITE_EXISTING='false', INPUT_VERSION='2.9.7')
        self.step('Prepare release')
        self.assertEqual(self.state['create_tag'], 'true')
        self.write_versions('2.9.7')
        self.step('Check version consistency')
        self.step('Configure Git identity')
        self.step('Commit version and push tag')
        self.step('Check published tag matches build')
        version_sha = self.git('rev-parse', 'HEAD')
        self.assertEqual(self.git('--git-dir', str(self.remote), 'rev-parse', 'master'), version_sha)
        self.env['GITHUB_RUN_ATTEMPT'] = '2'
        self.step('Prepare release')
        self.assertEqual(self.state['create_tag'], 'false')
        self.assertEqual(self.git('rev-parse', 'HEAD'), version_sha)
        self.step('Prepare release', INPUT_VERSION='', GITHUB_EVENT_NAME='push',
                  GITHUB_REF='refs/tags/v2.9.7', GITHUB_REF_NAME='v2.9.7')
        self.step('Check published tag matches build')

    def test_master_changed_during_build(self):
        self.step('Prepare release')
        oid = self.remote_tag()
        self.commit('fix: concurrent master change')
        self.git('push', 'origin', 'master')
        self.step('Replace existing release tag', success=False)
        self.assertEqual(self.remote_tag(), oid)

    def test_tag_changed_during_build(self):
        self.step('Prepare release')
        self.git('tag', '-f', '-a', 'v2.9.6', '-m', 'Concurrent tag update')
        self.git('push', '--force', 'origin', 'refs/tags/v2.9.6')
        oid = self.remote_tag()
        self.step('Replace existing release tag', success=False)
        self.git('tag', '-f', 'v2.9.6', self.release_sha)
        self.step('Check published tag matches build', success=False)
        self.assertEqual(self.remote_tag(), oid)

    def test_stale_tag_trigger_cannot_publish_old_source(self):
        self.publish()
        self.git('checkout', '--detach', self.release_sha)
        self.step('Prepare release', success=False, OVERWRITE_EXISTING='false', INPUT_VERSION='',
                  GITHUB_EVENT_NAME='push', GITHUB_REF='refs/tags/v2.9.6', GITHUB_REF_NAME='v2.9.6')
        self.git('checkout', '--detach', self.source_sha)
        self.step('Prepare release', OVERWRITE_EXISTING='false', INPUT_VERSION='',
                  GITHUB_EVENT_NAME='push', GITHUB_REF='refs/tags/v2.9.6', GITHUB_REF_NAME='v2.9.6')

    def test_source_cannot_go_backwards(self):
        self.commit('fix: newer release source')
        self.git('tag', '-f', 'v2.9.6')
        self.git('checkout', '--detach', self.source_sha)
        self.step('Prepare release', success=False)

    def test_protected_tag_rejection_preserves_remote(self):
        self.step('Prepare release')
        oid = self.remote_tag()
        hooks = self.remote / 'hooks'
        self.git('--git-dir', str(self.remote), 'config', 'core.hooksPath', str(hooks))
        hook = hooks / 'update'
        hook.write_text('#!/bin/sh\nexit 1\n', encoding='utf-8')
        hook.chmod(0o755)
        self.step('Replace existing release tag', success=False)
        self.assertEqual(self.remote_tag(), oid)

    def test_newer_version_blocks_overwrite_retry(self):
        self.publish()
        self.git('tag', 'v2.10.0')
        self.env['GITHUB_RUN_ATTEMPT'] = '2'
        self.step('Prepare release', success=False)

    def test_release_api_preflight(self):
        code = STEPS['Check release can be overwritten']['run'].split("<<'PY'\n", 1)[1].rsplit('\nPY', 1)[0]
        env = dict(self.env, GH_TOKEN='test-token', GITHUB_API_URL='https://api.example.invalid', TAG='v2.9.6')
        with patch.dict(os.environ, env):
            with patch('urllib.request.urlopen', return_value=io.StringIO('{"immutable": false}')):
                exec(compile(code, '<release-preflight>', 'exec'), {})
            with patch('urllib.request.urlopen', return_value=io.StringIO('{"immutable": true}')):
                with self.assertRaises(SystemExit):
                    exec(compile(code, '<release-preflight>', 'exec'), {})
            for status in (404, 403):
                with patch('urllib.request.urlopen', side_effect=HTTPError('test', status, 'test', {}, None)):
                    if status == 404:
                        exec(compile(code, '<release-preflight>', 'exec'), {})
                    else:
                        with self.assertRaises(HTTPError):
                            exec(compile(code, '<release-preflight>', 'exec'), {})


if __name__ == '__main__':
    unittest.main()
