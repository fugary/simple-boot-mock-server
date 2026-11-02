<script setup>
import { computed, nextTick, onActivated, onMounted, ref } from 'vue'
import { loadAiHelpStatus } from '@/api/mock/AiApi'
import { $i18nBundle, $i18nMsg } from '@/messages'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import MockAiHelp from '@/views/components/mock/MockAiHelp.vue'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'

const currentTab = defineModel({
  type: String,
  default: 'guide'
})

const aiHelpAvailable = ref(false)
let checkingAiHelp = false
const refreshAiHelpStatus = async () => {
  if (checkingAiHelp) return
  checkingAiHelp = true
  try {
    const response = await loadAiHelpStatus()
    aiHelpAvailable.value = response.success && response.resultData === true
  } catch {
    aiHelpAvailable.value = false
  } finally {
    checkingAiHelp = false
    if (!aiHelpAvailable.value && currentTab.value === 'aiHelp') currentTab.value = 'guide'
  }
}
onMounted(refreshAiHelpStatus)
onActivated(refreshAiHelpStatus)

const requestFields = [
  { field: 'request.body', desc: 'body内容对象', descEn: 'Body content object' },
  { field: 'request.bodyStr', desc: 'body内容字符串', descEn: 'Body content as a string' },
  { field: 'request.headers', desc: '头信息对象', descEn: 'Header information object' },
  { field: 'request.parameters', desc: '请求参数对象', descEn: 'Request parameters object' },
  { field: 'request.pathParameters', desc: '路径参数对象', descEn: 'Path parameters object' },
  { field: 'request.params', desc: '请求参数和路径参数合并', descEn: 'Merged object of request parameters and path parameters' },
  { field: 'request.ip', desc: '客户端IP', descEn: 'Client IP address' },
  { field: 'request.userAgent', desc: 'User-Agent', descEn: 'User-Agent' },
  { field: 'request.cookies', desc: 'Cookie对象', descEn: 'Cookie object' }
]

const responseFields = [
  { field: 'response.statusCode', desc: '状态码', descEn: 'Status Code' },
  { field: 'response.body', desc: 'body内容对象', descEn: 'Body content object' },
  { field: 'response.bodyStr', desc: 'body内容字符串', descEn: 'Body content as a string' },
  { field: 'response.headers', desc: '响应头信息对象', descEn: 'Response header information object' }
]
const internalFunctions = [
  { method: 'Mock.mock(data)', desc: 'MockJS支持', descEn: 'MockJS support' },
  { method: 'dayjs(...args)', desc: 'dayjs支持', descEn: 'dayjs support' },
  { method: 'fetch(url, options)', desc: 'fetch支持，async/await异步函数支持', descEn: 'fetch support, async/await functions support' },
  {
    method: 'require(url, options)',
    desc: '提供一个异步版的 CommonJS 风格 require 方法，用于动态加载第三方库。支持 module.exports 和 exports 形式的模块导出，不支持 ESM (export / import) 模块。返回 Promise，可用于异步获取远程或本地模块内容。',
    descEn: 'Provides an asynchronous CommonJS-style require method for dynamically loading third-party libraries. Supports modules exported via module.exports and exports, but does not support ESM (export / import) modules. Returns a Promise that resolves to the loaded module’s exports.'
  },
  {
    method: 'clearRequireCache',
    desc: '清空 require 函数的缓存。默认情况下，require 函数会缓存已加载过的 URL 模块数据，避免重复加载。',
    descEn: 'Clears the cache of the require function. By default, the require function caches loaded module data by URL to avoid redundant requests.'
  },
  { method: 'xml2Json(xmlStr)', desc: '将XML字符串转换成JSON格式', descEn: 'Convert an XML string into JSON format' },
  { method: 'json2Xml(jsonStr, rootName)', desc: '将JSON字符串转换成XML格式', descEn: 'Convert an JSON string into XML format' },
  { method: 'decodeHex(hex)', desc: '将十六进制字符串解码为普通字符串', descEn: 'Decode hexadecimal string to normal string' },
  { method: 'encodeHex(data)', desc: '将普通字符串编码为十六进制格式', descEn: 'Encode normal string to hexadecimal format' },
  { method: 'md5Hex(data)', desc: '对数据进行 MD5 加密，输出十六进制格式', descEn: 'MD5 encrypt data, output hexadecimal format' },
  { method: 'md5Base64(data)', desc: '对数据进行 MD5 加密，输出 Base64 格式', descEn: 'MD5 encrypt data, output Base64 format' },
  { method: 'sha1Hex(data)', desc: '对数据进行 SHA1 加密，输出十六进制格式', descEn: 'SHA1 encrypt data, output hexadecimal format' },
  { method: 'sha1Base64(data)', desc: '对数据进行 SHA1 加密，输出 Base64 格式', descEn: 'SHA1 encrypt data, output Base64 format' },
  { method: 'sha256Hex(data)', desc: '对数据进行 SHA256 加密，输出十六进制格式', descEn: 'SHA256 encrypt data, output hexadecimal format' },
  { method: 'sha256Base64(data)', desc: '对数据进行 SHA256 加密，输出 Base64 格式', descEn: 'SHA256 encrypt data, output Base64 format' },
  { method: 'btoa(data)', desc: '将数据编码为 Base64 格式', descEn: 'Encode data to Base64 format' },
  { method: 'atob(data)', desc: '从 Base64 格式解码为原始数据', descEn: 'Decode data from Base64 format' },
  { method: 'encryptAES(data, key, options)', desc: '使用 AES 加密数据，输出 Base64 格式', descEn: 'Encrypt data using AES, output Base64 format' },
  { method: 'encryptDES(data, key, options)', desc: '使用 DES 加密数据，输出 Base64 格式', descEn: 'Encrypt data using DES, output Base64 format' },
  { method: 'encrypt3DES(data, key, options)', desc: '使用 3DES 加密数据，输出 Base64 格式', descEn: 'Encrypt data using 3DES, output Base64 format' },
  { method: 'encryptSM4(data, key, options)', desc: '使用 SM4 加密数据，输出 Base64 格式', descEn: 'Encrypt data using SM4, output Base64 format' },
  { method: 'encryptRSA(data, key, options)', desc: '使用 RSA 加密数据，输出 Base64 格式', descEn: 'Encrypt data using RSA, output Base64 format' },
  { method: 'signRSA(data, key, options)', desc: '使用 RSA 私钥签名数据，输出 Base64 格式', descEn: 'Sign data using RSA private key, output Base64 format' },
  { method: 'verifyRSA(data, signature, key, options)', desc: '使用 RSA 公钥验证签名', descEn: 'Verify RSA signature using public key' },
  { method: 'decryptAES(data, key, options)', desc: '使用 AES 解密数据', descEn: 'Decrypt data using AES' },
  { method: 'decryptDES(data, key, options)', desc: '使用 DES 解密数据', descEn: 'Decrypt data using DES' },
  { method: 'decrypt3DES(data, key, options)', desc: '使用 3DES 解密数据', descEn: 'Decrypt data using 3DES' },
  { method: 'decryptSM4(data, key, options)', desc: '使用 SM4 解密数据', descEn: 'Decrypt data using SM4' },
  { method: 'decryptRSA(data, key, options)', desc: '使用 RSA 解密数据', descEn: 'Decrypt data using RSA' }
]

const requestColumns = computed(() => {
  return [
    { label: 'Property Name', prop: 'field' },
    { labelKey: 'common.label.description', prop: $i18nMsg('desc', 'descEn') }
  ]
})

const functionColumns = computed(() => {
  return [
    { label: 'Function Name', prop: 'method' },
    { labelKey: 'common.label.description', prop: $i18nMsg('desc', 'descEn') }
  ]
})

const { languageRef, monacoEditorOptions } = useMonacoEditorOptions({
  readOnly: true,
  scrollbar: {
    vertical: 'hidden'
  },
  wordWrap: 'off'
})
languageRef.value = 'javascript'

const exampleGroups = computed(() => [{
  name: 'basic',
  label: $i18nBundle('mock.label.basicExamples'),
  examples: [{
    label: 'JSON Content',
    content: `
{
    "id": 1927,
    "name": "Betty Hall",
    "birthday": "2024-10-18",
    "gender": "未知"
}
  `.trim()
  }, {
    label: 'Mock.js',
    link: 'http://mockjs.com/',
    content: `
Mock.mock({
    "code": "success",
    'data|10': [{
        "id": "@integer(1, 10000)",
        "name": "@name",
        "birthday": "@date",
        "gender": "@pick(['男', '女', '未知'])"
    }]
})
`.trim()
  }, {
    label: $i18nBundle('mock.msg.functionTooltip'),
    content: `
(function () {
    const data = [];
    for (let i = 0; i < 10; i++) {
        data.push(Mock.mock({
            "id": "@integer(1, 10000)",
            "name": "@name",
            "birthday": "@date",
            "gender": "@pick(['男', '女', '未知'])"
        }))
    }
    return {
        code: 'success',
        data
    };
}())
  `.trim()
  }, {
    label: $i18nBundle('mock.label.requestObject'),
    content: `
Mock.mock({
  ip: request.ip,
  userAgent: request.userAgent,
  headers: request.headers,
  body: request.body,
  params: request.params
})
`.trim()
  }, {
    label: 'Event Stream Array',
    content: `
[{
  "data": "连接成功",
  "delay": 0
}, {
  "data": {"msg": "这是一条消息", "id": 1},
  "delay": 1000
}, {
  "data": {"msg": "这是第二条消息", "id": 2},
  "delay": 1000
}, {
  "data": "结束",
  "delay": 500
}]
`.trim()
  }, {
    label: 'Event Stream Mock.js',
    content: `
(function () {
  const data = Mock.mock({
    "stream|3-6": [{
      "data": {
        "id": "@increment",
        "msg": function () {
          return \`这是ID为【\${this.id}】的消息\`
        }
      },
      "delay": 500
    }]
  });
  data.stream.push({
    data: { msg: '完成' },
    delay: 300
  });
  return data;
})
`.trim()
  }]
}, {
  label: $i18nBundle('mock.label.xmlExamples'),
  name: 'xmlExamples',
  examples: [{
    label: 'XML Content ( {{ dynamic js code }} is supported. )',
    language: 'xmlWithJs',
    content: `
<userAccount>
  <id>{{Mock.mock('@id')}}</id>
  <name gender="{{Mock.mock("@pick(['男','女','未知'])")}}">{{Mock.mock('@name')}}</name>
  <age>{{Mock.mock('@integer(18,35)')}}</age>
</userAccount>
`.trim()
  }, {
    label: 'XML Content (Generated by JavaScript)',
    content: `
(function () {
    let result = \`<root>
      <code>success</code>
      <data>
    \`;
    for (let i = 0; i < 10; i++) {
        let user = \`<user>
          <id>\${Mock.mock('@integer(1, 10000)')}</id>
          <name>\${Mock.mock('@name')}</name>
          <birthday>\${Mock.mock('@date')}</birthday>
          <gender>\${Mock.mock("@pick(['男', '女', '未知'])")}</birthday>
        </user>\`
        result += user;
    }
    result += \`<data></root>\`
    return result;
}())
`.trim()
  }]
}, {
  name: 'matchPattern',
  label: $i18nBundle('mock.label.matchPattern'),
  tooltip: $i18nBundle('mock.msg.matchPatternTooltip'),
  description: $i18nBundle('mock.msg.matchPatternDescription'),
  examples: [{
    label: 'Request parameters and headers matching',
    language: 'javascript',
    content: 'request.params.id==="1" || request.headers["custom-header-id"]==="test"'.trim()
  }, {
    label: 'Request body or bodyStr matching',
    language: 'javascript',
    content: 'request.body.id==="1" || request.bodyStr.includes("test")'.trim()
  }, {
    label: 'Match pattern function example',
    language: 'javascript',
    content: `
(function () {
    if (request.params.name==="admin" && request.params.password==="123456") {
        return true;
    }
    return false;
}())
`.trim()
  }]
}, {
  name: 'postProcessor',
  label: $i18nBundle('mock.label.postProcessor'),
  description: $i18nBundle('mock.msg.postProcessorDescription'),
  examples: [{
    label: 'Transform body XML to JSON',
    content: `
(function () {
  response.statusCode = 200; // modify status code
  response.headers['Content-Type'] = 'application/json'; // modify content type
  return xml2Json(response.bodyStr);
}())
`.trim()
  }, {
    label: 'Override response body',
    content: `
(async function () {
    const res = await fetch('https://httpbin.org/post', {
        method: 'post',
        body: JSON.stringify({ username: "example" })
    })
    const json = await res.json();
    response.headers['Content-Type'] = 'application/json';
    return json;
}())
`.trim()
  }]
}, {
  name: 'fetch',
  label: $i18nBundle('mock.label.fetchAndAsync'),
  examples: [{
    label: 'fetch JSON',
    content: `
(async function () {
    const response = await fetch('https://httpbin.org/put', {
        method: 'put',
        body: JSON.stringify({ username: "example" })
    })
    const json = await response.json();
    return json;
}())
`.trim()
  }, {
    label: 'fetch Image',
    content: `
(async function () {
    const response = await fetch('http://e.hiphotos.baidu.com/image/pic/item/a1ec08fa513d2697e542494057fbb2fb4316d81e.jpg');
    return await response.blob()
}())
`.trim()
  }, {
    label: 'require example',
    content: `
(async function () {
    const CryptoJS = await require('https://cdnjs.cloudflare.com/ajax/libs/crypto-js/4.2.0/crypto-js.min.js');
    const message = "test-message";
    const md5 = CryptoJS.MD5(message).toString(CryptoJS.enc.Hex);
    const sha256 = CryptoJS.SHA256(message).toString(CryptoJS.enc.Hex);
    return {
        md5,
        sha256
    };
}())
`.trim()
  }]
}])

const calcHeight = (text) => {
  return text.split('\n').length * 20 + 'px'
}

const quickStartSteps = ['Group', 'Request', 'Response', 'Test']
const quickStartResponse = `{
  "code": 0,
  "message": "ok",
  "data": { "id": 1, "name": "Alice" }
}`
const commonGuideSections = ['Response', 'Proxy', 'Import', 'Diagnostics']
const advancedGuideSections = ['Scenario', 'RoundRobin', 'Schema', 'Ai', 'Scripts']

const guideTopic = ref('QuickStart')
const guideGroups = [
  { title: 'guideCommonTitle', topics: commonGuideSections },
  { title: 'guideAdvancedTitle', topics: advancedGuideSections }
]
const guideLinks = { Response: ['basic'], Scripts: ['buildInObjects', 'buildIn', 'basic', 'matchPattern', 'postProcessor', 'fetch'] }
const tabLabels = computed(() => ({
  guide: $i18nBundle('mock.label.guideTitle'),
  buildInObjects: $i18nBundle('mock.label.buildInObjects'),
  buildIn: $i18nBundle('mock.label.buildInFunctions'),
  ...Object.fromEntries(exampleGroups.value.map(group => [group.name, group.label]))
}))
const currentTopic = computed(() => currentTab.value === 'guide'
  ? $i18nBundle('mock.label.guide' + guideTopic.value + 'Title')
  : tabLabels.value[currentTab.value])
const aiHelp = ref()
const askAboutTab = async () => {
  const question = $i18nBundle('mock.msg.aiHelpTopicPrompt', [currentTopic.value])
  currentTab.value = 'aiHelp'
  await nextTick()
  aiHelp.value?.prefillQuestion(question)
}

const exampleHeights = ref({})
const fitExampleHeight = (editor, key) => {
  const updateHeight = () => {
    exampleHeights.value[key] = `${editor.getContentHeight()}px`
  }
  editor.onDidContentSizeChange(updateHeight)
  updateHeight()
}
</script>

<template>
  <el-container
    class="flex-column mock-tips"
    style="height: calc(100% - 45px)"
  >
    <div
      v-if="aiHelpAvailable && currentTab !== 'aiHelp'"
      class="help-topic-action"
    >
      <el-button
        v-common-tooltip="$t('mock.label.aiHelpAboutTopic', [currentTopic])"
        :aria-label="$t('mock.label.aiHelpAboutTopic', [currentTopic])"
        text
        circle
        @click="askAboutTab"
      >
        <common-icon
          icon="AutoAwesomeOutlined"
          :size="18"
        />
      </el-button>
    </div>
    <el-tabs
      v-model="currentTab"
      class="tip-tabs"
      :class="{ 'ai-help-tabs': currentTab === 'aiHelp', 'has-topic-action': aiHelpAvailable && currentTab !== 'aiHelp' }"
    >
      <el-tab-pane
        v-if="aiHelpAvailable"
        :label="$t('mock.label.aiHelp')"
        name="aiHelp"
        class="ai-help-pane"
        lazy
      >
        <mock-ai-help
          ref="aiHelp"
          :active="currentTab === 'aiHelp'"
        />
      </el-tab-pane>
      <el-tab-pane
        :label="$i18nBundle('mock.label.guideTitle')"
        name="guide"
        lazy
      >
        <div class="user-guide">
          <nav
            class="guide-nav"
            :aria-label="$t('mock.label.guideTitle')"
          >
            <button
              type="button"
              :class="{ active: guideTopic === 'QuickStart' }"
              :aria-pressed="guideTopic === 'QuickStart'"
              @click="guideTopic = 'QuickStart'"
            >
              {{ $t('mock.label.guideQuickStartTitle') }}
            </button>
            <template
              v-for="group in guideGroups"
              :key="group.title"
            >
              <h4>{{ $t('mock.label.' + group.title) }}</h4>
              <button
                v-for="topic in group.topics"
                :key="topic"
                type="button"
                :class="{ active: guideTopic === topic }"
                :aria-pressed="guideTopic === topic"
                @click="guideTopic = topic"
              >
                {{ $t('mock.label.guide' + topic + 'Title') }}
              </button>
            </template>
          </nav>
          <section
            v-if="guideTopic === 'QuickStart'"
            class="guide-section"
          >
            <h3>{{ $t('mock.label.guideQuickStartTitle') }}</h3>
            <p class="guide-intro">
              {{ $t('mock.msg.guideIntroText') }}
            </p>
            <ol class="guide-steps">
              <li
                v-for="step in quickStartSteps"
                :key="step"
              >
                <strong>{{ $t('mock.label.guideStep' + step + 'Title') }}</strong>
                <p>{{ $t('mock.msg.guideStep' + step) }}</p>
              </li>
            </ol>
            <div class="guide-example">
              <div class="guide-example-title">
                <span>{{ $t('mock.label.guideSampleTitle') }}</span>
                <mock-url-copy-link :content="quickStartResponse" />
              </div>
              <vue-monaco-editor
                :value="quickStartResponse"
                language="json"
                :options="monacoEditorOptions"
                class="common-resize-vertical"
                :theme="useGlobalConfigStore().monacoTheme"
                :height="exampleHeights.quickStart || calcHeight(quickStartResponse)"
                @mount="editor => fitExampleHeight(editor, 'quickStart')"
              />
            </div>
            <div class="guide-result">
              <p>{{ $t('mock.msg.guideExpectedText') }}</p>
              <el-button
                type="primary"
                link
                @click="guideTopic = 'Diagnostics'"
              >
                {{ $t('mock.label.guideDiagnosticsTitle') }}
                <common-icon icon="ArrowRight" />
              </el-button>
            </div>
          </section>
          <section
            v-else
            :key="guideTopic"
            class="guide-section"
          >
            <h3>{{ $t('mock.label.guide' + guideTopic + 'Title') }}</h3>
            <p>{{ $t('mock.msg.guide' + guideTopic + 'Text') }}</p>
            <div class="guide-links">
              <el-button
                v-for="tab in guideLinks[guideTopic]"
                :key="tab"
                type="primary"
                plain
                @click="currentTab = tab"
              >
                {{ tabLabels[tab] }}
                <common-icon icon="ArrowRight" />
              </el-button>
            </div>
          </section>
        </div>
      </el-tab-pane>
      <el-tab-pane
        lazy
        :label="$t('mock.label.buildInObjects')"
        name="buildInObjects"
      >
        <p>
          {{ $t('mock.label.requestObject') }}
        </p>
        <common-table
          :data="requestFields"
          :columns="requestColumns"
        />
        <p>
          {{ $t('mock.label.responseObject') }}
        </p>
        <common-table
          :data="responseFields"
          :columns="requestColumns"
        />
      </el-tab-pane>
      <el-tab-pane
        lazy
        :label="$t('mock.label.buildInFunctions')"
        name="buildIn"
      >
        <common-table
          :data="internalFunctions"
          :columns="functionColumns"
        />
      </el-tab-pane>
      <el-tab-pane
        v-for="(exampleGroup, groupIndex) in exampleGroups"
        :key="groupIndex"
        lazy
        :label="$t('common.label.example')"
        class="prevent-scroll"
        :name="exampleGroup.name"
      >
        <template #label>
          {{ exampleGroup.label }}
          <el-link
            v-if="exampleGroup.tooltip"
            v-common-tooltip="exampleGroup.tooltip"
            underline="never"
            type="primary"
            class="margin-left1"
          >
            <common-icon
              icon="QuestionFilled"
            />
          </el-link>
        </template>
        <p v-if="exampleGroup.description">
          <strong>{{ exampleGroup.description }}</strong>
        </p>
        <template
          v-for="(example,index) in exampleGroup.examples"
          :key="`${groupIndex}-${index}`"
        >
          <p>
            {{ example.label }}
            <el-link
              v-if="example.link"
              type="primary"
              :href="example.link"
            >
              {{ example.link }}
            </el-link>
            <mock-url-copy-link :content="example.content" />
          </p>
          <vue-monaco-editor
            v-model:value="example.content"
            :language="example.language||languageRef"
            :options="monacoEditorOptions"
            class="common-resize-vertical"
            :theme="useGlobalConfigStore().monacoTheme"
            :height="exampleHeights[`${groupIndex}-${index}`] || calcHeight(example.content)"
            @mount="editor => fitExampleHeight(editor, `${groupIndex}-${index}`)"
          />
        </template>
      </el-tab-pane>
    </el-tabs>
  </el-container>
</template>

<style scoped>
.mock-tips { position: relative; }
.tip-tabs { flex: 1; min-height: 0; }
.ai-help-tabs :deep(.el-tabs__content) { overflow: hidden; }
.ai-help-pane { height: 100%; }
.help-topic-action { position: absolute; top: 4px; right: 0; z-index: 1; }
.has-topic-action :deep(.el-tabs__header) {
  padding-right: 44px;
}
.has-topic-action :deep(.el-tabs__header)::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background-color: var(--el-border-color-light);
  pointer-events: none;
}
.has-topic-action :deep(.el-tabs__nav-wrap)::after {
  display: none;
}
.user-guide {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 28px;
  align-items: start;
  font-size: var(--el-font-size-base);
  color: var(--el-text-color-regular);
  line-height: 1.7;
}
.guide-nav { padding: 4px 16px 12px 0; border-right: 1px solid var(--el-border-color-lighter); }
.guide-nav h4 { margin: 22px 12px 8px; font-size: 12px; font-weight: 400; color: var(--el-text-color-secondary); }
.guide-nav button {
  display: block;
  width: 100%;
  margin: 2px 0;
  padding: 8px 12px;
  border: 0;
  border-left: 2px solid transparent;
  border-radius: 0 var(--el-border-radius-base) var(--el-border-radius-base) 0;
  background: transparent;
  color: inherit;
  text-align: left;
  font: inherit;
  cursor: pointer;
}
.guide-nav button:hover { background: var(--el-fill-color-light); }
.guide-nav button.active { color: var(--el-color-primary); border-left-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.guide-section { min-width: 0; max-width: 960px; padding: 8px 8px 20px 0; }
.guide-section h3 { margin: 0 0 12px; font-size: 18px; font-weight: 600; color: var(--el-text-color-primary); }
.guide-section p { margin: 8px 0; }
.guide-intro { color: var(--el-text-color-secondary); }
.guide-steps {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 280px), 1fr));
  gap: 18px 24px;
  margin: 24px 0;
  padding: 0;
  list-style: none;
  counter-reset: guide-step;
}
.guide-steps li { position: relative; padding-left: 36px; counter-increment: guide-step; }
.guide-steps li::before {
  content: counter(guide-step);
  position: absolute;
  left: 0;
  top: 1px;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 50%;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-weight: 600;
}
.guide-steps strong { color: var(--el-text-color-primary); font-weight: 500; }
.guide-steps p { margin: 4px 0 0; color: var(--el-text-color-secondary); }
.guide-example { margin: 20px 0; }
.guide-example-title { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.guide-result { padding: 0 0 0 12px; border-left: 2px solid var(--el-border-color); color: var(--el-text-color-secondary); }
.guide-result p { margin: 0 0 4px; }
.guide-links { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 24px; }
.guide-links .el-button { margin-left: 0; }
@media (max-width: 700px) {
  .user-guide { grid-template-columns: 1fr; gap: 16px; }
  .guide-nav { max-height: 180px; overflow-y: auto; border-right: 0; border-bottom: 1px solid var(--el-border-color-lighter); padding-right: 0; }
  .guide-section { padding-right: 0; }
}
</style>
