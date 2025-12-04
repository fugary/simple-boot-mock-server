<script setup>
import { computed } from 'vue'
import { $i18nBundle, $i18nMsg } from '@/messages'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'

const requestFields = [
  { field: 'request.body', desc: 'body内容对象', descEn: 'Body content object' },
  { field: 'request.bodyStr', desc: 'body内容字符串', descEn: 'Body content as a string' },
  { field: 'request.headers', desc: '头信息对象', descEn: 'Header information object' },
  { field: 'request.parameters', desc: '请求参数对象', descEn: 'Request parameters object' },
  { field: 'request.pathParameters', desc: '路径参数对象', descEn: 'Path parameters object' },
  { field: 'request.params', desc: '请求参数和路径参数合并', descEn: 'Merged object of request parameters and path parameters' }
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
  { method: 'decodeHex(hex)', desc: '将十六进制字符串解码为普通字符串', descEn: 'Decode hexadecimal string to normal string' },
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
    label: $i18nBundle('mock.label.requestObject'),
    content: `
Mock.mock({
  headers: request.headers,
  body: request.body,
  params: request.params
})
`.trim()
  }]
}, {
  label: $i18nBundle('mock.label.xmlExamples'),
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
  label: $i18nBundle('mock.label.matchPattern'),
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
  label: $i18nBundle('mock.label.fetchAndAsync'),
  examples: [{
    label: 'fetch JSON',
    content: `
(async function () {
    const response = await fetch('https://httpbin.org/put', {
        method: 'put',
        body: JSON.stringify({ username: "example" }),
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

</script>

<template>
  <el-container class="flex-column">
    <el-tabs>
      <el-tab-pane
        lazy
        :label="$t('mock.label.requestObject')"
      >
        <common-table
          :data="requestFields"
          :columns="requestColumns"
        />
      </el-tab-pane>
      <el-tab-pane
        lazy
        :label="$t('mock.label.buildInFunctions')"
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
      >
        <template #label>
          {{ exampleGroup.label }}
        </template>
        <template
          v-for="(example,index) in exampleGroup.examples"
          :key="`${groupIndex}-${index}`"
        >
          <p>
            {{ example.label }}
            <mock-url-copy-link :content="example.content" />
            <el-link
              v-if="example.link"
              type="primary"
              :href="example.link"
            >
              {{ example.link }}
            </el-link>
          </p>
          <vue-monaco-editor
            v-model:value="example.content"
            :language="example.language||languageRef"
            :options="monacoEditorOptions"
            class="common-resize-vertical"
            :height="calcHeight(example.content)"
          />
        </template>
      </el-tab-pane>
    </el-tabs>
  </el-container>
</template>

<style scoped>

</style>
