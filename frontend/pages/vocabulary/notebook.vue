<script setup lang="ts">
const route = useRoute()
await navigateTo({ path: '/vocabulary', query: { tab: 'notebook', ...route.query } }, { replace: true })
</script>
