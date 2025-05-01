<script setup>
import { onBeforeUnmount, shallowRef } from 'vue'

defineProps({
  title: {
    type: String,
    default: ''
  },
  original: {
    type: Object, default: () => ({})
  },
  modified: {
    type: Object, default: () => ({})
  },
  contentKey: { type: String, default: 'content' },
  compareItems: {
    type: Array, default: () => []
  }
})
const showWindow = defineModel({
  type: Boolean,
  default: false
})
const diffOptions = {
  automaticLayout: true,
  formatOnType: true,
  formatOnPaste: true,
  readOnly: true
}
const diffEditorRef = shallowRef()
const handleMount = diffEditor => (diffEditorRef.value = diffEditor)
const showCompareWindow = () => {
  showWindow.value = true
}
onBeforeUnmount(() => {
  diffEditorRef.value?.dispose()
})
defineExpose({
  showCompareWindow
})
</script>

<template>
  <common-window
    v-model="showWindow"
    width="1100px"
    :show-cancel="false"
    :ok-label="$t('common.label.close')"
    destroy-on-close
    :title="title||$t('mock.label.compare')"
    append-to-body
    show-fullscreen
    v-bind="$attrs"
  >
    <el-container class="flex-column">
      <common-descriptions
        class="form-edit-width-100 margin-bottom2"
        :column="2"
        border
        width="25%"
        :items="compareItems"
      />
      <vue-monaco-diff-editor
        v-if="original && modified"
        theme="vs-dark"
        :original="original[contentKey]"
        :modified="modified[contentKey]"
        language="markdown"
        :options="diffOptions"
        style="height:500px;"
        @mount="handleMount"
      >
        <div v-loading="true" />
      </vue-monaco-diff-editor>
    </el-container>
  </common-window>
</template>

<style scoped>

</style>
