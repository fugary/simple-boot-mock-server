<script setup>
import { ref, computed } from 'vue'
import MockUserApi from '@/api/mock/MockUserApi'
import { useLoginConfigStore } from '@/stores/LoginConfigStore'
import { defineFormOptions } from '@/components/utils'
import { ElMessage } from 'element-plus'
import { $i18nBundle } from '@/messages'
import { useRoute } from 'vue-router'
import { isAdminUser, isUserAdmin, useBackUrl } from '@/utils'
import { useFormStatus } from '@/consts/GlobalConstants'

const props = defineProps({
  personal: {
    type: Boolean,
    default: false
  }
})
const { goBack } = useBackUrl('/admin/users')
const route = useRoute()
const loginConfigStore = useLoginConfigStore()

const userId = props.personal ? loginConfigStore.accountInfo?.id : route.params.id
const userAccount = ref({
  status: 1
})
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

loadUserAccount(userId)

const formOptions = computed(() => {
  return defineFormOptions([{
    labelKey: 'common.label.username',
    prop: 'userName',
    required: true,
    disabled: !!userAccount.value?.id
  }, {
    labelKey: 'common.label.nickName',
    prop: 'nickName',
    required: true
  }, {
    ...useFormStatus(),
    enabled: isAdminUser() && !isUserAdmin(userAccount.value?.userName)
  }, {
    labelKey: 'common.label.email',
    prop: 'userEmail',
    rules: [{
      validator (_, value) {
        console.log('==================', value, '===========')
        return !value || /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(value)
      },
      message: $i18nBundle('common.msg.emailMsg')
    }]
  }, {
    labelKey: 'common.label.password',
    prop: 'userPassword',
    required: true,
    attrs: {
      showPassword: true
    }
  }])
})

const submitForm = form => {
  form.validate(valid => {
    if (valid) {
      MockUserApi.saveOrUpdate(userAccount.value, { loading: true }).then(data => {
        if (data.success) {
          ElMessage.success($i18nBundle('common.msg.saveSuccess'))
          if (props.personal) {
            return loadUserAccount(userAccount.value.id)
          } else {
            goBack()
          }
        }
      }).then(() => {
        if (props.personal || userAccount.value.userName === loginConfigStore.accountInfo?.userName) {
          loginConfigStore.updateUserInfo({ ...userAccount.value })
        }
      })
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
      :submit-label="$t('common.label.save')"
      :back-url="!personal?goBack:''"
      @submit-form="submitForm"
    />
  </el-container>
</template>

<style scoped>

</style>
