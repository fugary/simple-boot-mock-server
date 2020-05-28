<template>
    <el-menu class="index-menu" :collapse="isCollapse" router :default-active="$route.path">
        <el-row>
            <el-col class="text-center padding-tb">
                <router-link class="index-title" to="/">{{ logoTitle }}</router-link>
            </el-col>
        </el-row>
        <el-submenu index="1">
            <template slot="title">
                <i class="el-icon-setting"></i>
                <span slot="title">模拟服务</span>
            </template>
            <el-menu-item index="/mock-server/groups">
                <i class="el-icon-s-custom"></i>
                <span slot="title">模拟分组</span>
            </el-menu-item>
            <el-link :href="dbUrl" :underline="false">
                <el-menu-item>
                    <i class="el-icon-user-solid"></i>
                    <span slot="title">数据库控制台</span>
                </el-menu-item>
            </el-link>
        </el-submenu>
    </el-menu>
</template>

<script>
export default {
  name: 'LeftMenus',
  props: {
    menuCollapse: {
      type: Boolean,
      default: false
    }
  },
  data () {
    const { menuCollapse } = this.$props
    const appName = process.env.VUE_APP_APP_NAME
    const appNameSimple = process.env.VUE_APP_APP_NAME_SIMPLE
    let dbUrl = process.env.VUE_APP_BASE_URL
    dbUrl = `${dbUrl}${dbUrl.endsWith('/') ? '' : '/'}h2-console`
    return {
      isCollapse: menuCollapse,
      logoTitle: appName,
      appName,
      appNameSimple,
      dbUrl
    }
  },
  watch: {
    menuCollapse: function (collapse) {
      if (collapse) {
        this.logoTitle = this.appNameSimple
        this.$nextTick(() => {
          this.isCollapse = collapse
        })
      } else {
        this.logoTitle = this.appName
        this.isCollapse = collapse
      }
    }
  },
  mounted () {
  },
  methods: {}
}
</script>

<style scoped></style>
