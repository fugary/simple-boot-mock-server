<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  }
})
const emit = defineEmits(['generateSample'])
</script>

<template>
  <el-dropdown>
    <el-link
      v-common-tooltip="title||$t('common.label.generateData')"
      type="primary"
      :underline="false"
      class="margin-left3"
    >
      <common-icon
        :size="18"
        icon="DataObjectFilled"
      />
    </el-link>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item @click="emit('generateSample', 'json')">
          {{ $t('common.label.generateJsonData') }}
        </el-dropdown-item>
        <el-dropdown-item @click="emit('generateSample', 'xml')">
          {{ $t('common.label.generateXmlData') }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped>

</style>
