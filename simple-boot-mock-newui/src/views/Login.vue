<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useThemeAndLocaleMenus } from '@/services/menu/MenuService'
import { useLoginConfigStore } from '@/stores/LoginConfigStore'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { APP_VERSION } from '@/config'
import { formatDate } from '@/utils'

const router = useRouter()
const loginConfigStore = useLoginConfigStore()

const themeAndLocaleMenus = ref(useThemeAndLocaleMenus())

/**
 * @type {[CommonFormOption]}
 */
const loginFormOptions = [{
  labelKey: 'common.label.username',
  required: true,
  prop: 'userName',
  attrs: {
    size: 'large',
    'prefix-icon': User
  }
}, {
  labelKey: 'common.label.password',
  required: true,
  prop: 'userPassword',
  attrs: {
    size: 'large',
    showPassword: true,
    'prefix-icon': Lock
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
  <div class="login-container">
    <!-- Tools (Language/Theme) - Absolute Top Right -->
    <div class="login-tools">
      <common-menu
        :menus="themeAndLocaleMenus"
        mode="horizontal"
        :ellipsis="false"
      />
    </div>

    <!-- Left Side: Branding -->
    <div class="login-branding">
      <div class="branding-content">
        <div class="branding-logo">
          <img src="@/assets/logo.png" alt="Logo" />
        </div>
        <h1 class="branding-title">
          {{ $t('common.label.title') }}
        </h1>
        <p class="branding-subtitle">
          {{ $t('common.msg.brandingSubtitle') }}
        </p>
        <div class="branding-decoration" />
      </div>
      <div class="branding-footer">
        <span>© {{ formatDate(new Date(), 'YYYY') }} Version {{ APP_VERSION }}</span>
      </div>
    </div>

    <!-- Right Side: Form -->
    <div class="login-form-section">
      <div class="form-wrapper">
        <div class="form-header">
          <h2>{{ $t('common.msg.loginTitle') }}</h2>
          <p class="form-subtitle">
            {{ $t('common.msg.loginSubtitle') }}
          </p>
        </div>

        <common-form
          ref="formRef"
          :model="loginVo"
          :options="loginFormOptions"
          label-position="top"
          class="modern-form"
          :show-buttons="false"
          @submit-form="submitForm"
        />

        <div class="form-actions">
          <el-button
            type="primary"
            class="login-btn"
            size="large"
            :loading="loading"
            @click="submitForm(formRef.form)"
          >
            {{ loading ? $t('common.label.logining') : $t('common.label.login') }}
          </el-button>
          <el-button
            class="reset-btn"
            text
            size="large"
            @click="formRef.form.resetFields()"
          >
            {{ $t('common.label.reset') }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped src="@/assets/login.css"></style>
