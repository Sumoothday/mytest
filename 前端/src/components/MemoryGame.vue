<template>
  <div class="gold-miner-wrapper">
    <div class="game-header">
      <h3 class="game-title">🏆 黄金矿工</h3>
      <div class="game-stats">
        <span class="stat-item">
          <span class="stat-icon">💰</span>
          <span class="stat-value">{{ score }}</span>
          <span class="stat-label">得分</span>
        </span>
        <span class="stat-item">
          <span class="stat-icon">💎</span>
          <span class="stat-value">{{ collectedItems.length }}</span>
          <span class="stat-label">已收集</span>
        </span>
        <span class="stat-item">
          <span class="stat-icon">🎯</span>
          <span class="stat-value">{{ remainingItems }}</span>
          <span class="stat-label">剩余</span>
        </span>
        <span v-if="gameCompleted" class="stat-item game-over-message">
          <span class="game-over-icon">🎉</span>
          <span class="game-over-text">游戏完成！</span>
        </span>
      </div>
    </div>

    <div class="gold-miner-game">
      <canvas
        ref="gameCanvas"
        :width="canvasWidth"
        :height="canvasHeight"
        @click="handleCanvasClick"
        class="game-canvas"
      />
    </div>

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
        v-else-if="gameCompleted"
        class="control-btn reset-btn"
        @click="resetGame"
      >
        <span class="btn-icon">🔄</span>
        <span class="btn-text">重新开始</span>
      </button>
      <button
        v-else
        class="control-btn pause-btn"
        @click="togglePause"
      >
        <span class="btn-icon">{{ paused ? '▶️' : '⏸️' }}</span>
        <span class="btn-text">{{ paused ? '继续' : '暂停' }}</span>
      </button>
    </div>

    <div class="control-hint">
      钩子自动摆动，点击画布发射钩子抓取物品
    </div>

    <div class="game-instructions">
      <div class="instruction-item">
        <span class="instruction-icon">💰</span>
        <span>金币 - 50分</span>
      </div>
      <div class="instruction-item">
        <span class="instruction-icon">💎</span>
        <span>宝石 - 100分</span>
      </div>
      <div class="instruction-item">
        <span class="instruction-icon">🪶</span>
        <span>羽毛 - 30分</span>
      </div>
      <div class="instruction-item">
        <span class="instruction-icon">🗝️</span>
        <span>魔法钥匙 - 200分</span>
      </div>
    </div>

    <div v-if="showDebug" class="debug-info">
      <p>游戏状态: {{ gameState }}</p>
      <p>钩子角度: {{ (hook.angle * 180 / Math.PI).toFixed(1) }}°</p>
      <p>钩子长度: {{ hook.length.toFixed(1) }}</p>
      <p>物品总数: {{ totalItems }}</p>
      <p>已收集: {{ collectedItems.length }}</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'GoldMinerGame',
  props: {
    targets: {
      type: Array,
      default: () => []
    },
    items: {
      type: Array,
      default: () => []
    },
    showDebug: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      canvasWidth: 600,
      canvasHeight: 400,
      gameStarted: false,
      gameCompleted: false,
      paused: false,
      gameState: 'ready', // 'ready', 'swinging', 'extending', 'retracting'
      score: 0,
      hook: {
        angle: 0,
        length: 50,
        x: 300,
        y: 60,
        swingDirection: 1,
        caughtItem: null
      },
      gameItems: [],
      collectedItems: [],
      gameLoop: null,
      animationFrameId: null, // 使用 requestAnimationFrame
      HOOK_START_X: 300,
      HOOK_START_Y: 60,
      SWING_SPEED: 0.02,
      EXTEND_SPEED: 3,
      RETRACT_SPEED: 2,
      MAX_HOOK_LENGTH: 300
    };
  },
  computed: {
    totalItems() {
      if (this.items.length) {
        return this.items.reduce((sum, it) => sum + (it.quantity || 1), 0);
      }
      return this.targets.length;
    },
    remainingItems() {
      return this.gameItems.filter(item => !item.collected).length;
    }
  },
  watch: {
    targets: {
      handler() {
        this.generateGameItems();
        // 重置游戏状态
        this.gameCompleted = false;
        this.collectedItems = [];
        this.score = 0;
      },
      immediate: true
    },
    items: {
      handler() {
        this.generateGameItems();
        // 重置游戏状态
        this.gameCompleted = false;
        this.collectedItems = [];
        this.score = 0;
      },
      immediate: true,
      deep: true
    }
  },
  mounted() {
    this.generateGameItems();
    // 页面加载后直接让钩子开始摆动
    this.gameStarted = true;
    this.gameState = 'swinging';
    this.startGameLoop();
  },
  beforeUnmount() {
    this.stopGameLoop();
  },
  methods: {
    generateGameItems() {
      this.gameItems = [];
      if (this.items.length) {
        let id = 0;
        this.items.forEach(it => {
          const qty = it.quantity || 1;
          for (let i = 0; i < qty; i++) {
            this.gameItems.push({
              id: id++,
              name: it.name,
              x: Math.random() * (this.canvasWidth - 100) + 50,
              y: Math.random() * 200 + 200,
              ...this.getItemProperties(it.name),
              collected: false
            });
          }
        });
      } else {
        this.targets.forEach(([col, row], idx) => {
          this.gameItems.push({
            id: idx,
            name: '金币',
            x: col * 30 + 50,
            y: row * 30 + 200,
            ...this.getItemProperties('金币'),
            collected: false
          });
        });
      }
    },
    getItemProperties(name) {
      const props = {
        '金币': { width: 25, height: 25, value: 50, color: '#FFD700', icon: '💰' },
        '宝石': { width: 30, height: 30, value: 100, color: '#FF1493', icon: '💎' },
        '羽毛': { width: 20, height: 35, value: 30, color: '#87CEEB', icon: '🪶' },
        '魔法钥匙': { width: 35, height: 20, value: 200, color: '#9370DB', icon: '🗝️' },
        '古籍': { width: 40, height: 30, value: 150, color: '#8B4513', icon: '📚' },
        '神秘图纸': { width: 45, height: 35, value: 180, color: '#32CD32', icon: '📜' }
      };
      return props[name] || props['金币'];
    },
    startGame() {
      this.gameStarted = true;
      this.score = 0;
      this.collectedItems = [];
      this.gameCompleted = false;
      this.paused = false;
      this.gameState = 'swinging';
      this.hook = {
        angle: 0,
        length: 50,
        x: this.HOOK_START_X,
        y: this.HOOK_START_Y,
        swingDirection: 1,
        caughtItem: null
      };
      this.generateGameItems();
      this.startGameLoop();
    },
    resetGame() {
      this.startGame();
    },
    togglePause() {
      this.paused = !this.paused;
    },
    startGameLoop() {
      const animate = () => {
        this.updateGame();
        this.drawGame();
        this.animationFrameId = requestAnimationFrame(animate);
      };
      animate();
    },
    stopGameLoop() {
      if (this.animationFrameId) {
        cancelAnimationFrame(this.animationFrameId);
        this.animationFrameId = null;
      }
    },
    updateGame() {
      // 游戏完成或暂停时不更新
      if (this.gameCompleted || this.paused) return;

      if (this.gameState === 'swinging') {
        this.updateHookSwing();
      } else if (this.gameState === 'extending') {
        this.updateHookExtend();
      } else if (this.gameState === 'retracting') {
        this.updateHookRetract();
      }
    },
    updateHookSwing() {
      let angle = this.hook.angle + this.SWING_SPEED * this.hook.swingDirection;
      let dir   = this.hook.swingDirection;
      if (angle > Math.PI/3) {
        angle = Math.PI/3;
        dir = -1;
      }
      if (angle < -Math.PI/3) {
        angle = -Math.PI/3;
        dir = 1;
      }
      this.hook.angle = angle;
      this.hook.swingDirection = dir;
      // 更新钩子位置
      this.hook.x = this.HOOK_START_X + Math.sin(this.hook.angle) * this.hook.length;
      this.hook.y = this.HOOK_START_Y + Math.cos(this.hook.angle) * this.hook.length;
    },
    updateHookExtend() {
      const len = this.hook.length + this.EXTEND_SPEED;
      const endX = this.HOOK_START_X + Math.sin(this.hook.angle) * len;
      const endY = this.HOOK_START_Y + Math.cos(this.hook.angle) * len;

      let caught = this.hook.caughtItem;
      if (!caught) {
        caught = this.gameItems.find(it =>
          !it.collected &&
          endX >= it.x - it.width/2 &&
          endX <= it.x + it.width/2 &&
          endY >= it.y - it.height/2 &&
          endY <= it.y + it.height/2
        ) || null;
      }

      if (len >= this.MAX_HOOK_LENGTH ||
        endY >= this.canvasHeight - 20 ||
        endX <= 10 ||
        endX >= this.canvasWidth - 10 ||
        caught
      ) {
        this.gameState = 'retracting';
        this.hook.caughtItem = caught;
      }

      this.hook.length = len;
      this.hook.x = endX;
      this.hook.y = endY;
    },

    updateHookRetract() {
      const speed = this.hook.caughtItem ? this.RETRACT_SPEED * 0.6 : this.RETRACT_SPEED;
      const len = Math.max(50, this.hook.length - speed);

      if (len <= 50) {
        // 处理抓到的物品
        if (this.hook.caughtItem) {
          const it = this.hook.caughtItem;
          it.collected = true;
          this.collectedItems.push(it);
          this.score += it.value;
          this.$emit('item-collected', it.name);
          // 更新得分到父组件
          this.$emit('score-update', this.score);
        }

        // 检查游戏是否完成
        const allCollected = this.gameItems.every(it => it.collected);
        if (allCollected && this.gameItems.length > 0) {
          this.gameCompleted = true;
          this.stopGameLoop();

          // 🔥 关键修复：触发游戏完成事件
          this.$emit('completed');

          console.log('🎉 黄金矿工游戏完成！触发 completed 事件');
        } else {
          this.gameState = 'swinging';
        }

        // 重置钩子
        this.hook = {
          angle: 0,
          length: 50,
          x: this.HOOK_START_X,
          y: this.HOOK_START_Y,
          swingDirection: 1,
          caughtItem: null
        };
      } else {
        this.hook.length = len;
        this.hook.x = this.HOOK_START_X + Math.sin(this.hook.angle) * len;
        this.hook.y = this.HOOK_START_Y + Math.cos(this.hook.angle) * len;
      }
    },
    handleCanvasClick() {
      if (this.gameState === 'swinging' && this.gameStarted && !this.paused) {
        this.gameState = 'extending';
      }
    },
    drawGame() {
      const c = this.$refs.gameCanvas;
      if (!c) return;
      const ctx = c.getContext('2d');
      ctx.clearRect(0, 0, this.canvasWidth, this.canvasHeight);

      // 背景
      const g = ctx.createLinearGradient(0, 0, 0, this.canvasHeight);
      g.addColorStop(0, '#87CEEB');
      g.addColorStop(0.3, '#DEB887');
      g.addColorStop(1, '#8B4513');
      ctx.fillStyle = g;
      ctx.fillRect(0, 0, this.canvasWidth, this.canvasHeight);

      // 地面线
      ctx.strokeStyle = '#654321';
      ctx.lineWidth = 3;
      ctx.setLineDash([10, 5]);
      ctx.beginPath();
      ctx.moveTo(0, 150);
      ctx.lineTo(this.canvasWidth, 150);
      ctx.stroke();
      ctx.setLineDash([]);

      // 矿工小屋
      ctx.fillStyle = '#8B4513';
      ctx.fillRect(this.HOOK_START_X - 40, 20, 80, 40);
      ctx.fillStyle = '#FF4500';
      ctx.beginPath();
      ctx.moveTo(this.HOOK_START_X - 45, 20);
      ctx.lineTo(this.HOOK_START_X, 5);
      ctx.lineTo(this.HOOK_START_X + 45, 20);
      ctx.closePath();
      ctx.fill();

      // 钩子绳索
      ctx.strokeStyle = '#8B4513';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(this.HOOK_START_X, this.HOOK_START_Y);
      ctx.lineTo(this.hook.x, this.hook.y);
      ctx.stroke();

      // 钩子
      ctx.fillStyle = '#C0C0C0';
      ctx.beginPath();
      ctx.arc(this.hook.x, this.hook.y, 8, 0, Math.PI * 2);
      ctx.fill();

      // 钩尖
      ctx.fillStyle = '#696969';
      ctx.beginPath();
      ctx.arc(this.hook.x + 6, this.hook.y + 6, 4, 0, Math.PI * 2);
      ctx.fill();

      // 物品
      this.gameItems.forEach(it => {
        if (!it.collected) {
          ctx.fillStyle = it.color;
          ctx.fillRect(it.x - it.width/2, it.y - it.height/2, it.width, it.height);
          ctx.strokeStyle = '#000';
          ctx.lineWidth = 2;
          ctx.strokeRect(it.x - it.width/2, it.y - it.height/2, it.width, it.height);
          ctx.fillStyle = '#FFF';
          ctx.font = '10px Arial';
          ctx.textAlign = 'center';
          ctx.fillText(it.name, it.x, it.y + it.height/2 + 12);
        }
      });

      // 被钩住的物品
      if (this.hook.caughtItem && !this.hook.caughtItem.collected) {
        const it = this.hook.caughtItem;
        ctx.fillStyle = it.color;
        ctx.fillRect(this.hook.x - it.width/2, this.hook.y + 10, it.width, it.height);
        ctx.strokeStyle = '#000';
        ctx.lineWidth = 2;
        ctx.strokeRect(this.hook.x - it.width/2, this.hook.y + 10, it.width, it.height);
      }

      // 提示文字
      if (this.gameState === 'swinging' && !this.paused) {
        ctx.fillStyle = '#FFF';
        ctx.font = 'bold 16px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('点击发射钩子!', this.canvasWidth/2, this.canvasHeight - 20);
      }

      // 暂停提示
      if (this.paused) {
        ctx.fillStyle = 'rgba(0,0,0,0.7)';
        ctx.fillRect(0, 0, this.canvasWidth, this.canvasHeight);
        ctx.fillStyle = '#FFD700';
        ctx.font = 'bold 24px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('游戏暂停', this.canvasWidth/2, this.canvasHeight/2);
      }

      // 完成遮罩
      if (this.gameCompleted) {
        ctx.fillStyle = 'rgba(0,0,0,0.7)';
        ctx.fillRect(0, 0, this.canvasWidth, this.canvasHeight);
        ctx.fillStyle = '#FFD700';
        ctx.font = 'bold 24px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('🎉 恭喜完成! 🎉', this.canvasWidth/2, this.canvasHeight/2 - 20);
        ctx.fillText(`最终得分: ${this.score}`, this.canvasWidth/2, this.canvasHeight/2 + 20);
      }
    }
  }
};
</script>

<style scoped>
.gold-miner-wrapper {
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
  color: #FFD700;
  font-size: 24px;
  font-weight: bold;
  text-shadow: 0 0 20px rgba(255,215,0,0.8);
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
  border: 1px solid rgba(255,215,0,0.3);
}

.stat-icon {
  font-size: 16px;
}

.stat-value {
  color: #FFD700;
  font-weight: bold;
  font-size: 16px;
}

.stat-label {
  color: #7aa7ff;
  font-size: 12px;
}

.game-over-message {
  background: rgba(0,255,0,0.2);
  border-color: rgba(0,255,0,0.5);
  color: #00ff00;
}

.gold-miner-game {
  display: flex;
  justify-content: center;
  border-radius: 8px;
  overflow: hidden;
  width: 100%;
}

.game-canvas {
  width: 100% !important;
  height: auto !important;
  max-width: 600px;
  border: 3px solid #FFD700;
  border-radius: 8px;
  cursor: pointer;
  box-shadow: 0 0 20px rgba(255,215,0,0.3);
  transition: box-shadow 0.3s ease;
}

.game-canvas:hover {
  box-shadow: 0 0 30px rgba(255,215,0,0.5);
}

.game-controls {
  display: flex;
  justify-content: center;
  margin: 16px 0;
  flex-wrap: wrap;
  gap: 8px;
}

.control-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, rgba(0,30,60,0.8), rgba(0,50,100,0.8));
  border: 2px solid rgba(255,215,0,0.3);
  border-radius: 12px;
  color: #a0c4ff;
  font-family: 'Share Tech Mono', monospace;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.control-btn:hover {
  background: linear-gradient(135deg, rgba(0,50,100,0.9), rgba(0,80,160,0.9));
  border-color: #FFD700;
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(255,215,0,0.4);
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

.game-instructions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 8px;
  padding: 12px;
  background: rgba(0,0,0,0.3);
  border: 1px solid rgba(255,215,0,0.3);
  border-radius: 8px;
}

.instruction-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #7aa7ff;
}

.instruction-icon {
  font-size: 14px;
}

.debug-info {
  background: rgba(0,0,0,0.5);
  border: 1px solid rgba(255,215,0,0.3);
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
