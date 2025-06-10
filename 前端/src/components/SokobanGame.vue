<template>
  <div class="sokoban-wrapper">
    <!-- 游戏标题和状态 -->
    <div class="game-header">
      <h3 class="game-title">
        🎮 推箱子小游戏
      </h3>
      <div class="game-stats">
        <span class="stat-item">
          <span class="stat-icon">👣</span>
          <span class="stat-value">{{ moves }}</span>
          <span class="stat-label">步数</span>
        </span>
        <span
          v-if="isCompleted"
          class="win-message"
        >
          <span class="win-icon">🎉</span>
          <span class="win-text">恭喜完成所有目标！</span>
        </span>
      </div>
    </div>

    <!-- 游戏地图 -->
    <div class="sokoban-game">
      <div
        v-for="(row, y) in gameMap"
        :key="y"
        class="sokoban-row"
      >
        <div
          v-for="(cell, x) in row"
          :key="`${x}-${y}`"
          :class="getCellClass(cell)"
          class="sokoban-cell"
        >
          <span class="cell-content">{{ getCellContent(cell) }}</span>
        </div>
      </div>
    </div>

    <!-- 控制按钮 -->
    <div class="game-controls">
      <button
        v-if="!gameStarted"
        class="control-btn start-btn"
        @click="startGame"
      >
        <span class="btn-icon">🚀</span>
        <span class="btn-text">开始游戏</span>
      </button>
      <button
        v-else
        class="control-btn reset-btn"
        @click="resetGame"
      >
        <span class="btn-icon">🔄</span>
        <span class="btn-text">重新开始</span>
      </button>
    </div>
  </div>
</template>
<script>
export default {
  name: 'SokobanGame',
  props: {
    // 传入的目标坐标列表：作为目标渲染
    targets: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      rows: 8,
      cols: 10,
      gameMap: [],
      playerPos: { x: 1, y: 1 },
      moves: 0,
      isCompleted: false,
      gameStarted: false
    };
  },
  watch: {
    // 当目标更新时，自动重置地图（若已开始）
    targets: {
      handler() {
        if (this.gameStarted) this.startGame();
      }
    }
  },
  mounted() {
    window.addEventListener('keydown', this.handleKeyPress);
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.handleKeyPress);
  },
  methods: {
    startGame() {
      // 创建全地板地图
      const map = Array.from({ length: this.rows }, () => Array(this.cols).fill(0));
      // 渲染目标格
      this.targets.forEach(([x, y]) => {
        if (y >= 0 && y < this.rows && x >= 0 && x < this.cols) {
          map[y][x] = 3;
        }
      });
      // 随机初始化箱子，数量等于目标数，且不在地图边缘
      let count = this.targets.length;
      while (count > 0) {
        // 仅在 [1, rows-2] 和 [1, cols-2] 区间取值，避免边缘
        const ry = Math.floor(Math.random() * (this.rows - 2)) + 1;
        const rx = Math.floor(Math.random() * (this.cols - 2)) + 1;
        if (map[ry][rx] === 0) {
          map[ry][rx] = 2;
          count--;
        }
      }
      // 放置玩家
      let { x: px, y: py } = this.playerPos;
      if (map[py][px] !== 0) {
        outer: for (let y = 0; y < this.rows; y++) {
          for (let x = 0; x < this.cols; x++) {
            if (map[y][x] === 0) { px = x; py = y; break outer; }
          }
        }
      }
      map[py][px] = 4;
      this.playerPos = { x: px, y: py };
      this.gameMap = map;
      this.moves = 0;
      this.isCompleted = false;
      this.gameStarted = true;
    },
    resetGame() {
      this.startGame();
    },
    handleKeyPress(e) {
      if (!this.gameStarted || this.isCompleted) return;
      const dirs = {
        arrowup: [0, -1], w: [0, -1],
        arrowdown: [0, 1], s: [0, 1],
        arrowleft: [-1, 0], a: [-1, 0],
        arrowright: [1, 0], d: [1, 0]
      };
      const dir = dirs[e.key.toLowerCase()];
      if (dir) {
        e.preventDefault();
        this.movePlayer(...dir);
      }
    },
    movePlayer(dx, dy) {
      const { x, y } = this.playerPos;
      const nx = x + dx, ny = y + dy;
      if (nx < 0 || nx >= this.cols || ny < 0 || ny >= this.rows) return;
      const map = this.gameMap.map(r => [...r]);
      const dest = map[ny][nx];
      if (dest === 2) {
        // 推箱子
        const bx = nx + dx, by = ny + dy;
        if (by < 0 || by >= this.rows || bx < 0 || bx >= this.cols) return;
        if (map[by][bx] === 0 || map[by][bx] === 3) {
          map[by][bx] = 2;
          map[ny][nx] = 4;
          map[y][x] = 0;
          this.playerPos = { x: nx, y: ny };
          this.moves++;
          this.checkCompletion(map);
        }
      } else if (dest === 0 || dest === 3) {
        // 普通移动或走到目标
        map[y][x] = 0;
        map[ny][nx] = 4;
        this.playerPos = { x: nx, y: ny };
        this.moves++;
        this.checkCompletion(map);
      }
      this.gameMap = map;
    },
    checkCompletion(map) {
      const flat = map.flat();
      const goalCount = this.targets.length;
      const onGoals = this.targets.filter(
        ([gx, gy]) => flat[gy * this.cols + gx] === 2
      ).length;
      if (onGoals === goalCount) {
        this.isCompleted = true;
        this.$emit('completed');
      }
    },
    getCellClass(cell) {
      return {
        'cell-floor': cell === 0,
        'cell-target': cell === 3,
        'cell-box': cell === 2,
        'cell-player': cell === 4
      };
    },
    getCellContent(cell) {
      const icons = { 3: '🎯', 2: '📦', 4: '🤖' };
      return icons[cell] || '';
    }
  }
};
</script>


<style scoped>
.sokoban-wrapper { display: flex; flex-direction: column; gap: 16px; width: 100%; }
.game-header { display: flex; justify-content: space-between; align-items: center; }
.sokoban-game { display: inline-block; }
.sokoban-row { display: flex; }
.sokoban-cell { width: 40px; height: 40px; display: flex; justify-content: center; align-items: center; border: 1px solid #444; box-sizing: border-box; }
.cell-floor { background: #333; }
.cell-target { background: #112; }
.cell-box { background: #882; }
.cell-player { background: #288; }
.cell-content { font-size: 18px; }
.control-btn { margin-top: 8px; padding: 6px 12px; border: none; border-radius: 4px; cursor: pointer; }
.start-btn { background: #0a0; color: #fff; }
.reset-btn { background: #a00; color: #fff; }
.win-message { color: #ff0; font-weight: bold; }
.sokoban-container {
  width: 100%;
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}
</style>
