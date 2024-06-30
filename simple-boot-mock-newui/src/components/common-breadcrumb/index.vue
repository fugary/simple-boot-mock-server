<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { calcMatchedRoutes } from '@/route/RouteUtils'

const props = defineProps({
  labelConfig: {
    type: [Object, Array],
    default: null
  },
  showIcon: {
    type: Boolean,
    default: true
  }
})

const route = useRoute()
const breadcrumbs = computed(() => {
  return calcMatchedRoutes(route, props.labelConfig)
})

</script>

<template>
  <el-breadcrumb
    class="common-breadcrumb"
  >
    <el-breadcrumb-item
      v-for="(item, index) in breadcrumbs"
      :key="item.path"
      :to="index!==breadcrumbs.length-1?{ path: item.path }:undefined"
    >
      <common-icon
        v-if="showIcon&&item.icon"
        :icon="item.icon"
      />
      {{ item.menuName }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<style scoped>
.common-breadcrumb {
  padding-left: 15px;
  padding-top: 10px;
  height: 30px;
  list-style: none;
  border-radius: 4px;
}
.common-breadcrumb .el-icon {
  vertical-align: bottom;
}
</style>
