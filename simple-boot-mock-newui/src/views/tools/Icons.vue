<script setup>
import { computed, ref } from 'vue'
import { filterIconsByKeywords } from '@/services/icon/IconService'
import { $copyText } from '@/utils'

const colSize = ref(8)
const keyWords = ref('')

const filterIcons = computed(() => {
  return filterIconsByKeywords(keyWords.value, colSize.value)
})

const copyIcon = (icon) => {
  const iconStr = `<common-icon icon="${icon}"/>`
  $copyText({ text: iconStr, success: `Copied: ${iconStr}` })
}

</script>

<template>
  <el-container
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
    <el-main>
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
              @click="copyIcon(icon)"
            >
              <div>
                <common-icon
                  size="20"
                  :icon="icon"
                />
                <br>
                <span class="icon-text">{{ icon }}</span>
              </div>
            </a>
          </el-col>
        </el-row>
      </recycle-scroller>
      <el-backtop
        v-common-tooltip="$t('common.label.backtop')"
        target=".scroller"
        :right="40"
        :bottom="40"
      />
    </el-main>
  </el-container>
</template>

<style scoped>
.scroller {
  height: 100%;
}
.icon-container {
  height: calc(100% - 70px);
}
.icon-container .el-input {
  width: 80%;
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
