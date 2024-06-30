<script setup>
import { ref } from 'vue'
const triggerRef = ref()
const visible = ref(false)

defineProps({
  type: {
    type: String,
    default: 'el-tooltip'
  }
})

const propConfig = ref({
  placement: 'top-start',
  rawContent: true,
  effect: 'dark'
})

const setConfig = (config) => {
  Object.assign(propConfig.value, config)
}

const showOrHideTooltip = (show) => {
  visible.value = show
}

defineExpose({
  triggerRef,
  setConfig,
  showOrHideTooltip
})
</script>

<template>
  <component
    :is="type"
    v-if="triggerRef"
    :disabled="!propConfig.content"
    :popper-class="`common-${type}`"
    :visible="visible"
    :virtual-ref="triggerRef"
    virtual-triggering
    v-bind="propConfig"
  >
    <template #content>
      <!--eslint-disable-next-line vue/no-v-html-->
      <div v-html="propConfig.content" />
    </template>
  </component>
</template>

<style scoped>

</style>
