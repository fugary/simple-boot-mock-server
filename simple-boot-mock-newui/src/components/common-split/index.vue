<script setup>
import { onMounted, onUnmounted, ref, useAttrs, watch } from 'vue'
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
  },
  disabled: {
    type: Boolean,
    default: false
  }
})
const itemRefs = ref([])
const isDragging = ref(false)

const attrs = useAttrs()
let splitInstance = null

const initSplit = () => {
  if (splitInstance) {
    splitInstance.destroy()
    splitInstance = null
  }
  if (props.disabled) return

  splitInstance = Split(itemRefs.value.map(itemRef => itemRef), {
    sizes: props.sizes,
    minSize: props.minSize,
    maxSize: props.maxSize,
    gutterAlign: props.gutterAlign,
    gutterSize: 5,
    direction: props.direction,
    ...attrs,
    onDragStart: (sizes) => {
      isDragging.value = true
      if (attrs.onDragStart) {
        attrs.onDragStart(sizes)
      }
    },
    onDragEnd: (sizes) => {
      isDragging.value = false
      if (attrs.onDragEnd) {
        attrs.onDragEnd(sizes)
      }
    }
  })
}

onMounted(() => {
  initSplit()
})

onUnmounted(() => {
  if (splitInstance) {
    splitInstance.destroy()
  }
})

watch(() => props.disabled, () => {
  initSplit()
})

watch(() => props.sizes, (newSizes) => {
  if (splitInstance) {
    splitInstance.setSizes(newSizes)
  }
})

</script>

<template>
  <div
    class="common-split"
    :class="{ 'is-disabled': disabled, 'is-dragging': isDragging }"
  >
    <div
      v-for="(_, index) in sizes"
      ref="itemRefs"
      :key="index"
      class="split-pane"
    >
      <slot :name="`split-${index}`" />
    </div>
  </div>
</template>

<style scoped>
.common-split {
  height: 100%;
  width: 100%;
}
.split-pane {
  overflow: hidden;
  height: 100%;
}
.common-split.is-disabled {
  display: flex;
  flex-direction: row;
}
.common-split.is-disabled > .split-pane:first-child {
  width: auto !important;
  flex: none;
}
.common-split.is-disabled > .split-pane:last-child {
  flex: 1;
  width: auto !important;
}
:deep(.gutter) {
  background-color: #eee;
  background-repeat: no-repeat;
  background-position: 50%;
}
/* Highlight when dragging (controlled by JS state) */
.is-dragging > :deep(.gutter) {
  background-color: #409eff !important;
}
:deep(.gutter.gutter-horizontal) {
  cursor: col-resize;
  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAeCAYAAADkftS9AAAAIklEQVQoU2M4c+bMfxAGAgYYmwGrIIiDjrELjpo5aiZemwF+yNnOs5KSvgAAAABJRU5ErkJggg==');
}
</style>
