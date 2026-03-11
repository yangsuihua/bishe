<template>
  <div id="blank-layout">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'BlankLayout'
}
</script>

<style scoped>
#blank-layout {
  width: 100%;
  height: 100%;
}
</style>