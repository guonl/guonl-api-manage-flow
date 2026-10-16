<template>
  <div>
    <el-table :data="rows" size="small">
      <el-table-column label="启用" width="60">
        <template #default="{ row }"><el-switch v-model="row.enabled" size="small" /></template>
      </el-table-column>
      <el-table-column label="Key">
        <template #default="{ row }"><el-input v-model="row.key" size="small" placeholder="key" /></template>
      </el-table-column>
      <el-table-column label="Value">
        <template #default="{ row }"><el-input v-model="row.value" size="small" placeholder="value" /></template>
      </el-table-column>
      <el-table-column width="50" align="center">
        <template #default="{ $index }">
          <el-button size="small" text type="danger" @click="rows.splice($index, 1)">删</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-button size="small" text type="primary" style="margin-top:4px" @click="addRow">+ 添加</el-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ modelValue: { type: Array, default: () => [] } })
const emit = defineEmits(['update:modelValue'])

const rows = computed({
  get: () => props.modelValue,
  set: v => emit('update:modelValue', v)
})

function addRow() {
  emit('update:modelValue', [...props.modelValue, { key: '', value: '', enabled: true }])
}
</script>
