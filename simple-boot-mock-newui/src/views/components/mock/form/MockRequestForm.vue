<script setup>
import { computed } from 'vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import MockRequestFormRes from '@/views/components/mock/form/MockRequestFormRes.vue'
import MockRequestFormReq from '@/views/components/mock/form/MockRequestFormReq.vue'
import MockRequestFormUrl from '@/views/components/mock/form/MockRequestFormUrl.vue'
import MockRequestFormMatchPattern from '@/views/components/mock/form/MockRequestFormMatchPattern.vue'
import { addParamsToURL } from '@/utils'

const props = defineProps({
  responseTarget: {
    type: Object,
    default: () => undefined
  },
  requestPath: {
    type: String,
    required: true
  },
  matchPatternMode: {
    type: Boolean,
    default: false
  },
  mockResponseEditable: {
    type: Boolean,
    default: false
  },
  schemas: {
    type: Array,
    default: () => []
  }
})
const paramTarget = defineModel('modelValue', {
  type: Object,
  default: () => ({})
})

const requestUrl = computed(() => {
  let reqUrl = props.requestPath
  paramTarget.value?.pathParams?.forEach(pathParam => {
    reqUrl = reqUrl.replace(new RegExp(`:${pathParam.name}`, 'g'), pathParam.value)
      .replace(new RegExp(`\\{${pathParam.name}\\}`, 'g'), pathParam.value)
  })
  paramTarget.value?.requestParams?.filter(requestParam => !!requestParam.name && requestParam.enabled).forEach(requestParam => {
    reqUrl = addParamsToURL(reqUrl, { [requestParam.name]: requestParam.value })
  })
  return reqUrl
})

const emit = defineEmits(['sendRequest', 'saveMockResponseBody'])

const sendRequest = (form) => {
  form.validate(valid => {
    if (valid) {
      console.log('===============================发送请求', valid, paramTarget.value)
      emit('sendRequest', paramTarget.value)
    }
  })
}

const schema = computed(() => {
  return props.schemas[0]
})

const requestExamples = computed(() => {
  const examples = schema.value?.requestExamples
  return examples ? JSON.parse(examples) : []
})

const responseExamples = computed(() => {
  const examples = schema.value?.responseExamples
  return examples ? JSON.parse(examples) : []
})

</script>

<template>
  <el-container class="flex-column">
    <common-form
      :show-buttons="false"
      :model="paramTarget"
    >
      <template #default="{form}">
        <el-row>
          <el-col :span="20">
            <mock-request-form-url
              v-if="matchPatternMode"
              v-model="paramTarget"
            />
            <el-descriptions
              v-else
              :column="1"
              class="form-edit-width-100 margin-bottom3"
              border
            >
              <el-descriptions-item
                :label="paramTarget.method"
                min-width="40px"
              >
                <el-text
                  class="padding-right1"
                  truncated
                  style="white-space: break-spaces;"
                >
                  {{ requestUrl }}
                </el-text>
                <mock-url-copy-link
                  style="vertical-align: unset;"
                  :url-path="requestUrl"
                />
              </el-descriptions-item>
            </el-descriptions>
          </el-col>
          <el-col
            :span="4"
            class="padding-top1 padding-left2"
          >
            <el-button
              type="primary"
              @click="sendRequest(form)"
            >
              {{ $t('mock.label.sendRequest') }}
            </el-button>
          </el-col>
        </el-row>
        <el-row>
          <el-col>
            <mock-request-form-match-pattern
              v-if="matchPatternMode"
              v-model="paramTarget"
            />
          </el-col>
        </el-row>
        <mock-request-form-req
          v-model="paramTarget"
          :show-authorization="!matchPatternMode"
          :response-target="responseTarget"
          :schema-type="schema?.requestMediaType"
          :schema-body="schema?.requestBodySchema"
          :examples="requestExamples"
        />
      </template>
    </common-form>
    <mock-request-form-res
      v-if="responseTarget||mockResponseEditable"
      v-model="paramTarget"
      :mock-response-editable="mockResponseEditable"
      :response-target="responseTarget"
      :schema-type="schema?.responseMediaType"
      :schema-body="schema?.responseBodySchema"
      :examples="responseExamples"
      @save-mock-response-body="emit('saveMockResponseBody', $event)"
    />
  </el-container>
</template>

<style scoped>

</style>
