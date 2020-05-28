<template>
    <el-container>
        <el-card shadow="never" class="login-form">
            <div>
                <el-form :model="loginParam" ref="loginForm" :rules="loginFormRules" @submit.native="doLogin">
                    <div class="padding-b text-center">
                        <h3>用户登录</h3>
                    </div>
                    <el-form-item prop="name">
                        <el-input placeholder="登录名称" prefix-icon="el-icon-s-custom"
                                  v-model="loginParam.name"></el-input>
                    </el-form-item>
                    <el-form-item prop="password">
                        <el-input placeholder="登录密码" prefix-icon="el-icon-lock" type="password"
                                  v-model="loginParam.password"></el-input>
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" class="login-button" native-type="submit">登录
                        </el-button>
                    </el-form-item>
                </el-form>
            </div>
        </el-card>
    </el-container>
</template>

<script>
export default {
  name: 'Login',
  data () {
    const loginParam = {
      name: 'admin',
      password: '123456'
    }
    return {
      loginParam,
      loginFormRules: {
        name: [
          { required: true, message: '请输入登录名称', trigger: 'change' }
        ],
        password: [
          { required: true, message: '请输入登录密码', trigger: 'change' },
          { min: 6, message: '密码长度至少6个字符', trigger: 'change' }
        ]
      }
    }
  },
  methods: {
    doLogin () {
      this.$refs.loginForm.validate(result => {
        if (result) {
          this.$router.push('/')
        }
      })
    }
  }
}
</script>

<style scoped>

</style>
