<template>
  <el-container class="index-container">
    <el-aside width="auto">
      <left-menus :menu-collapse.sync="menuCollapse" />
    </el-aside>
    <el-container>
      <el-header class="index-header" height="50px">
        <top-menus :menu-collapse.sync="menuCollapse" />
      </el-header>
      <router-view />
    </el-container>
  </el-container>
</template>

<script>
// @ is an alias to /src
import LeftMenus from '../components/LeftMenus'
import TopMenus from '../components/TopMenus'

export default {
  name: 'Home',
  components: {
    TopMenus,
    LeftMenus
  },
  data () {
    return {
      menuCollapse: false
    }
  },
  computed: {}
}
</script>
