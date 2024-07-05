<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useThemeAndLocaleMenus } from '@/services/menu/MenuService'
import { useLoginConfigStore } from '@/stores/LoginConfigStore'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loginConfigStore = useLoginConfigStore()

const themeAndLocaleMenus = ref(useThemeAndLocaleMenus())

/**
 * @type {[CommonFormOption]}
 */
const loginFormOptions = [{
  labelKey: 'common.label.username',
  required: true,
  prop: 'userName'
}, {
  labelKey: 'common.label.password',
  required: true,
  prop: 'userPassword',
  attrs: {
    showPassword: true
  }
}]

/**
 * @type {LoginVo}
 */
const loginVo = ref({
  userName: loginConfigStore.lastLoginName || 'admin',
  userPassword: ''
})

const loading = ref(false)

const submitForm = form => {
  form.validate(async (valid) => {
    if (valid) {
      loading.value = true
      const loginResult = await loginConfigStore.login(loginVo.value)
        .finally(() => {
          loading.value = false
        })
      if (loginResult.success) {
        router.push('/')
      } else {
        ElMessage.error(loginResult.message)
      }
    }
  })
}
const formRef = ref()
</script>

<template>
  <el-container>
    <el-card class="login-form">
      <template #header>
        <div class="card-header">
          <span>{{ $t('common.msg.loginTitle') }}</span>
          <common-menu
            :menus="themeAndLocaleMenus"
            mode="horizontal"
            :ellipsis="false"
          />
        </div>
      </template>
      <common-form
        ref="formRef"
        :model="loginVo"
        :options="loginFormOptions"
        label-width="100px"
        :show-buttons="false"
        @submit-form="submitForm"
      />
      <template #footer>
        <el-button
          type="primary"
          :loading="loading"
          @click="submitForm(formRef.form)"
        >
          <span v-if="!loading">{{ $t('common.label.login') }}</span>
          <span v-else>{{ $t('common.label.logining') }}</span>
        </el-button>
        <el-button
          @click="formRef.form.resetFields()"
        >
          {{ $t('common.label.reset') }}
        </el-button>
      </template>
    </el-card>
  </el-container>
</template>

<style scoped>
.login-form {
  width: 500px;
  margin: 5% auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.el-menu--horizontal.el-menu {
  border-bottom: none;
}

</style>
