<script setup>
import { computed, ref } from 'vue'
import { filterIconsByKeywords } from '@/services/icon/IconService'
import { UPDATE_MODEL_EVENT, CHANGE_EVENT, useFormItem } from 'element-plus'

const props = defineProps({
  dialogAttrs: {
    type: Object,
    default () {
      return {}
    }
  },
  colSize: {
    type: Number,
    default: 6
  },
  dialogHeight: {
    type: String,
    default: '400px'
  },
  dialogWidth: {
    type: String,
    default: '600px'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  readonly: {
    type: Boolean,
    default: false
  },
  placeholder: {
    type: String,
    default: ''
  },
  clearable: {
    type: Boolean,
    default: true
  },
  validateEvent: {
    type: Boolean,
    default: true
  }
})
const iconSelectVisible = ref(false)
const keyWords = ref('')

const filterIcons = computed(() => {
  if (iconSelectVisible.value) {
    return filterIconsByKeywords(keyWords.value, props.colSize)
  }
  return []
})

const emit = defineEmits([UPDATE_MODEL_EVENT, CHANGE_EVENT])

const vModel = defineModel({
  type: String,
  default: ''
})

const { formItem } = useFormItem()

const selectIcon = icon => {
  iconSelectVisible.value = false
  vModel.value = icon
  emit(CHANGE_EVENT, icon)
  if (props.validateEvent) {
    formItem?.validate(CHANGE_EVENT)
  }
}

</script>

<template>
  <label class="el-radio">
    <common-icon
      v-if="vModel"
      :icon="vModel"
      class="el-radio__input"
    />
    <span
      v-if="vModel"
      class="el-radio__label"
    >{{ vModel }}</span>
    <el-button
      :class="{'icon-select-button': !!vModel}"
      type="primary"
      :disabled="disabled||readonly"
      size="small"
      @click.prevent="iconSelectVisible = true"
    >
      {{ $t('common.label.select') }}
    </el-button>
    <el-button
      v-if="clearable&&vModel"
      type="danger"
      :disabled="disabled||readonly"
      size="small"
      @click.prevent="selectIcon()"
    >
      {{ $t('common.label.clear') }}
    </el-button>
    <common-window
      v-model="iconSelectVisible"
      :width="dialogWidth"
      v-bind="dialogAttrs"
      draggable
      class="icon-dialog"
      :title="$t('common.msg.pleaseSelectIcon')"
      :show-buttons="false"
    >
      <el-container
        style="overflow: auto;"
        :style="{ height: dialogHeight }"
        class="icon-container"
      >
        <el-header height="40px">
          <el-form label-width="120px">
            <el-form-item :label="$t('common.label.keywords')">
              <el-input
                v-model="keyWords"
                :placeholder="$t('common.msg.inputKeywords')"
              />
            </el-form-item>
          </el-form>
        </el-header>
        <el-main class="icon-area">
          <recycle-scroller
            v-slot="{ item }"
            class="scroller icon-list"
            :items="filterIcons"
            :item-size="80"
            key-field="id"
          >
            <el-row>
              <el-col
                v-for="icon in item.icons"
                :key="icon"
                :span="24/colSize"
                class="text-center"
              >
                <a
                  class="el-button el-button--large is-text icon-a"
                  @click.prevent="selectIcon(icon)"
                >
                  <div>
                    <common-icon
                      size="20"
                      :icon="icon"
                    /><br>
                    <span class="icon-text">{{ icon }}</span>
                  </div>
                </a>
              </el-col>
            </el-row>
          </recycle-scroller>
          <el-backtop
            v-common-tooltip="$t('common.label.backtop')"
            target=".scroller"
            :right="10"
            :bottom="10"
          />
        </el-main>
      </el-container>
    </common-window>
  </label>
</template>

<style scoped>
.scroller  {
  height: 100%;
}
.icon-container {
  overflow: auto;
}
.icon-container .el-input {
  width: 80%;
}
.icon-select-button {
  margin-left: 10px;
}
.el-radio {
  margin-right: 10px;
}
.icon-area {
  padding: 0;
  position: relative;
}
.icon-area .el-backtop {
  position: absolute;
}
.icon-a {
  height:80px;
  display: block;
  width:100%;
  overflow:hidden;
  padding: 15px 10px;
}
.icon-a span {
  font-size: 12px;
}
</style>
