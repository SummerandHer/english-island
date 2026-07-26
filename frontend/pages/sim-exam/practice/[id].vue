<script setup lang="ts">
const route = useRoute()
await navigateTo(`/islands/exam/practice/${route.params.id}`, { redirectCode: 301 })
</script>
