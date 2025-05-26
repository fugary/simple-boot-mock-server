<script setup>
import { $i18nKey } from '@/messages'

defineProps({
  title: {
    type: String,
    default: ''
  },
  examples: {
    type: Array,
    default: () => []
  }
})
const emit = defineEmits(['selectExample'])
</script>

<template>
  <el-dropdown>
    <el-link
      v-common-tooltip="title||$i18nKey('common.label.commonView', 'common.label.example')"
      type="primary"
      underline="never"
      class="margin-left3"
    >
      <common-icon
        :size="18"
        icon="ListAltFilled"
      />
    </el-link>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="(example, index) in examples"
          :key="index"
          @click="emit('selectExample', example)"
        >
          {{ example.summary || example.description }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped>

</style>
