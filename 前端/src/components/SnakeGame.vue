<template>
  <div class="snake-wrapper">
    <div class="game-header">
      <h3 class="game-title">🐍 贪吃蛇</h3>
      <div class="game-stats">
        <span class="stat-item">
          <span class="stat-icon">🍎</span>
          <span class="stat-value">{{ score }}</span>
          <span class="stat-label">得分</span>
        </span>
        <span class="stat-item">
          <span class="stat-icon">📏</span>
          <span class="stat-value">{{ snake.length }}</span>
          <span class="stat-label">长度</span>
        </span>
        <span class="stat-item">
          <span class="stat-icon">🎯</span>
          <span class="stat-value">{{ remainingFood }}</span>
          <span class="stat-label">剩余</span>
        </span>
        <span v-if="gameOver" class="stat-item game-over-message">
          <span class="game-over-icon">💀</span>
          <span class="game-over-text">游戏结束！</span>
        </span>
      </div>
    </div>

    <div class="snake-game">
      <div v-for="y in gridSize" :key="y" class="snake-row">
        <div
          v-for="x in gridSize"
          :key="`${x}-${y}`"
          :class="getCellClass(x-1, y-1)"
          class="snake-cell"
        >
          <span v-if="isTarget(x-1, y-1)" class="food-icon">🍎</span>
          <span v-if="isSnakeHead(x-1, y-1)" class="snake-head">👀</span>
        </div>
      </div>
    </div>

    <div class="game-controls">
      <button v-if="!gameStarted" class="control-btn start-btn" @click="startGame">
        <span class="btn-icon">🚀</span>
        <span class="btn-text">开始游戏</span>
      </button>
      <button v-else-if="gameOver" class="control-btn reset-btn" @click="resetGame">
        <span class="btn-icon">🔄</span>
        <span class="btn-text">重新开始</span>
      </button>
      <button v-else class="control-btn pause-btn" @click="togglePause">
        <span class="btn-icon">{{ paused ? '▶️' : '⏸️' }}</span>
        <span class="btn-text">{{ paused ? '继续' : '暂停' }}</span>
      </button>
    </div>

    <div class="control-hint">
      使用 ↑ ↓ ← → 或 WASD 控制方向
    </div>

    <div v-if="showDebug" class="debug-info">
      <p>目标数量: {{ totalTargets }}</p>
      <p>剩余食物: {{ remainingFood }}</p>
      <p>当前食物位置: {{ currentFood ? `(${currentFood.x}, ${currentFood.y})` : '无' }}</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'SnakeGame',
  props: {
    targets: Array,
    items: Array,
    showDebug: Boolean
  },
  data() {
    const size = 20;
    const center = { x: Math.floor(size / 2), y: Math.floor(size / 2) };
    return {
      gridSize: size,
      snake: [center],
      direction: { x: 0, y: 0 },
      nextDirection: { x: 0, y: 0 },
      score: 0,
      gameStarted: false,
      gameOver: false,
      paused: false,
      gameLoop: null,
      speed: 150,
      foodQueue: [],
      currentFood: null
    };
  },
  computed: {
    totalTargets() {
      if (this.items && this.items.length) {
        return this.items.reduce((sum, item) => sum + (item.quantity || 1), 0);
      }
      return this.targets.length;
    },
    remainingFood() {
      // 仅计算队列中剩余的食物数量，不包括当前食物
      return this.foodQueue.length;
    }
  },
  watch: {
    targets: { handler: 'generateFoodQueue', immediate: true },
    items: { handler: 'generateFoodQueue', deep: true, immediate: true }
  },
  mounted() {
    window.addEventListener('keydown', this.handleKeyPress);
    this.generateFoodQueue();
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.handleKeyPress);
    clearInterval(this.gameLoop);
  },
  methods: {
    generateFoodQueue() {
      this.foodQueue = [];
      // 如果房间物品清空，则清空食物队列并结束游戏
      if (!this.items || this.items.length === 0) {
        this.currentFood = null;
        if (this.gameStarted && !this.gameOver) {
          this.endGame();
        }
        return;
      }
      if (this.items && this.items.length) {
        this.items.forEach(item => {
          const qty = item.quantity || 1;
          for (let i = 0; i < qty; i++) {
            this.foodQueue.push({ x: item.position.col, y: item.position.row, itemName: item.name });
          }
        });
      } else if (this.targets && this.targets.length) {
        this.targets.forEach(([col, row]) => {
          this.foodQueue.push({ x: col, y: row, itemName: '食物' });
        });
      }
      this.shuffleArray(this.foodQueue);
      if (this.gameStarted && !this.currentFood) {
        this.currentFood = this.foodQueue.shift() || null;
      }
    },
    shuffleArray(arr) {
      for (let i = arr.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [arr[i], arr[j]] = [arr[j], arr[i]];
      }
    },
    startGame() {
      const head = { x: Math.floor(this.gridSize / 2), y: Math.floor(this.gridSize / 2) };
      this.snake = [head];
      this.direction = { x: 1, y: 0 };
      this.nextDirection = { x: 1, y: 0 };
      this.score = 0;
      this.gameStarted = true;
      this.gameOver = false;
      this.paused = false;
      this.speed = 150;
      this.generateFoodQueue();
      this.currentFood = this.foodQueue.shift() || null;
      if (!this.currentFood) return this.endGame();
      this.gameLoop = setInterval(this.updateGame, this.speed);
    },
    resetGame() {
      clearInterval(this.gameLoop);
      this.startGame();
    },
    togglePause() {
      this.paused = !this.paused;
      this.paused ? clearInterval(this.gameLoop) : (this.gameLoop = setInterval(this.updateGame, this.speed));
    },
    updateGame() {
      if (this.gameOver || this.paused) return;
      this.direction = { ...this.nextDirection };
      const head = { x: this.snake[0].x + this.direction.x, y: this.snake[0].y + this.direction.y };
      if (this.checkCollision(head)) return this.endGame();
      this.snake.unshift(head);
      if (this.currentFood && head.x === this.currentFood.x && head.y === this.currentFood.y) {
        this.score += 10;
        this.$emit('item-collected', this.currentFood.itemName);
        this.$emit('score-update', this.score);
        this.currentFood = this.foodQueue.shift() || null;
        if (!this.currentFood) return this.endGame(true);
      } else {
        this.snake.pop();
      }
    },
    checkCollision(head) {
      if (head.x < 0 || head.x >= this.gridSize || head.y < 0 || head.y >= this.gridSize) return true;
      return this.snake.slice(1).some(segment => segment.x === head.x && segment.y === head.y);
    },
    endGame(isVictory = false) {
      this.gameOver = true;
      clearInterval(this.gameLoop);
      this.$emit('score-update', this.score);
      if (isVictory) this.$emit('completed');
    },
    handleKeyPress(e) {
      if (!this.gameStarted || this.gameOver || this.paused) return;
      const key = e.key.toLowerCase();
      if (['arrowup','w'].includes(key) && this.direction.y === 0) this.nextDirection = { x: 0, y: -1 };
      if (['arrowdown','s'].includes(key) && this.direction.y === 0) this.nextDirection = { x: 0, y: 1 };
      if (['arrowleft','a'].includes(key) && this.direction.x === 0) this.nextDirection = { x: -1, y: 0 };
      if (['arrowright','d'].includes(key) && this.direction.x === 0) this.nextDirection = { x: 1, y: 0 };
    },
    getCellClass(x, y) {
      return {
        'snake-body': this.snake.some(seg => seg.x === x && seg.y === y),
        'snake-head-cell': this.snake[0].x === x && this.snake[0].y === y,
        'food-cell': this.currentFood && this.currentFood.x === x && this.currentFood.y === y
      };
    },
    isTarget(x, y) {
      return this.currentFood && this.currentFood.x === x && this.currentFood.y === y;
    },
    isSnakeHead(x, y) {
      return this.snake[0].x === x && this.snake[0].y === y;
    }
  }
};
</script>

<style scoped>
.snake-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
  font-family: 'Share Tech Mono', monospace;
  color: #a0c4ff;
}

.game-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}

.game-title {
  color: #00d4ff;
  font-size: 24px;
  font-weight: bold;
  text-shadow: 0 0 20px rgba(0,212,255,0.8);
  margin: 0;
}

.game-stats {
  display: flex;
  gap: 20px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: center;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(0,0,0,0.3);
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid rgba(0,168,232,0.3);
}

.stat-icon {
  font-size: 16px;
}

.stat-value {
  color: #00d4ff;
  font-weight: bold;
  font-size: 16px;
}

.stat-label {
  color: #7aa7ff;
  font-size: 12px;
}

.game-over-message {
  background: rgba(255,0,0,0.2);
  border-color: rgba(255,0,0,0.5);
  color: #ff6b6b;
}

.snake-game {
  display: flex;
  flex-direction: column;
  width: 100%;
  aspect-ratio: 1 / 1;
  border: 2px solid #00d4ff;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.snake-row {
  display: flex;
  flex: 1;
}

.snake-cell {
  flex: 1;
  border: 1px solid #111;
  background: #111;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.1s;
  position: relative;
}

.snake-body {
  background: #0d0;
}

.snake-head-cell {
  background: #0f0;
}

.food-cell {
  background: rgba(255,0,0,0.2);
}

.food-icon,
.snake-head {
  font-size: 1rem;
  position: relative;
  z-index: 2;
}

.game-controls {
  display: flex;
  justify-content: center;
  margin: 16px 0;
}

.control-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, rgba(0,30,60,0.8), rgba(0,50,100,0.8));
  border: 2px solid rgba(0,168,232,0.3);
  border-radius: 12px;
  color: #a0c4ff;
  font-family: 'Share Tech Mono', monospace;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.control-btn:hover {
  background: linear-gradient(135deg, rgba(0,50,100,0.9), rgba(0,80,160,0.9));
  border-color: #00d4ff;
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0,212,255,0.4);
}

.start-btn:hover {
  background: linear-gradient(135deg, rgba(0,150,0,0.8), rgba(0,200,0,0.8));
  border-color: #00ff00;
}

.reset-btn:hover {
  background: linear-gradient(135deg, rgba(255,100,0,0.8), rgba(255,150,0,0.8));
  border-color: #ff6b00;
}

.btn-icon {
  font-size: 16px;
}

.control-hint {
  text-align: center;
  color: #7aa7ff;
  font-size: 12px;
  margin-top: 8px;
}

.debug-info {
  background: rgba(0,0,0,0.5);
  border: 1px solid rgba(0,168,232,0.3);
  border-radius: 8px;
  padding: 12px;
  margin-top: 16px;
  font-size: 12px;
  color: #7aa7ff;
}

.debug-info p {
  margin: 4px 0;
}
</style>
