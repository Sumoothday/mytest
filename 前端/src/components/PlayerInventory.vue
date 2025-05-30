<template>
  <div class="player-inventory">
    <h3>玩家背包</h3>
    <ul>
      <li v-for="item in inventory" :key="item.name">
        {{ item.name }} - 重量: {{ item.weight }}
      </li>
    </ul>
    <p>总重量：{{ totalWeight }} / {{ weightLimit }}</p>
  </div>
</template>

<script>
export default {
  name: 'PlayerInventory',
  props: {
    inventory: Array,
    totalWeight: Number,
    weightLimit: Number
  }
}
</script>

<style scoped>
ul { padding-left: 1em; }
</style>
