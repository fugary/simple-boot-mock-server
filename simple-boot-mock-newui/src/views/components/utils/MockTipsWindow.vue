<script setup>
import { ref, computed } from 'vue'
import { $i18nMsg } from '@/messages'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'

const showWindow = ref(false)
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

const { languageRef, monacoEditorOptions } = useMonacoEditorOptions({ readOnly: false })
languageRef.value = 'javascript'
const javascriptContent = ref(`
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
}());
`.trim())
const mockJsContent = ref(`
Mock.mock({
    "code": "success",
    'data|10': [{
        "id": "@integer(1, 10000)",
        "name": "@name",
        "birthday": "@date",
        "gender": "@pick(['男', '女', '未知'])"
    }]
})
`.trim())
const requestContent = ref(`
Mock.mock({
  headers: request.headers,
  body: request.body,
  params: request.params
})
`.trim())
const xmlContent = ref(`
<root>
  <name>{{Mock.mock('@name')}}</name>
  <id>{{Mock.mock('@id')}}</id>
</root>
`.trim())
const js2XMLContent = ref(`
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
`.trim())

const showMockTips = () => {
  showWindow.value = true
}
defineExpose({
  showMockTips
})
</script>

<template>
  <common-window
    v-model="showWindow"
    width="800px"
    :show-cancel="false"
    :ok-label="$t('common.label.close')"
    destroy-on-close
    :title="`${$t('mock.label.requestObject')} & ${$t('mock.label.buildInFunctions')}`"
    append-to-body
    v-bind="$attrs"
  >
    <el-container class="flex-column">
      <el-tabs>
        <el-tab-pane :label="$t('mock.label.requestObject')">
          <common-table
            :data="requestFields"
            :columns="requestColumns"
          />
        </el-tab-pane>
        <el-tab-pane :label="$t('mock.label.buildInFunctions')">
          <common-table
            :data="internalFunctions"
            :columns="functionColumns"
          />
        </el-tab-pane>
        <el-tab-pane :label="$t('common.label.example')">
          <div class="padding-bottom3">
            {{ $t('mock.msg.functionTooltip') }}
          </div>
          <vue-monaco-editor
            v-model:value="javascriptContent"
            :language="languageRef"
            :options="monacoEditorOptions"
            class="common-resize-vertical"
            height="300px"
          />
          <p>
            Mock.js
          </p>
          <vue-monaco-editor
            v-model:value="mockJsContent"
            :language="languageRef"
            :options="monacoEditorOptions"
            class="common-resize-vertical"
            height="180px"
          />
          <p>
            {{ $t('mock.label.requestObject') }}
          </p>
          <vue-monaco-editor
            v-model:value="requestContent"
            :language="languageRef"
            :options="monacoEditorOptions"
            class="common-resize-vertical"
            height="100px"
          />
          <p>
            XML Output
          </p>
          <vue-monaco-editor
            v-model:value="xmlContent"
            :language="languageRef"
            :options="monacoEditorOptions"
            class="common-resize-vertical"
            height="80px"
          />
          <p>
            JS generate XML
          </p>
          <vue-monaco-editor
            v-model:value="js2XMLContent"
            :language="languageRef"
            :options="monacoEditorOptions"
            class="common-resize-vertical"
            height="350px"
          />
        </el-tab-pane>
      </el-tabs>
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
