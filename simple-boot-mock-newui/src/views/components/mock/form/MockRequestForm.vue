<script setup>
import { computed, ref } from 'vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import MockRequestFormRes from '@/views/components/mock/form/MockRequestFormRes.vue'
import MockRequestFormReq from '@/views/components/mock/form/MockRequestFormReq.vue'
import MockRequestFormUrl from '@/views/components/mock/form/MockRequestFormUrl.vue'
import MockRequestFormMatchPattern from '@/views/components/mock/form/MockRequestFormMatchPattern.vue'
import { addParamsToURL, calcAffixOffset } from '@/utils'
import { useDisableAffix } from '@/hooks/useDisableAffix'
import { addRequestParamsToResult, processEvnParams } from '@/services/mock/MockCommonService'

const props = defineProps({
  responseTarget: {
    type: Object,
    default: () => undefined
  },
  requestPath: {
    type: String,
    required: true
  },
  affixEnabled: {
    type: Boolean,
    default: false
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
  },
  schemaSpec: {
    type: Object,
    default: undefined
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
  if (paramTarget.value?.method?.toLowerCase() === 'get') {
    const calcReqParams = paramTarget.value?.requestParams?.filter(requestParam => !!requestParam.name && requestParam.enabled).reduce((results, item) => {
      return addRequestParamsToResult(results, item.name, processEvnParams(paramTarget.value.groupConfig, item.value, true))
    }, {})
    reqUrl = addParamsToURL(reqUrl, calcReqParams)
  }
  return reqUrl
})

const emit = defineEmits(['sendRequest', 'saveMockResponseBody', 'resetRequestForm'])
const formRef = ref()
const sendRequest = (form) => {
  form.validate(valid => {
    if (valid) {
      console.log('===============================发送请求', valid, paramTarget.value)
      emit('sendRequest', paramTarget.value)
    }
  })
}

const schema = computed(() => {
  const jsonSchema = props.schemas.find(schema => schema.requestMediaType?.includes('json') || schema.responseMediaType?.includes('json'))
  return jsonSchema || props.schemas[0]
})

const requestExamples = computed(() => {
  const examples = schema.value?.requestExamples
  return examples ? JSON.parse(examples) : []
})

const responseExamples = computed(() => {
  const examples = schema.value?.responseExamples
  return examples ? JSON.parse(examples) : []
})
const sendButtonOffset = computed(() => calcAffixOffset(0))
const { disableAffix, AffixToggleButton } = useDisableAffix()
</script>

<template>
  <el-container class="flex-column">
    <common-form
      ref="formRef"
      :show-buttons="false"
      :model="paramTarget"
    >
      <template #default="{form}">
        <template v-if="!matchPatternMode">
          <el-row>
            <el-col>
              <el-affix
                v-disable-affix="!affixEnabled || disableAffix"
                style="width: 100%"
                :offset="sendButtonOffset"
              >
                <el-row style="background: var(--el-bg-color)">
                  <el-col :span="20">
                    <el-descriptions
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
                          style="white-space: break-spaces;word-break: break-all;display: inline;"
                        >
                          {{ requestUrl }}
                        </el-text>
                        <mock-url-copy-link
                          style="vertical-align: unset;"
                          :url-path="requestUrl"
                        />
                        <affix-toggle-button
                          v-if="affixEnabled"
                          circle
                        />
                      </el-descriptions-item>
                    </el-descriptions>
                  </el-col>
                  <el-col
                    :span="4"
                    class="flex-center-col padding-left2"
                  >
                    <el-button
                      type="primary"
                      style="margin-top: -15px;"
                      @click="sendRequest(form)"
                    >
                      {{ $t('mock.label.sendRequest') }}
                    </el-button>
                  </el-col>
                </el-row>
              </el-affix>
            </el-col>
          </el-row>
        </template>
        <template v-else>
          <el-row>
            <el-col :span="20">
              <mock-request-form-url v-model="paramTarget" />
            </el-col>
            <el-col
              :span="4"
              class="flex-center-col padding-left2"
            >
              <el-button
                type="primary"
                style="margin-top: -15px;"
                @click="sendRequest(form)"
              >
                {{ $t('mock.label.sendRequest') }}
              </el-button>
            </el-col>
          </el-row>
          <el-row>
            <el-col>
              <mock-request-form-match-pattern v-model="paramTarget" />
            </el-col>
          </el-row>
        </template>
        <mock-request-form-req
          v-model="paramTarget"
          :show-authorization="!matchPatternMode"
          :response-target="responseTarget"
          :schema-type="schema?.requestMediaType"
          :schema-body="schema?.requestBodySchema"
          :schema-spec="schemaSpec"
          :examples="requestExamples"
          @reset-request-form="$emit('resetRequestForm')"
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
      :schema-spec="schemaSpec"
      :examples="responseExamples"
      @save-mock-response-body="emit('saveMockResponseBody', $event)"
      @send-mock-request="sendRequest(formRef?.form)"
    />
  </el-container>
</template>

<style scoped>

</style>
