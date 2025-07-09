<script setup>
import { inject, ref, onMounted, isRef, watchEffect, onUnmounted } from 'vue'
import { useVModel, onKeyStroke } from '@vueuse/core'
import { useRouter } from 'vue-router'
import { isFunction } from 'lodash-es'

/**
 * @type {CommonFormProps}
 */
const props = defineProps({
  /**
   * @type [CommonFormOption]
   */
  options: {
    type: Array,
    default () {
      return []
    }
  },
  labelWidth: {
    type: String,
    default: '110px'
  },
  model: {
    type: Object,
    default: null
  },
  inline: {
    type: Boolean
  },
  className: {
    type: String,
    default: 'common-form'
  },
  buttonStyle: {
    type: [String, Object],
    default: ''
  },
  validateOnRuleChange: {
    type: Boolean,
    default: false
  },
  showButtons: {
    type: Boolean,
    default: true
  },
  disableButtons: {
    type: Boolean,
    default: false
  },
  showSubmit: {
    type: Boolean,
    default: true
  },
  disableSubmitIfNotValid: {
    type: Boolean,
    default: false
  },
  submitLabel: {
    type: String,
    default: ''
  },
  showReset: {
    type: Boolean,
    default: false
  },
  resetLabel: {
    type: String,
    default: ''
  },
  showBack: {
    type: Boolean,
    default: false
  },
  backLabel: {
    type: String,
    default: ''
  },
  backUrl: {
    type: [String, Function],
    default: ''
  },
  submitByEnter: {
    type: Boolean,
    default: true
  },
  scrollToError: {
    type: Boolean,
    default: false
  }
})

const router = useRouter()

defineOptions({
  inheritAttrs: false
})

const emit = defineEmits(['submitForm', 'update:model'])

const formModel = useVModel(props, 'model', emit)

//= ============form暴露============//
const form = ref()

defineExpose({
  form
})
const formDiv = ref()
const removeEnterFn = ref()
const commonWindowRef = inject('commonWindow', null)
onMounted(() => {
  if (isRef(commonWindowRef)) {
    commonWindowRef.value?.addForm(form)
  }
  if (props.submitByEnter) {
    removeEnterFn.value = onKeyStroke('Enter', (event) => {
      if (event?.target?.tagName === 'TEXTAREA' || event?.target?.isContentEditable) {
        return
      }
      event?.stopImmediatePropagation()
      if (form.value) {
        console.info('=========================submitByEnter', formDiv.value)
        emit('submitForm', form.value)
      }
    }, { target: formDiv.value })
  }
})

onUnmounted(() => {
  if (isRef(commonWindowRef)) {
    commonWindowRef.value?.removeForm(form)
  }
  if (removeEnterFn.value) {
    removeEnterFn.value()
    removeEnterFn.value = null
  }
})

/**
 * 表单校验不通过时禁止点击提交按钮
 */
const disableSubmit = ref(false)
watchEffect(async () => {
  if (!props.disableSubmitIfNotValid) { return false }
  if (!form.value) { return true }
  await form.value.validate((ok) => { disableSubmit.value = !ok })
})

const goBack = (...args) => {
  if (isFunction(props.backUrl)) {
    return props.backUrl(...args)
  } else if (props.backUrl) {
    router.push(props.backUrl)
  } else {
    router.go(-1)
  }
}

</script>

<template>
  <div
    ref="formDiv"
    class="common-form-div"
    :class="$attrs.class"
  >
    <el-form
      ref="form"
      :inline="inline"
      :class="className"
      :model="formModel"
      :label-width="labelWidth"
      :scroll-to-error="scrollToError"
      :validate-on-rule-change="validateOnRuleChange"
      v-bind="{...$attrs, 'class':undefined}"
      @submit.prevent
    >
      <template
        v-for="(option,index) in options"
        :key="index"
      >
        <slot
          v-if="option.slot"
          :name="option.slot"
          :option="option"
          :form="form"
          :model="formModel"
        />
        <common-form-control
          v-if="!option.slot&&option.enabled!==false"
          :model="formModel"
          :option="option"
        >
          <template
            v-if="option.labelSlot"
            #label="scope"
          >
            <slot
              v-if="option.labelSlot"
              :name="option.labelSlot"
              :form="form"
              v-bind="scope"
            />
          </template>
        </common-form-control>
      </template>
      <slot
        :form="form"
        :model="formModel"
        name="default"
      />
      <el-form-item
        v-if="showButtons"
        :style="buttonStyle"
        class="buttonsDiv"
      >
        <el-button
          v-if="showSubmit"
          :disabled="disableSubmit || disableButtons"
          type="primary"
          @click="$emit('submitForm', form)"
        >
          {{ submitLabel||$t('common.label.submit') }}
        </el-button>
        <el-button
          v-if="showReset"
          :disabled="disableButtons"
          @click="form.resetFields()"
        >
          {{ resetLabel||$t('common.label.reset') }}
        </el-button>
        <el-button
          v-if="showBack||backUrl"
          :disabled="disableButtons"
          @click="goBack"
        >
          {{ backLabel||$t('common.label.back') }}
        </el-button>
        <slot
          :form="form"
          :model="formModel"
          name="buttons"
        />
      </el-form-item>
      <slot
        :form="form"
        :model="formModel"
        name="after-buttons"
      />
    </el-form>
  </div>
</template>

<style scoped>

</style>
