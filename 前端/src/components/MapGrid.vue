<template>
  <div
    ref="mapWrapper"
    class="map-container"
    tabindex="0"
    @keydown="onKeyDown"
  >
    <!-- 网格背景 -->
    <div
      v-for="(row, rowIndex) in mapData"
      :key="rowIndex"
      class="map-row"
    >
      <div
        v-for="(cell, colIndex) in row"
        :key="colIndex"
        :class="['map-cell', cell === 1 ? 'wall' : 'floor']"
        @dragover.prevent
        @drop="handleDrop(rowIndex, colIndex)"
      />
    </div>

    <!-- 地图上的物品 -->
    <img
      v-for="(item, idx) in internalItems"
      :key="item.name + '-' + idx"
      class="item-icon"
      :src="getItemIcon(item.name)"
      :style="cellPositionStyle(item.position)"
      :alt="item.name"
    >

    <!-- 角色图标 -->
    <img
      class="character-icon"
      :src="characterIcon"
      :style="cellPositionStyle(characterPosition)"
      alt="Character"
      @load="onIconLoad"
      @error="onIconError"
    >
  </div>
</template>

<script>
import chest from '@/assets/chest.png';
import player from '@/assets/player.png';

export default {
  name: 'MapGrid',
  props: {
    mapData: Array,
    items: Array,
    characterIcon: {
      type: String,
      default: player
    },
    initialPosition: {
      type: Object,
      default: () => ({ row: 1, col: 1 })
    },
    cellSize: {
      type: Number,
      default: 48
    }
  },
  data() {
    return {
      characterPosition: { ...this.initialPosition },
      internalItems: [],
      iconDebug: {
        loaded: false,
        error: false,
        errorMessage: ''
      }
    };
  },
  watch: {
    items: {
      immediate: true,
      handler(newList) {
        this.internalItems = newList.map(item => ({
          name: item.name,
          position: { ...item.position }
        }));
      }
    }
  },
  mounted() {
    this.$nextTick(() => {
      if (this.$refs.mapWrapper) {
        this.$refs.mapWrapper.focus();
      }

    });
  },
  methods: {
    onKeyDown(e) {
      const { row, col } = this.characterPosition;
      let nextRow = row, nextCol = col;

      switch (e.key) {
        case 'ArrowUp': nextRow--; break;
        case 'ArrowDown': nextRow++; break;
        case 'ArrowLeft': nextCol--; break;
        case 'ArrowRight': nextCol++; break;
        default: return;
      }

      e.preventDefault();
      if (
        nextRow >= 0 &&
        nextRow < this.mapData.length &&
        nextCol >= 0 &&
        nextCol < this.mapData[0].length &&
        this.mapData[nextRow][nextCol] !== 1
      ) {
        this.characterPosition = { row: nextRow, col: nextCol };
        this.$emit('move', { ...this.characterPosition });
        this.checkItemCollision();
      }
    },

    checkItemCollision() {
      const foundIndex = this.internalItems.findIndex(
        it =>
          it.position.row === this.characterPosition.row &&
          it.position.col === this.characterPosition.col
      );
      if (foundIndex !== -1) {
        const item = this.internalItems[foundIndex];
        this.$emit('pick-item', item.name);
        this.internalItems.splice(foundIndex, 1);
      }
    },

    cellPositionStyle(position) {
      const size = this.cellSize;
      return {
        position: 'absolute',
        top: `${position.row * size}px`,
        left: `${position.col * size}px`,
        width: `${size}px`,
        height: `${size}px`,
        zIndex: 10,
        border: this.iconDebug.error ? '2px solid red' : '1px dotted transparent'
      };
    },

    getItemIcon() {
      return chest;
    },

    onIconLoad() {
      this.iconDebug.loaded = true;
      this.iconDebug.error = false;
    },

    onIconError(e) {
      this.iconDebug.loaded = false;
      this.iconDebug.error = true;
      this.iconDebug.errorMessage = e.message || '图标加载失败';
      console.error('角色图标加载失败:', e);
    },

    // ✅ 拖放物品到地图格子时触发
    handleDrop(row, col) {
      const itemName = event.dataTransfer.getData('text/plain');
      if (!itemName) return;
      this.$emit('drop-on-map', { itemName, row, col });
    }
  }
};
</script>

<style scoped>
.map-container {
  position: relative;
  outline: none;
  display: inline-block;
  background-color: #0a1128;
  padding: 4px;
  border-radius: 8px;
}

.map-row {
  display: flex;
}

.map-cell {
  width: 48px;
  height: 48px;
  box-sizing: border-box;
  border: 1px dotted transparent;
}
.floor {
  background-color: #1b263b;
}
.wall {
  background-color: #4a4a4a;
}

.character-icon {
  z-index: 10;
  pointer-events: none;
  object-fit: contain;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 0, 0, 0.1);
}

.item-icon {
  z-index: 5;
  pointer-events: none;
}
</style>
