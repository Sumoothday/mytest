<template>
  <div
    class="side-panel"
    :class="{ open: isOpen }"
  >
    <div class="header">
      <span>📋 游戏信息</span>
      <button @click="$emit('close')">
        ✖
      </button>
    </div>

    <div class="section">
      <h4>👤 玩家状态</h4>
      <p>姓名：{{ name }}</p>
      <p>位置：{{ currentRoom }}</p>
    </div>

    <div class="section">
      <h4>🎒 玩家背包</h4>
      <p>总重量：{{ totalWeight }} / {{ weightLimit }}</p>
      <ul>
        <li
          v-for="item in inventory"
          :key="item.name"
        >
          {{ item.name }} x{{ item.quantity }}
        </li>
      </ul>
    </div>

    <div class="section">
      <h4>📦 房间内物品</h4>
      <ul>
        <li
          v-for="item in roomItems"
          :key="item.name"
        >
          {{ item.name }}
        </li>
      </ul>
    </div>
  </div>
</template>

<script>
export default {
  props: ['isOpen', 'name', 'currentRoom', 'inventory', 'totalWeight', 'weightLimit', 'roomItems']
};
</script>

<style scoped>
.side-panel {
  position: fixed;
  top: 0;
  right: -350px;
  width: 300px;
  height: 100%;
  background: rgba(30, 30, 30, 0.95);
  color: white;
  transition: right 0.3s ease;
  padding: 20px;
  z-index: 1000;
  overflow-y: auto;
  box-shadow: -2px 0 10px rgba(0,0,0,0.4);
}
.side-panel.open {
  right: 0;
}
.header {
  display: flex;
  justify-content: space-between;
  font-weight: bold;
  margin-bottom: 15px;
}
.section {
  margin-bottom: 20px;
}
</style>
