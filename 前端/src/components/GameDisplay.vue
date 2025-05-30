<template>
  <div class="game-box game-display">
    <div class="room-grid">
      <div
        v-for="(row, rowIndex) in paddedLayout"
        :key="rowIndex"
        class="grid-row"
      >
        <div
          v-for="(cell, colIndex) in row"
          :key="colIndex"
          class="grid-cell"
        >
          <img
            v-if="cell === 'player'"
            src="@/assets/player.png"
            alt="player"
          >
          <img
            v-else-if="cell === 'monster'"
            src="@/assets/monster.png"
            alt="monster"
          >
          <img
            v-else-if="cell === 'chest'"
            src="@/assets/chest.png"
            alt="chest"
          >
          <img
            v-else-if="cell === 'trap'"
            src="@/assets/chest.png"
            alt="trap"
          >
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';

export default {
  name: 'GameDisplay',
  computed: {
    ...mapState(['roomMap', 'roomName']),
    currentRoomLayout() {
      const room = this.roomMap.rooms.find(r => r.name === this.roomName);
      return room && room.layout ? room.layout : [];
    },
    paddedLayout() {
      const ROWS = 6;
      const COLS = 6;
      const layout = this.currentRoomLayout.map(r => Array.from(r));
      while (layout.length < ROWS) layout.push([]);
      return layout.map(row => {
        const newRow = Array.from(row);
        while (newRow.length < COLS) newRow.push(null);
        return newRow;
      });
    }
  }
};
</script>

<style scoped>
.room-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
  background: transparent;
  padding: 0;
  margin: 0;
}

.grid-cell {
  position: relative;
  aspect-ratio: 1;
  background: linear-gradient(145deg, #30323f, #1c1e2c);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  /* More pronounced 3D effect */
  box-shadow:
    6px 6px 12px rgba(0, 0, 0, 0.7),
    -6px -6px 12px rgba(80, 80, 100, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: box-shadow 0.3s;
  overflow: hidden;
}
.grid-cell:hover {
  box-shadow:
    2px 2px 5px rgba(0, 0, 0, 0.8),
    -2px -2px 5px rgba(100, 100, 120, 0.3);
}

.grid-cell img {
  width: 60%;
  height: 60%;
}
</style>
