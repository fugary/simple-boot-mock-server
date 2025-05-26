<script setup>
import { $i18nBundle } from '@/messages'

defineProps({
  title: {
    type: String,
    default: ''
  },
  schemas: {
    type: Array,
    default: () => []
  }
})
const emit = defineEmits(['generateSample'])
</script>

<template>
  <el-dropdown>
    <el-link
      v-common-tooltip="title||$t('common.label.generateData')"
      type="primary"
      underline="never"
      class="margin-left3"
    >
      <common-icon
        :size="18"
        icon="custom-icon-json"
      />
    </el-link>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="(schema, index) in schemas"
          :key="index"
          @click="emit('generateSample', schema)"
        >
          <common-icon :icon="`custom-icon-${schema.type}`" />
          {{ $i18nBundle('common.label.commonGenerate', [schema.type]) }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped>

</style>
