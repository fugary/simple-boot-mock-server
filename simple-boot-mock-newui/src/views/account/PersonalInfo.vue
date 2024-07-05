<script setup>
import { ref } from 'vue'
import MockUserApi from '@/api/mock/MockUserApi'
import { useLoginConfigStore } from '@/stores/LoginConfigStore'
import { defineFormOptions } from '@/components/utils'
import { ElMessage } from 'element-plus'
import { $i18nBundle } from '@/messages'

const loginConfigStore = useLoginConfigStore()
const userAccount = ref()
const loadUserAccount = id => {
  if (id) {
    return MockUserApi.getById(id)
      .then(data => {
        if (data.success && data.resultData) {
          userAccount.value = data.resultData
        }
      })
  }
}
loadUserAccount(loginConfigStore.accountInfo?.id)

const formOptions = defineFormOptions([{
  label: '用户名',
  prop: 'userName',
  required: true,
  disabled: !!userAccount.value?.id
}, {
  label: '昵称',
  prop: 'nickName',
  required: true
}, {
  label: '邮箱',
  prop: 'userEmail',
  rules: [{
    validator (_, value) {
      console.log('==================', value, '===========')
      return !value || /^[A-Za-z0-9\u4e00-\u9fa5]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/.test(value)
    },
    message: '请输入正确的邮箱地址'
  }]
}, {
  label: '密码',
  prop: 'userPassword',
  required: true,
  attrs: {
    showPassword: true
  }
}])

const submitForm = form => {
  form.validate(valid => {
    if (valid) {
      MockUserApi.saveOrUpdate(userAccount.value, { loading: true }).then(data => {
        if (data.success) {
          ElMessage.success($i18nBundle('common.msg.saveSuccess'))
          return loadUserAccount(userAccount.value.id)
        }
      }).then(() => loginConfigStore.updateUserInfo({ ...userAccount.value }))
    }
  })
}

</script>

<template>
  <el-container class="container-center">
    <common-form
      v-if="userAccount"
      class="form-edit-width-70"
      :model="userAccount"
      :options="formOptions"
      label-width="150px"
      @submit-form="submitForm"
    />
  </el-container>
</template>

<style scoped>

</style>
