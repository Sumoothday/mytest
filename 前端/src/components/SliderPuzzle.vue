<template>
  <div class="pool-game-wrapper">
    <div class="game-header">
      <h3 class="game-title">🎱 台球游戏</h3>
      <div class="game-stats">
        <span class="stat-item">
          <span class="stat-icon">🏆</span>
          <span class="stat-value">{{ score }}</span>
          <span class="stat-label">得分</span>
        </span>
        <span class="stat-item">
          <span class="stat-icon">🎯</span>
          <span class="stat-value">{{ remainingBalls }}</span>
          <span class="stat-label">剩余球</span>
        </span>
        <span class="stat-item">
          <span class="stat-icon">🔥</span>
          <span class="stat-value">{{ currentPlayer }}</span>
          <span class="stat-label">当前玩家</span>
        </span>
      </div>
    </div>

    <canvas
      ref="canvas"
      :width="width"
      :height="height"
      class="pool-canvas"
      @mousedown="startAiming"
      @mousemove="updateAim"
      @mouseup="shoot"
      tabindex="0"
    />

    <div class="control-panel">
      <div class="power-meter" v-if="aiming">
        <div class="power-label">力度</div>
        <div class="power-bar">
          <div class="power-fill" :style="{ width: power + '%' }"></div>
        </div>
        <div class="power-value">{{ Math.round(power) }}%</div>
      </div>
      <div class="control-hint">
        鼠标拖拽瞄准，松开发射 | 球袋中球将被拾取
      </div>
      <button @click="resetGame" class="control-btn">🔄 重新开始</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PoolGame',
  props: {
    // 接收房间物品，每个 item: { name, quantity }
    items: {
      type: Array,
      default: () => [
        { name: '红球', quantity: 3 },
        { name: '黄球', quantity: 2 },
        { name: '蓝球', quantity: 1 }
      ]
    }
  },
  data() {
    return {
      width: 800,
      height: 500,
      balls: [],
      cueBall: null,
      aiming: false,
      aimStart: { x: 0, y: 0 },
      aimEnd: { x: 0, y: 0 },
      power: 0,
      maxPower: 100,
      score: 0,
      currentPlayer: 1,
      gameActive: true,
      rafId: null,
      friction: 0.985,
      pockets: [
        { x: 30, y: 30, radius: 25 },
        { x: 400, y: 25, radius: 25 },
        { x: 770, y: 30, radius: 25 },
        { x: 30, y: 470, radius: 25 },
        { x: 400, y: 475, radius: 25 },
        { x: 770, y: 470, radius: 25 }
      ]
    };
  },
  computed: {
    remainingBalls() {
      return this.balls.filter(b => b.type !== 'cue' && !b.pocketed).length;
    }
  },
  watch: {
    // 📌 关键修复：监听 items 变化
    items: {
      handler(newItems) {
        console.log('🎱 台球游戏检测到物品变化:', newItems);
        // 当 items 发生变化时，重新初始化游戏
        this.initGame();
      },
      deep: true,
      immediate: false // 不需要立即执行，因为 mounted 会调用 initGame
    }
  },
  mounted() {
    this.initGame();
    this.gameLoop();
  },
  beforeUnmount() {
    if (this.rafId) {
      cancelAnimationFrame(this.rafId);
    }
  },
  methods: {
    initGame() {
      console.log('🎱 初始化台球游戏，当前物品:', this.items);

      this.balls = [];
      this.score = 0;
      this.currentPlayer = 1;
      this.gameActive = true;
      this.aiming = false;
      this.power = 0;

      // 白球（母球）
      this.cueBall = {
        id: 'cue',
        x: 200,
        y: 250,
        vx: 0,
        vy: 0,
        radius: 12,
        color: '#ffffff',
        type: 'cue',
        pocketed: false
      };
      this.balls.push(this.cueBall);

      // 根据房间 items 创建彩球
      let id = 1;
      const colors = ['#ff0000', '#ffff00', '#0066cc', '#800080', '#ff8800', '#008800', '#8b0000', '#000000'];
      let ballIndex = 0;

      this.items.forEach((item, itemIdx) => {
        const qty = item.quantity || 1;
        console.log(`🎱 为物品 "${item.name}" 创建 ${qty} 个球`);

        for (let i = 0; i < qty; i++) {
          // 三角形排列
          const row = Math.floor(ballIndex / 5);
          const col = ballIndex % 5;
          const x = 500 + col * 25 + (row % 2) * 12.5;
          const y = 200 + row * 22;

          const color = colors[itemIdx % colors.length];
          this.balls.push({
            id,
            x: Math.min(x, this.width - 30),
            y: Math.min(y, this.height - 30),
            vx: 0,
            vy: 0,
            radius: 12,
            color,
            type: 'item',
            name: item.name,
            pocketed: false
          });
          id++;
          ballIndex++;
        }
      });

      console.log(`🎱 台球游戏初始化完成，共创建 ${this.balls.length} 个球`);
    },
    resetGame() {
      console.log('🎱 重置台球游戏');
      this.initGame();
    },
    startAiming(e) {
      if (!this.gameActive || this.ballsMoving() || this.cueBall.pocketed) return;

      const rect = this.$refs.canvas.getBoundingClientRect();
      const x = (e.clientX - rect.left) * (this.width / rect.width);
      const y = (e.clientY - rect.top) * (this.height / rect.height);
      const dx = x - this.cueBall.x;
      const dy = y - this.cueBall.y;

      // 点击母球附近才能开始瞄准
      if (Math.hypot(dx, dy) < 50) {
        this.aiming = true;
        this.aimStart = { x: this.cueBall.x, y: this.cueBall.y };
        this.aimEnd = { x, y };
        console.log('开始瞄准', { cueBall: this.cueBall, click: { x, y } });
      }
    },
    updateAim(e) {
      if (!this.aiming) return;

      const rect = this.$refs.canvas.getBoundingClientRect();
      const x = (e.clientX - rect.left) * (this.width / rect.width);
      const y = (e.clientY - rect.top) * (this.height / rect.height);
      this.aimEnd = { x, y };

      const distance = Math.hypot(x - this.cueBall.x, y - this.cueBall.y);
      this.power = Math.min(distance / 3, this.maxPower);
    },
    shoot(e) {
      if (!this.aiming) return;

      const rect = this.$refs.canvas.getBoundingClientRect();
      const x = (e.clientX - rect.left) * (this.width / rect.width);
      const y = (e.clientY - rect.top) * (this.height / rect.height);

      const dx = x - this.cueBall.x;
      const dy = y - this.cueBall.y;
      const dist = Math.hypot(dx, dy);

      console.log('发射', { dx, dy, dist, power: this.power });

      if (dist > 5) {
        const force = Math.min(this.power / 5, 20); // 增加力度
        const angle = Math.atan2(dy, dx);
        this.cueBall.vx = Math.cos(angle) * force;
        this.cueBall.vy = Math.sin(angle) * force;
        console.log('母球速度设置为', { vx: this.cueBall.vx, vy: this.cueBall.vy });
      }

      this.aiming = false;
      this.power = 0;
    },
    ballsMoving() {
      return this.balls.some(b => !b.pocketed && (Math.abs(b.vx) > 0.05 || Math.abs(b.vy) > 0.05));
    },
    gameLoop() {
      this.update();
      this.draw();
      this.rafId = requestAnimationFrame(this.gameLoop);
    },
    update() {
      if (!this.gameActive) return;

      // 更新球的位置
      this.balls.forEach(ball => {
        if (ball.pocketed) return;

        ball.x += ball.vx;
        ball.y += ball.vy;

        // 边界碰撞
        if (ball.x - ball.radius < 0 || ball.x + ball.radius > this.width) {
          ball.vx = -ball.vx * 0.8;
          ball.x = Math.max(ball.radius, Math.min(this.width - ball.radius, ball.x));
        }
        if (ball.y - ball.radius < 0 || ball.y + ball.radius > this.height) {
          ball.vy = -ball.vy * 0.8;
          ball.y = Math.max(ball.radius, Math.min(this.height - ball.radius, ball.y));
        }

        // 摩擦力
        ball.vx *= this.friction;
        ball.vy *= this.friction;

        // 停止微小运动
        if (Math.abs(ball.vx) < 0.05) ball.vx = 0;
        if (Math.abs(ball.vy) < 0.05) ball.vy = 0;
      });

      // 球与球碰撞
      this.checkBallCollisions();

      // 检查进袋
      this.checkPockets();
    },
    checkBallCollisions() {
      for (let i = 0; i < this.balls.length; i++) {
        for (let j = i + 1; j < this.balls.length; j++) {
          const ball1 = this.balls[i];
          const ball2 = this.balls[j];

          if (ball1.pocketed || ball2.pocketed) continue;

          const dx = ball2.x - ball1.x;
          const dy = ball2.y - ball1.y;
          const distance = Math.hypot(dx, dy);
          const minDistance = ball1.radius + ball2.radius;

          if (distance < minDistance) {
            // 简单的弹性碰撞
            const angle = Math.atan2(dy, dx);
            const sin = Math.sin(angle);
            const cos = Math.cos(angle);

            // 分离球体
            const overlap = minDistance - distance;
            ball1.x -= (overlap / 2) * cos;
            ball1.y -= (overlap / 2) * sin;
            ball2.x += (overlap / 2) * cos;
            ball2.y += (overlap / 2) * sin;

            // 交换速度分量
            const v1x = ball1.vx * cos + ball1.vy * sin;
            const v1y = ball1.vy * cos - ball1.vx * sin;
            const v2x = ball2.vx * cos + ball2.vy * sin;
            const v2y = ball2.vy * cos - ball2.vx * sin;

            ball1.vx = v2x * cos - v1y * sin;
            ball1.vy = v1y * cos + v2x * sin;
            ball2.vx = v1x * cos - v2y * sin;
            ball2.vy = v2y * cos + v1x * sin;
          }
        }
      }
    },
    checkPockets() {
      this.balls.forEach(ball => {
        if (ball.pocketed) return;

        this.pockets.forEach(pocket => {
          const distance = Math.hypot(ball.x - pocket.x, ball.y - pocket.y);
          if (distance < pocket.radius - 5) {
            ball.pocketed = true;
            ball.vx = 0;
            ball.vy = 0;

            if (ball.type === 'item') {
              this.score += 10;
              console.log(`🎱 物品球 "${ball.name}" 进袋，触发收集事件`);
              this.$emit('item-collected', ball.name);
            } else if (ball.type === 'cue') {
              // 母球进袋：犯规，重置位置
              setTimeout(() => {
                ball.pocketed = false;
                ball.x = 200;
                ball.y = 250;
              }, 1000);
            }
          }
        });
      });

      // 检查游戏结束
      if (this.remainingBalls === 0 && this.gameActive) {
        this.gameActive = false;
        console.log('🎱 台球游戏完成！');
        // alert('恭喜！游戏完成！');
        // 📌 发射完成事件
        this.$emit('completed');
      }
    },
    draw() {
      const canvas = this.$refs.canvas;
      if (!canvas) return;

      const ctx = canvas.getContext('2d');
      ctx.clearRect(0, 0, this.width, this.height);

      // 绘制台球桌背景
      ctx.fillStyle = '#0d5d0d';
      ctx.fillRect(0, 0, this.width, this.height);

      // 绘制球袋
      this.pockets.forEach(pocket => {
        ctx.beginPath();
        ctx.arc(pocket.x, pocket.y, pocket.radius, 0, Math.PI * 2);
        ctx.fillStyle = '#000000';
        ctx.fill();

        // 球袋边缘
        ctx.beginPath();
        ctx.arc(pocket.x, pocket.y, pocket.radius - 2, 0, Math.PI * 2);
        ctx.strokeStyle = '#333333';
        ctx.lineWidth = 2;
        ctx.stroke();
      });

      // 绘制球
      this.balls.forEach(ball => {
        if (ball.pocketed) return;

        ctx.beginPath();
        ctx.arc(ball.x, ball.y, ball.radius, 0, Math.PI * 2);
        ctx.fillStyle = ball.color;
        ctx.fill();

        // 球的边缘
        ctx.beginPath();
        ctx.arc(ball.x, ball.y, ball.radius, 0, Math.PI * 2);
        ctx.strokeStyle = '#333333';
        ctx.lineWidth = 1;
        ctx.stroke();

        // 白球特殊标记
        if (ball.type === 'cue') {
          ctx.beginPath();
          ctx.arc(ball.x, ball.y, ball.radius / 3, 0, Math.PI * 2);
          ctx.strokeStyle = '#000000';
          ctx.lineWidth = 2;
          ctx.stroke();
        }

        // 球号码或名称
        if (ball.type === 'item' && ball.name) {
          ctx.fillStyle = ball.color === '#ffffff' || ball.color === '#ffff00' ? '#000000' : '#ffffff';
          ctx.font = '8px Arial';
          ctx.textAlign = 'center';
          ctx.fillText(ball.name.substring(0, 2), ball.x, ball.y + 3);
        }
      });

      // 绘制瞄准线
      if (this.aiming && !this.cueBall.pocketed) {
        const dx = this.aimEnd.x - this.cueBall.x;
        const dy = this.aimEnd.y - this.cueBall.y;
        const distance = Math.hypot(dx, dy);

        if (distance > 5) {
          // 瞄准线
          ctx.beginPath();
          ctx.moveTo(this.cueBall.x, this.cueBall.y);
          ctx.lineTo(this.aimEnd.x, this.aimEnd.y);
          ctx.strokeStyle = `rgba(255, 255, 0, ${0.5 + this.power / 200})`;
          ctx.lineWidth = 4;
          ctx.setLineDash([5, 5]);
          ctx.stroke();
          ctx.setLineDash([]);

          // 瞄准点
          ctx.beginPath();
          ctx.arc(this.aimEnd.x, this.aimEnd.y, 8, 0, Math.PI * 2);
          ctx.fillStyle = 'rgba(255, 255, 0, 0.8)';
          ctx.fill();
          ctx.strokeStyle = 'rgba(255, 0, 0, 0.8)';
          ctx.lineWidth = 2;
          ctx.stroke();
        }
      }
    }
  }
};
</script>

<style scoped>
.pool-game-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
  font-family: Arial, sans-serif;
}

.game-header {
  text-align: center;
  background: linear-gradient(135deg, #2d5a2d 0%, #1a3d1a 100%);
  padding: 15px;
  border-radius: 10px;
  width: 100%;
  color: white;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
}

.game-title {
  margin: 0 0 10px 0;
  font-size: 24px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
}

.game-stats {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  background: rgba(255, 255, 255, 0.1);
  padding: 6px 12px;
  border-radius: 20px;
  backdrop-filter: blur(5px);
}

.stat-value {
  font-weight: bold;
  font-size: 16px;
  color: #90EE90;
}

.stat-label {
  font-size: 10px;
  opacity: 0.9;
}

.pool-canvas {
  width: 100%;
  max-width: 600px;
  height: auto;
  border: 3px solid #8b4513;
  border-radius: 8px;
  background: #0d5d0d;
  cursor: crosshair;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.5);
}

.control-panel {
  text-align: center;
  width: 100%;
}

.power-meter {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 8px;
  padding: 8px;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 8px;
}

.power-label {
  font-weight: bold;
  color: #333;
  min-width: 30px;
}

.power-bar {
  width: 150px;
  height: 16px;
  background: #ddd;
  border-radius: 10px;
  overflow: hidden;
  border: 2px solid #333;
}

.power-fill {
  height: 100%;
  background: linear-gradient(90deg, #4CAF50 0%, #FFC107 50%, #FF5722 100%);
  transition: width 0.1s ease;
  border-radius: 8px;
}

.power-value {
  font-weight: bold;
  color: #333;
  min-width: 30px;
}

.control-hint {
  color: #666;
  font-size: 12px;
  margin-bottom: 8px;
}

.control-btn {
  background: linear-gradient(135deg, #8b4513, #654321);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: bold;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.control-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
}

.control-btn:active {
  transform: translateY(0);
}
</style>
