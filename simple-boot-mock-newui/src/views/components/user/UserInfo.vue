<script setup>
import { computed } from 'vue'
import { formatDate } from '@/utils'
const props = defineProps({
  user: {
    type: Object,
    default: () => ({})
  },
  minWidth: {
    type: String,
    default: '200px'
  }
})
const descriptionItems = computed(() => {
  const user = props.user
  return [
    { labelKey: 'common.label.username', value: user.userName },
    { labelKey: 'common.label.nickName', value: user.nickName },
    { labelKey: 'common.label.email', value: user.userEmail },
    { labelKey: 'common.label.createDate', value: formatDate(user.createDate) },
    { labelKey: 'common.label.modifyDate', value: formatDate(user.modifyDate) }
  ]
})
</script>

<template>
  <common-descriptions
    :items="descriptionItems"
    :column="1"
    border
    :min-width="minWidth"
  />
</template>

<style scoped>

</style>
