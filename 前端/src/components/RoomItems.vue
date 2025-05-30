// ✅ RoomItems.vue
<template>
  <div class="room-items">
    <h3>房间内物品</h3>
    <ul>
      <li
        v-for="item in roomItems"
        :key="item.name"
      >
        {{ item.name }} - 重量: {{ item.weight }}
      </li>
    </ul>
  </div>
</template>

<script>
export default {
  name: 'RoomItems',
  props: {
    roomItems: Array
  }
};
</script>

<style scoped>
ul { padding-left: 1em; }
</style>

