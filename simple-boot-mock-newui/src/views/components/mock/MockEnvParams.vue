<script setup>
import { ref } from 'vue'
import MockGroupApi from '@/api/mock/MockGroupApi'
import CommonParamsEdit from '@/views/components/utils/CommonParamsEdit.vue'
import { ElMessage } from 'element-plus'
import { $i18nBundle } from '@/messages'
import { DEFAULT_HEADERS } from '@/consts/MockConstants'

const showWindow = ref(false)
const groupItem = ref()
const groupConfig = ref({
  envParams: []
})

let callback
const toEditGroupEnvParams = (groupId) => {
  MockGroupApi.getById(groupId, { loading: true }).then(data => {
    groupItem.value = data.resultData
    if (groupItem.value?.groupConfig) {
      groupConfig.value = JSON.parse(groupItem.value.groupConfig)
    }
    showWindow.value = true
  })
  return new Promise(resolve => (callback = resolve))
}

defineExpose({
  toEditGroupEnvParams
})

const saveGroupConfig = ({ form }) => {
  form.validate(valid => {
    if (valid) {
      groupItem.value.groupConfig = JSON.stringify({ ...groupConfig.value })
      MockGroupApi.saveOrUpdate(groupItem.value)
        .then(() => {
          ElMessage.success($i18nBundle('common.msg.saveSuccess'))
          callback?.(groupItem.value)
          showWindow.value = false
        })
    }
  })
  return false
}

</script>

<template>
  <common-window
    v-model="showWindow"
    width="1000px"
    :ok-label="$t('common.label.save')"
    show-fullscreen
    destroy-on-close
    :ok-click="saveGroupConfig"
  >
    <template #header>
      <span class="el-dialog__title">
        {{ $t('mock.label.mockEnv') }}
      </span>
    </template>
    <common-form
      inline
      :model="groupConfig"
      :show-buttons="false"
    >
      <common-params-edit
        v-model="groupConfig.envParams"
        form-prop="envParams"
        name-required
        value-required
        :name-suggestions="DEFAULT_HEADERS"
      />
    </common-form>
  </common-window>
</template>

<style scoped>

</style>
