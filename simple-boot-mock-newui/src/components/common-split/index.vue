<script setup>
import { onMounted, ref, useAttrs } from 'vue'
import Split from 'split.js'

/**
 * 更多属性配置可以参考文档
 * @link https://github.com/nathancahill/split/tree/master/packages/splitjs <br>
 */
const props = defineProps({
  sizes: {
    type: Array,
    default: () => [25, 75]
  },
  minSize: {
    type: [Number, Array],
    default: 100
  },
  maxSize: {
    type: [Number, Array],
    default: Infinity
  },
  direction: {
    type: String,
    default: 'horizontal',
    validator (value) {
      return ['horizontal', 'vertical'].includes(value)
    }
  },
  gutterAlign: {
    type: String,
    default: 'center',
    validator (value) {
      return ['start', 'center', 'end'].includes(value)
    }
  }
})
const itemRefs = ref([])

const attrs = useAttrs()

onMounted(() => {
  Split(itemRefs.value.map(itemRef => itemRef), {
    sizes: props.sizes,
    minSize: props.minSize,
    maxSize: props.maxSize,
    gutterAlign: props.gutterAlign,
    direction: props.direction,
    ...attrs
  })
})

</script>

<template>
  <div class="common-split">
    <div
      v-for="(_, index) in sizes"
      ref="itemRefs"
      :key="index"
    >
      <slot :name="`split-${index}`" />
    </div>
  </div>
</template>

<style scoped>

</style>
