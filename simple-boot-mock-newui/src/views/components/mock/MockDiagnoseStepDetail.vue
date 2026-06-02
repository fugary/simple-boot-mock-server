<script setup>
import { computed } from 'vue'
import { $copyText, $openNewWin } from '@/utils'

const props = defineProps({
  step: {
    type: Object,
    required: true
  }
})
const emit = defineEmits(['showRawData'])

const chips = computed(() => props.step?.chips || [])
const showStep = () => emit('showRawData', props.step)
const copyChip = chip => {
  if (chip.copyText) {
    $copyText(chip.copyText)
  }
}
const openChipLink = chip => {
  if (chip.externalLink) {
    $openNewWin(chip.externalLink)
  }
}
</script>

<template>
  <div
    class="mock-diagnose-step-detail"
    @dblclick="showStep"
  >
    <el-link
      type="primary"
      underline="never"
      class="mock-diagnose-step-detail__code"
      @click="showStep"
    >
      <span class="mock-diagnose-step-detail__code-label">{{ step.codeLabel }}</span>
      <span
        v-if="step.showCodeKey"
        class="mock-diagnose-step-detail__code-key"
      >
        {{ step.code }}
      </span>
    </el-link>
    <div
      v-if="chips.length"
      class="mock-diagnose-step-detail__chips"
    >
      <el-tag
        v-for="chip in chips"
        :key="chip.key"
        size="small"
        effect="plain"
        :type="chip.type"
        class="mock-diagnose-step-detail__chip"
        role="button"
        tabindex="0"
        @click.stop="copyChip(chip)"
        @dblclick.stop
        @keydown.enter.prevent.stop="copyChip(chip)"
        @keydown.space.prevent.stop="copyChip(chip)"
      >
        <span class="mock-diagnose-step-detail__chip-label">
          <span>{{ chip.label }}</span>
        </span>
        <span class="mock-diagnose-step-detail__chip-value">{{ chip.value }}</span>
        <el-link
          v-if="chip.externalLink"
          v-common-tooltip="$t('mock.label.linkAddress')"
          type="primary"
          underline="never"
          class="mock-diagnose-step-detail__chip-link"
          @click.stop="openChipLink(chip)"
        >
          <common-icon
            :size="13"
            icon="Link"
          />
        </el-link>
      </el-tag>
    </div>
  </div>
</template>

<style scoped>
.mock-diagnose-step-detail {
  cursor: pointer;
}

.mock-diagnose-step-detail__code {
  max-width: 100%;
  overflow-wrap: anywhere;
  vertical-align: top;
}

.mock-diagnose-step-detail__code-label {
  font-weight: 600;
}

.mock-diagnose-step-detail__code-key {
  font-family: var(--el-font-family);
  font-size: 12px;
  font-weight: 400;
  opacity: 0.72;
}

.mock-diagnose-step-detail__code-key::before {
  content: "(";
  margin-left: 4px;
}

.mock-diagnose-step-detail__code-key::after {
  content: ")";
}

.mock-diagnose-step-detail__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.mock-diagnose-step-detail__chip {
  max-width: 100%;
  cursor: pointer;
  transition: border-color var(--el-transition-duration), background-color var(--el-transition-duration);
}

.mock-diagnose-step-detail__chip:hover {
  background-color: var(--el-fill-color-light);
  border-color: var(--el-tag-hover-color);
}

.mock-diagnose-step-detail__chip-label {
  color: var(--el-text-color-secondary);
}

.mock-diagnose-step-detail__chip-label::after {
  content: ":";
  margin-right: 4px;
}

.mock-diagnose-step-detail__chip-value {
  display: inline-block;
  max-width: min(420px, 58vw);
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}

.mock-diagnose-step-detail__chip-link {
  margin-left: 6px;
  vertical-align: text-bottom;
}

@media (max-width: 768px) {
  .mock-diagnose-step-detail__chip-value {
    max-width: 220px;
  }
}
</style>
