<template>

  <div class="player-status">
    <h3>玩家状态</h3>
    <p>姓名：{{ name }}</p>
    <p>当前位置：{{ currentRoom }}</p>
  </div>
</template>

<script>
export default {
  name: 'PlayerStatus',
  props: {
    name: String,
    currentRoom: String
  }
}
</script>

<style scoped>
.player-status {
  margin-top: 10px;
}
.status-bar {
  padding: 10px;
  font-size: 14px;
  color: white;
  background-color: rgba(0,0,0,0.5);
  border-bottom: 2px solid #ffd700;
}
</style>
