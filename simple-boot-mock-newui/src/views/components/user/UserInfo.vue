<script setup>
defineProps({
  user: {
    type: Object,
    default: () => ({})
  },
  minWidth: {
    type: String,
    default: '200px'
  }
})
</script>

<template>
  <el-descriptions
    :column="1"
    border
  >
    <el-descriptions-item
      :label="$t('common.label.username')"
      :min-width="minWidth"
    >
      {{ user.userName }}
    </el-descriptions-item>
    <el-descriptions-item :label="$t('common.label.nickName')">
      {{ user.nickName }}
    </el-descriptions-item>
    <el-descriptions-item :label="$t('common.label.email')">
      {{ user.userEmail }}
    </el-descriptions-item>
    <el-descriptions-item :label="$t('common.label.createDate')">
      {{ $date(user.createDate) }}
    </el-descriptions-item>
    <el-descriptions-item :label="$t('common.label.modifyDate')">
      {{ $date(user.modifyDate) }}
    </el-descriptions-item>
  </el-descriptions>
</template>

<style scoped>

</style>
