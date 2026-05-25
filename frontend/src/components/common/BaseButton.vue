<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :class="[
      'inline-flex items-center justify-center font-medium rounded-lg transition-colors duration-150 focus:outline-none focus:ring-2 focus:ring-offset-1',
      sizeClasses,
      variantClasses,
      { 'opacity-50 cursor-not-allowed': disabled || loading }
    ]"
    v-bind="$attrs"
  >
    <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
    </svg>
    <slot />
  </button>
</template>

<script setup>
const props = defineProps({
  type: { type: String, default: 'button' },
  variant: { type: String, default: 'primary' },
  size: { type: String, default: 'md' },
  disabled: Boolean,
  loading: Boolean,
})

const sizeClasses = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2.5 text-sm',
  lg: 'px-6 py-3 text-base',
}[props.size] || 'px-4 py-2.5 text-sm'

const variantClasses = {
  primary: 'bg-primary-600 hover:bg-primary-700 text-white focus:ring-primary-500',
  outline: 'border border-gray-300 hover:bg-gray-50 text-gray-700 focus:ring-gray-400',
  danger: 'bg-red-600 hover:bg-red-700 text-white focus:ring-red-500',
  ghost: 'hover:bg-gray-100 text-gray-700 focus:ring-gray-400',
}[props.variant] || 'bg-primary-600 hover:bg-primary-700 text-white focus:ring-primary-500'
</script>
