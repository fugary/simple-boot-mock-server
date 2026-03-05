<script setup>
import { computed, ref } from 'vue'
import { MockRandom } from '@/vendors/mockjs/MockJsonHintData'
import { $i18nBundle } from '@/messages'

const emit = defineEmits(['select'])

const dictGroups = [
  { name: 'mock.label.dict.basic', keys: ['boolean', 'natural', 'integer', 'float', 'character', 'string', 'range'] },
  { name: 'mock.label.dict.dateTime', keys: ['date', 'time', 'datetime', 'now', 'timestamp'] },
  { name: 'mock.label.dict.image', keys: ['image', 'dataImage'] },
  { name: 'mock.label.dict.color', keys: ['color', 'hex', 'rgb', 'rgba', 'hsl'] },
  { name: 'mock.label.dict.text', keys: ['paragraph', 'sentence', 'word', 'title', 'cparagraph', 'csentence', 'cword', 'ctitle'] },
  { name: 'mock.label.dict.name', keys: ['first', 'last', 'name', 'cfirst', 'clast', 'cname'] },
  { name: 'mock.label.dict.web', keys: ['url', 'protocol', 'domain', 'dtl', 'email', 'ip'] },
  { name: 'mock.label.dict.address', keys: ['region', 'province', 'city', 'county', 'zip'] },
  { name: 'mock.label.dict.helpers', keys: ['capitalize', 'upper', 'lower', 'pick', 'shuffle'] },
  { name: 'mock.label.dict.id', keys: ['guid', 'id', 'increment'] },
  { name: 'mock.label.dict.misc', keys: ['version', 'phone'] }
]

const groupedDicts = computed(() => {
  return dictGroups.map(g => ({
    name: $i18nBundle(g.name),
    items: g.keys.map(k => ({ key: k, desc: MockRandom[k]?.desc || k }))
  }))
})

const activeNames = ref([$i18nBundle('mock.label.dict.basic'), $i18nBundle('mock.label.dict.name')])

const handleSelect = (item) => {
  emit('select', `@${item.key}`)
}
</script>

<template>
  <el-popover
    placement="bottom"
    :width="400"
    trigger="click"
  >
    <template #reference>
      <el-link
        v-common-tooltip="$t('mock.label.insertMockVariable')"
        type="primary"
        underline="never"
        class="margin-left3"
      >
        <common-icon
          :size="18"
          icon="MenuBookFilled"
        />
      </el-link>
    </template>

    <div style="max-height: 400px; overflow-y: auto; overflow-x: hidden;">
      <el-collapse v-model="activeNames">
        <el-collapse-item
          v-for="group in groupedDicts"
          :key="group.name"
          :title="group.name"
          :name="group.name"
        >
          <div class="dict-tags">
            <el-tooltip
              v-for="item in group.items"
              :key="item.key"
              :content="item.desc"
              placement="top"
            >
              <el-tag
                class="dict-tag cursor-pointer"
                effect="plain"
                type="info"
                @click="handleSelect(item)"
              >
                @{{ item.key }}
              </el-tag>
            </el-tooltip>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </el-popover>
</template>

<style scoped>
.dict-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.dict-tag {
  cursor: pointer;
  transition: all 0.2s;
}
.dict-tag:hover {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
}
</style>
