<script setup>
import { useResourceApi } from '@/hooks/ApiHooks'
import UserInfo from '@/views/components/user/UserInfo.vue'
import { ref } from 'vue'

const { getById } = useResourceApi('/admin/users')
const showWindow = ref(false)
const userDetail = ref({})
const loading = ref(false)
const showUserInfo = (id) => {
  userDetail.value = {}
  loading.value = true
  getById(id).then(data => {
    loading.value = false
    if (data.success && data.resultData) {
      userDetail.value = data.resultData
    }
  })
  showWindow.value = true
}

defineExpose({
  showUserInfo
})
</script>

<template>
  <common-window
    v-model="showWindow"
    :show-cancel="false"
    :ok-label="$t('common.label.close')"
    title="用户详情"
    destroy-on-close
    width="600px"
  >
    <el-container
      v-loading="loading"
      class="flex-column"
    >
      <user-info :user="userDetail" />
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
