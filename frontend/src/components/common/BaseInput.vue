<template>
  <div class="w-full">
    <label v-if="label" :for="id" class="block text-sm font-medium text-gray-700 mb-1">
      {{ label }}
      <span v-if="required" class="text-red-500 ml-0.5">*</span>
    </label>
    <div class="relative">
      <input
        :id="id"
        v-bind="$attrs"
        :value="modelValue"
        :type="type"
        :placeholder="placeholder"
        :disabled="disabled"
        :class="[
          'w-full border rounded-lg px-3 py-2.5 text-sm transition-shadow duration-150 focus:outline-none focus:ring-2 focus:border-transparent',
          error
            ? 'border-red-400 focus:ring-red-400'
            : 'border-gray-300 focus:ring-primary-500',
          { 'bg-gray-50 cursor-not-allowed': disabled }
        ]"
        @input="$emit('update:modelValue', $event.target.value)"
      />
      <slot name="suffix" />
    </div>
    <p v-if="error" class="mt-1 text-xs text-red-500">{{ error }}</p>
    <p v-else-if="hint" class="mt-1 text-xs text-gray-500">{{ hint }}</p>
  </div>
</template>

<script setup>
defineProps({
  id: String,
  label: String,
  modelValue: [String, Number],
  type: { type: String, default: 'text' },
  placeholder: String,
  error: String,
  hint: String,
  disabled: Boolean,
  required: Boolean,
})
defineEmits(['update:modelValue'])
</script>
