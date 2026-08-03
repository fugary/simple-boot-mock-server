<script setup>
import CommonIcon from '@/components/common-icon/index.vue'

defineProps({
  buttons: {
    type: Array,
    required: true
  },
  item: {
    type: Object,
    required: true
  }
})
</script>

<template>
  <el-dropdown-menu>
    <template v-for="(moreButton, index) in buttons">
      <el-dropdown-item
        v-if="moreButton.enabled!==false&&(!moreButton.buttonIf||moreButton.buttonIf(item))"
        :key="index"
        :disabled="moreButton.disabled"
        @click="moreButton.click?.(item)"
      >
        <el-text :type="moreButton.type">
          <common-icon
            :size="16"
            :icon="moreButton.icon"
          />
          {{ moreButton.label || $t(moreButton.labelKey) }}
        </el-text>
      </el-dropdown-item>
    </template>
  </el-dropdown-menu>
</template>
