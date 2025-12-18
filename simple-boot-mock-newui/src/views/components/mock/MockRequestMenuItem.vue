<script setup>
import { computed } from 'vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import MockRequestApi, { ALL_METHODS, copyMockRequest } from '@/api/mock/MockRequestApi'
import CommonIcon from '@/components/common-icon/index.vue'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import { defineTableButtons } from '@/components/utils'
import { $coreConfirm } from '@/utils'
import { $i18nBundle, $i18nConcat, $i18nKey } from '@/messages'
const props = defineProps({
  groupItem: {
    type: Object,
    required: true
  },
  editable: {
    type: Boolean,
    default: false
  }
})
const requestItem = defineModel('modelValue', {
  type: Object,
  required: true
})
const methodsConfig = Object.fromEntries(ALL_METHODS.map(method => [method.method, method]))
const fullPath = computed(() => {
  const groupItem = props.groupItem
  return `/mock/${groupItem?.groupPath}${requestItem.value?.requestPath}`
})

const emit = defineEmits(['requestChanged', 'toTestMockRequest', 'toEditMockRequest', 'toShowRequestHistory', 'toTestMatchPattern', 'toEditDelay', 'saveMockRequest'])

const buttons = computed(() => {
  return defineTableButtons([{
    labelKey: 'common.label.test',
    icon: 'RemoveRedEyeFilled',
    type: 'success',
    click: (item) => {
      emit('toTestMockRequest', item, props.groupItem)
    }
  }, {
    labelKey: 'mock.msg.matchPatternTest',
    icon: 'FactCheckFilled',
    type: 'success',
    enabled: !!requestItem.value?.matchPattern,
    click: (item) => {
      emit('toTestMatchPattern', item, props.groupItem)
    }
  }, {
    label: $i18nBundle('common.label.commonDelay', [requestItem.value?.delay]),
    icon: 'MoreTimeFilled',
    type: 'success',
    enabled: !!requestItem.value?.delay,
    click: (item) => {
      emit('toEditDelay', item, props.groupItem)
    }
  }])
})

const moreButtons = computed(() => {
  return defineTableButtons([{
    labelKey: 'common.label.edit',
    icon: 'Edit',
    type: 'primary',
    click: (item) => {
      emit('toEditMockRequest', item, props.groupItem)
    }
  }, {
    labelKey: 'common.label.copy',
    type: 'warning',
    icon: 'FileCopyFilled',
    enabled: props.editable,
    click: (item) => {
      $coreConfirm($i18nBundle('common.msg.confirmCopy'))
        .then(() => copyMockRequest(item.id))
        .then(() => emit('requestChanged', item, props.groupItem))
    }
  }, {
    labelKey: 'mock.label.matchPattern',
    icon: 'FactCheckFilled',
    type: 'success',
    click: (item) => {
      emit('toTestMatchPattern', item, props.groupItem)
    }
  }, {
    labelKey: 'mock.label.modifyHistory',
    type: 'info',
    icon: 'AccessTimeFilled',
    buttonIf (item) {
      return !!item.historyCount
    },
    click: item => {
      emit('toShowRequestHistory', item)
    }
  }, {
    labelKey: 'common.label.delete',
    type: 'danger',
    icon: 'DeleteFilled',
    enabled: props.editable,
    click: item => {
      $coreConfirm($i18nBundle('common.msg.commonDeleteConfirm', [`${item.requestPath}#${item.method}`]))
        .then(() => MockRequestApi.deleteById(item.id))
        .then(() => emit('requestChanged', item, props.groupItem))
    }
  }])
})

const changeStatus = (status) => {
  status = +status
  const statusLabel = $i18nBundle(status === 0 ? 'common.label.statusDisable' : 'common.label.statusEnable')
  const mockRequestKey = $i18nBundle('mock.label.mockRequest')
  $coreConfirm($i18nBundle('common.msg.commonConfirm', [$i18nConcat(statusLabel, mockRequestKey)]))
    .then(() => {
      emit('saveMockRequest', { ...requestItem.value, status })
    })
}

const confirmResumeMock = () => $coreConfirm($i18nKey('common.msg.commonConfirm', 'mock.label.resumeMock'))
  .then(() => emit('saveMockRequest', { ...requestItem.value, disableMock: false }))

const requestProxyUrl = computed(() => {
  let proxyUrl = requestItem.value.proxyUrl || props.groupItem.proxyUrl
  if (proxyUrl) { // 去掉末尾的斜杠
    proxyUrl = proxyUrl.endsWith('/') ? proxyUrl.slice(0, proxyUrl.length - 1) : proxyUrl
    return `${proxyUrl}${requestItem.value?.requestPath}`
  }
  return null
})

</script>

<template>
  <el-container class="flex-column">
    <el-row class="margin-bottom1">
      <el-col>
        <mock-url-copy-link
          class="margin-left1"
          :url-path="fullPath"
        >
          {{ requestItem.requestPath }}
        </mock-url-copy-link>
        <mock-url-copy-link
          v-if="requestProxyUrl"
          :tooltip="`${$t('mock.label.proxyUrl')}: ${requestProxyUrl}`"
          :url-path="requestProxyUrl"
        >
          <common-icon
            :size="18"
            icon="Link"
          />
        </mock-url-copy-link>
      </el-col>
    </el-row>
    <el-row class="margin-bottom1">
      <el-col :span="14">
        <el-tag
          :type="methodsConfig[requestItem.method].type"
          size="small"
          effect="dark"
        >
          {{ requestItem.method }}
        </el-tag>
        <del-flag-tag
          v-model="requestItem.status"
          class="margin-left1"
          :click-to-toggle="editable"
          @toggle-value="changeStatus"
        />
        <el-tag
          v-if="requestItem.dataCount"
          v-common-tooltip="$t('mock.label.mockDataCount')"
          class="margin-left1 pointer"
          type="success"
          size="small"
          effect="plain"
          style="height: 20px;"
          round
        >
          {{ requestItem.dataCount }}
        </el-tag>
        <el-text
          v-if="requestItem.disableMock"
          v-common-tooltip="$t('mock.label.disabledMock')"
          type="danger"
          style="vertical-align: sub;"
          class="margin-left1 pointer"
          @click="confirmResumeMock"
        >
          <common-icon
            :size="18"
            icon="DoDisturbFilled"
          />
        </el-text>
      </el-col>
      <el-col
        :span="10"
        class="text-right"
      >
        <template
          v-for="(button, index) in buttons"
          :key="index"
        >
          <el-link
            v-if="button.enabled!==false&&(!button.buttonIf||button.buttonIf(requestItem))"
            v-common-tooltip="button.label || $t(button.labelKey)"
            :type="button.type"
            underline="never"
            :disabled="button.disabled"
            class="margin-right2"
            @click="button.click?.(requestItem)"
          >
            <common-icon
              :size="16"
              :icon="button.icon"
            />
          </el-link>
        </template>
        <el-dropdown
          placement="bottom"
          style="top: 5px;"
        >
          <el-link
            underline="never"
            type="info"
          >
            <common-icon
              :size="16"
              icon="MoreFilled"
            />
          </el-link>
          <template #dropdown>
            <el-dropdown-menu>
              <template v-for="(moreButton, index) in moreButtons">
                <el-dropdown-item
                  v-if="moreButton.enabled!==false&&(!moreButton.buttonIf||moreButton.buttonIf(requestItem))"
                  :key="index"
                  :disabled="moreButton.disabled"
                  @click="moreButton.click?.(requestItem)"
                >
                  <el-text :type="moreButton.type">
                    <common-icon
                      :size="16"
                      :icon="moreButton.icon"
                    />
                    {{ moreButton.label || $t(moreButton.labelKey) }}
                  </el-text>
                </el-dropdown-item>
              </template>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-col>
    </el-row>
    <el-row v-if="requestItem.requestName">
      <el-col>
        <el-text
          type="info"
          size="small"
        >
          {{ requestItem.requestName }}
        </el-text>
      </el-col>
    </el-row>
  </el-container>
</template>

<style scoped>

</style>
