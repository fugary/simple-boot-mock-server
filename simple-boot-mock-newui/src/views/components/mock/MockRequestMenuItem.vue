<script setup>
import { computed } from 'vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import MockRequestApi, { ALL_METHODS, copyMockRequest } from '@/api/mock/MockRequestApi'
import CommonIcon from '@/components/common-icon/index.vue'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import { defineTableButtons } from '@/components/utils'
import { $coreConfirm } from '@/utils'
import { $i18nBundle, $i18nConcat } from '@/messages'
const props = defineProps({
  groupItem: {
    type: Object,
    required: true
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

const emit = defineEmits(['requestChanged', 'toTestMockRequest', 'toEditMockRequest', 'toTestMatchPattern', 'toEditDelay', 'saveMockRequest'])

const buttons = computed(() => {
  return defineTableButtons([{
    labelKey: 'common.label.edit',
    icon: 'Edit',
    type: 'primary',
    click: (item) => {
      emit('toEditMockRequest', item, props.groupItem)
    }
  }, {
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
  }, {
    labelKey: 'common.label.copy',
    type: 'warning',
    icon: 'FileCopyFilled',
    click: (item) => {
      $coreConfirm($i18nBundle('common.msg.confirmCopy'))
        .then(() => copyMockRequest(item.id))
        .then(() => emit('requestChanged', item, props.groupItem))
    }
  }, {
    labelKey: 'common.label.delete',
    type: 'danger',
    icon: 'DeleteFilled',
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

</script>

<template>
  <el-container class="flex-column">
    <el-row>
      <el-col>
        {{ requestItem.requestPath }}
        <mock-url-copy-link
          v-if="requestItem.proxyUrl||groupItem.proxyUrl"
          :tooltip="`${$t('mock.label.proxyUrl')}: ${requestItem.proxyUrl||groupItem.proxyUrl}`"
          :url-path="requestItem.proxyUrl||groupItem.proxyUrl"
        >
          <common-icon
            :size="18"
            icon="Link"
          />
        </mock-url-copy-link>
        <mock-url-copy-link
          class="margin-left1"
          :url-path="fullPath"
        />
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="12">
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
          click-to-toggle
          @toggle-value="changeStatus"
        />
      </el-col>
      <el-col
        :span="12"
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
            :underline="false"
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
