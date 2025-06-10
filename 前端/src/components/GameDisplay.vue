<template>
  <div class="game-display-wrapper">
    <!-- 游戏选择器 -->
    <div class="game-selector">
      <h3 class="selector-title">选择小游戏</h3>
      <div class="game-buttons">
        <button
          v-for="game in availableGames"
          :key="game.id"
          :class="['game-btn', { active: currentGame === game.id }]"
          @click="selectGame(game.id)"
        >
          <span class="game-icon">{{ game.icon }}</span>
          <span class="game-name">{{ game.name }}</span>
        </button>
      </div>
    </div>

    <!-- 游戏区域 - 动态组件 -->
    <div class="game-container">
      <component
        :is="currentGameComponent"
        :targets="gameTargets"
        :items="gameItems"
        :show-debug="false"
        @completed="onPuzzleCompleted"
        @score-update="onScoreUpdate"
        @item-collected="onItemCollected"
      />
    </div>

    <!-- 主信息面板 -->
    <div class="info-panel">
      <!-- 房间信息 -->
      <div class="room-info">
        <p class="room-title">房间信息</p>
        <div class="room-details">
          <p>
            <span class="label">📍 当前位置：</span>
            <span class="value">[{{ localResponse.currentRoom.name }}]</span>
          </p>
          <p>
            <span class="label">🚪 出口方向：</span>
            <span class="value">{{ exitList }}</span>
          </p>
          <p v-if="gameScore">
            <span class="label">🏆 游戏得分：</span>
            <span class="value">{{ gameScore }}</span>
          </p>
          <p>
            <span class="label">📦 物品总数：</span>
            <span class="value">{{ totalItemCount }}</span>
          </p>
        </div>

        <!-- 房间物品图标区 -->
        <div
          class="room-items"
          @dragover.prevent="onDragOverRoom"
          @drop="onDropToRoom"
        >
          <template v-if="localResponse.currentRoom.items.length">
            <div
              v-for="item in localResponse.currentRoom.items"
              :key="item.name"
              class="room-item"
              draggable="true"
              @dragstart="onDragStartRoomItem(item)"
            >
              <img
                :src="getItemIcon(item.name)"
                :alt="item.name"
                class="item-icon"
              >
              <div class="item-quantity">{{ item.quantity }}</div>
              <span class="item-tooltip">{{ item.name }} ×{{ item.quantity }}</span>
            </div>
          </template>
          <div v-else class="no-room-items">— 房间内无物品 —</div>
        </div>
      </div>

      <hr class="divider">

      <!-- 背包区域 -->
      <div class="middle-section">
        <div
          class="inventory-panel"
          @dragover.prevent="onDragOverInventory"
          @drop="onDropToInventory"
        >
          <p class="section-title">🎒 背包</p>
          <div class="inventory-items">
            <template v-if="localResponse.inventory.length">
              <div
                v-for="item in localResponse.inventory"
                :key="item.name"
                class="inventory-item"
                draggable="true"
                @dragstart="onDragStartInventoryItem(item)"
              >
                <img
                  :src="getItemIcon(item.name)"
                  :alt="item.name"
                  class="item-icon"
                >
                <div class="item-quantity">{{ item.quantity }}</div>
                <span class="item-tooltip">{{ item.name }} ×{{ item.quantity }}</span>
              </div>
            </template>
            <div v-else class="empty-inventory">— 空 —</div>
          </div>
        </div>
      </div>

      <hr class="divider">

      <!-- 消息面板 -->
      <div v-if="localResponse.message" class="message-panel">
        <p class="section-title">💬 消息</p>
        <p class="message-text">{{ localResponse.message }}</p>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import SokobanGame from '@/components/SokobanGame.vue';
import MemoryGame from '@/components/MemoryGame.vue';
import SliderPuzzle from '@/components/SliderPuzzle.vue';
import SnakeGame from '@/components/SnakeGame.vue';

export default {
  name: 'GameDisplay',
  components: {
    SokobanGame,
    MemoryGame,
    SliderPuzzle,
    SnakeGame
  },
  props: {
    response: {
      type: Object,
      default: () => ({
        message: '',
        currentRoom: { name: '', items: [], exits: {}, description: '' },
        inventory: []
      })
    }
  },
  data() {
    return {
      // 可用游戏列表
      availableGames: [
        { id: 'sokoban', name: '推箱子', icon: '📦', component: 'SokobanGame' },
         { id: 'memory', name: '黄金矿工', icon: '🎴', component: 'MemoryGame' },
         { id: 'slider', name: '弹珠台', icon: '🧩', component: 'SliderPuzzle' },
        { id: 'snake', name: '贪吃蛇', icon: '🐍', component: 'SnakeGame' }
      ],
      currentGame: 'sokoban',
      gameScore: 0,

      specialItems: ['魔法钥匙', '古籍', '神秘图纸'],
      collectibleItems: ['金币', '宝石', '羽毛'],
      draggingItemName: null,
      draggingFrom: null,

      localResponse: {
        message: '',
        currentRoom: { name: '', items: [], exits: {}, description: '' },
        inventory: []
      },

      mapData: [
        [0, 0, 0, 0, 0, 0],
        [0, 1, 1, 0, 1, 0],
        [0, 0, 0, 0, 0, 0],
        [1, 0, 1, 1, 0, 0],
        [0, 0, 0, 0, 0, 1]
      ],
      mapItems: [],
      mapInitialized: false,
      characterIcon: '/images/character_blue.png'
    };
  },
  computed: {
    currentGameComponent() {
      const game = this.availableGames.find(g => g.id === this.currentGame);
      return game ? game.component : 'SokobanGame';
    },
    exitList() {
      if (!this.localResponse || !this.localResponse.currentRoom) return '—';
      const keys = Object.keys(this.localResponse.currentRoom.exits || {});
      return keys.length ? keys.join('，') : '—';
    },
    // 计算房间内物品的总数量
    totalItemCount() {
      return this.localResponse.currentRoom.items.reduce((sum, item) => sum + item.quantity, 0);
    },
    // 根据物品数量生成游戏目标位置
    gameTargets() {
      const targets = [];
      this.mapItems.forEach(mapItem => {
        // 找到对应的房间物品获取其数量
        const roomItem = this.localResponse.currentRoom.items.find(item => item.name === mapItem.name);
        const quantity = roomItem ? roomItem.quantity : 1;

        // 为每个物品数量生成一个目标位置
        for (let i = 0; i < quantity; i++) {
          targets.push([mapItem.position.col, mapItem.position.row]);
        }
      });
      return targets;
    },
    gameItems() {
      return this.mapItems.map(mapItem => {
        // 找到对应的房间物品获取其数量
        const roomItem = this.localResponse.currentRoom.items.find(item => item.name === mapItem.name);
        return {
          name: mapItem.name,
          quantity: roomItem ? roomItem.quantity : 1,
          position: {
            col: mapItem.position.col,
            row: mapItem.position.row
          }
        };
      });
    },
    roomItemsRaw() {
      return (this.localResponse.currentRoom.items || []).map(item => item.name);
    }
  },
  watch: {
    response: {
      handler(newVal) {
        this.localResponse = {
          message: newVal.message,
          currentRoom: {
            name: newVal.currentRoom.name,
            description: newVal.currentRoom.description,
            exits: { ...newVal.currentRoom.exits },
            items: newVal.currentRoom.items.map(i => ({ ...i }))
          },
          inventory: newVal.inventory.map(i => ({ ...i }))
        };
      },
      immediate: true,
      deep: true
    },
    roomItemsRaw: {
      handler(newList) {
        this.mapItems = [];
        newList.forEach(name => {
          let placed = false;
          let row, col;
          while (!placed) {
            row = Math.floor(Math.random() * this.mapData.length);
            col = Math.floor(Math.random() * this.mapData[0].length);
            if (
              this.mapData[row][col] === 0 &&
              !this.mapItems.some(it => it.position.row === row && it.position.col === col)
            ) {
              placed = true;
            }
          }
          this.mapItems.push({ name, position: { row, col } });
        });
      },
      immediate: true
    }
  },
  mounted() {
    const sessionId = this.$store.state.sessionId || 'demo-session';
    axios.post(
      'http://localhost:8080/api/command',
      null,
      {
        headers: { 'X-Session-Id': sessionId },
        params: { command: 'look', parameter: '' }
      }
    )
      .then(res => {
        this.$store.commit('updateGameState', res.data);
      })
      .catch(err => {
        console.error('❌ GameDisplay mounted 时 look 失败：', err);
        this.$store.commit('addMessage', '⚠️ 初始化获取房间信息失败，请稍后重试');
      });
  },
  methods: {
    selectGame(gameId) {
      this.currentGame = gameId;
      this.gameScore = 0;
    },

    onScoreUpdate(score) {
      this.gameScore = score;
    },
    async onItemCollected(itemName) {
      const sessionId = this.$store.state.sessionId || 'demo-session';
      try {
        const res = await axios.post(
          'http://localhost:8080/api/command',
          null,
          { headers: { 'X-Session-Id': sessionId }, params: { command: 'take', parameter: itemName } }
        );
        this.$store.commit('updateGameState', res.data);
      } catch (err) {
        console.error(`自动拾取 ${itemName} 失败:`, err);
        this.$store.commit('addMessage', `⚠️ 自动拾取 ${itemName} 失败`);
      }
    },
    async onPuzzleCompleted() {
      console.log('🎮 游戏完成事件触发！当前游戏:', this.currentGame);

      // 游戏完成时的奖励逻辑
      this.$store.commit('addMessage', `🎉 恭喜完成${this.currentGame}游戏！`);

      // 获取当前房间物品
      const itemNames = this.localResponse.currentRoom.items.map(i => i.name);
      console.log('🎒 准备自动拾取物品:', itemNames);

      if (!itemNames.length) {
        console.log('⚠️ 房间内没有物品可拾取');
        return;
      }

      const sessionId = this.$store.state.sessionId || 'demo-session';

      // 自动拾取所有房间物品
      for (const name of itemNames) {
        try {
          console.log(`📦 正在拾取: ${name}`);

          const res = await axios.post(
            'http://localhost:8080/api/command',
            null,
            {
              headers: { 'X-Session-Id': sessionId },
              params: { command: 'take', parameter: name }
            }
          );

          console.log(`✅ 成功拾取 ${name}:`, res.data);
          this.$store.commit('updateGameState', res.data);

        } catch (err) {
          console.error('❌ 自动拾取失败：', name, err);
          this.$store.commit('addMessage', `⚠️ 自动拾取 ${name} 失败`);
        }
      }

      console.log('🎉 自动拾取流程完成');
    },

    getItemIcon(itemName) {
      if (this.specialItems.includes(itemName)) {
        return require(`@/assets/${itemName}_blue.png`);
      } else if (this.collectibleItems.includes(itemName)) {
        return require(`@/assets/${itemName}_bluecollectible.png`);
      }
      return require('@/assets/chest.png');
    },

    // 拖拽方法保持不变
    onDragStartRoomItem(item) {
      this.draggingItemName = item.name;
      this.draggingFrom = 'room';
      event.dataTransfer.setData('text/plain', item.name);
    },
    onDragStartInventoryItem(item) {
      this.draggingItemName = item.name;
      this.draggingFrom = 'inventory';
      event.dataTransfer.setData('text/plain', item.name);
    },
    onDragOverRoom(e) {
      e.preventDefault();
    },
    async onDropToRoom(e) {
      e.preventDefault();
      const name = e.dataTransfer.getData('text/plain');
      if (this.draggingFrom === 'inventory') {
        try {
          const sessionId = this.$store.state.sessionId || 'demo-session';
          const res = await axios.post(
            'http://localhost:8080/api/command',
            null,
            {
              headers: { 'X-Session-Id': sessionId },
              params: { command: 'drop', parameter: name }
            }
          );
          this.$store.commit('updateGameState', res.data);
          this.$emit('drag-command-response', res.data);
        } catch (error) {
          console.error('❌ 拖拽到房间失败:', name, error);
          this.$store.commit('addMessage', '⚠️ 拖拽到房间操作失败');
        }
      }
      this.draggingItemName = null;
      this.draggingFrom = null;
    },
    onDragOverInventory(e) {
      e.preventDefault();
    },
    async onDropToInventory(e) {
      e.preventDefault();
      const name = e.dataTransfer.getData('text/plain');
      if (this.draggingFrom === 'room') {
        try {
          const sessionId = this.$store.state.sessionId || 'demo-session';
          const res = await axios.post(
            'http://localhost:8080/api/command',
            null,
            {
              headers: { 'X-Session-Id': sessionId },
              params: { command: 'take', parameter: name }
            }
          );
          this.$store.commit('updateGameState', res.data);
          this.$emit('drag-command-response', res.data);
        } catch (err) {
          console.error('❌ 拾取物品失败：', err);
          this.$store.commit('addMessage', `⚠️ 无法拾取 ${name}`);
        }
      }
      this.draggingItemName = null;
      this.draggingFrom = null;
    }
  }
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&family=Share+Tech+Mono&display=swap');

/* 整体容器 */
.game-display-wrapper {
  width: 100%;
  min-height: 100%;
  padding: 0;
  margin: 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 24px;
  background: none;
}

/* —— 统一风格：选择器、游戏区和信息面板 —— */
.game-selector,
.game-container,
.info-panel {
  width: 100%;
  padding: 16px;
  box-sizing: border-box;
  background: linear-gradient(135deg, rgba(0,10,20,0.8), rgba(0,20,40,0.8));
  border: 2px solid rgba(0,168,232,0.3);
  border-radius: 12px;
  backdrop-filter: blur(20px);
  box-shadow:
    0 0 30px rgba(0,168,232,0.2),
    inset 0 0 15px rgba(0,168,232,0.1);
}

/* —— 游戏选择器 —— */
.selector-title {
  color: #00d4ff;
  font-family: 'Orbitron', monospace;
  font-size: 20px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 16px;
  text-shadow: 0 0 20px rgba(0,212,255,0.8);
}

.game-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.game-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, rgba(0,30,60,0.8), rgba(0,50,100,0.8));
  border: 2px solid rgba(0,168,232,0.3);
  border-radius: 12px;
  color: #a0c4ff;
  font-family: 'Share Tech Mono', monospace;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.game-btn:hover {
  background: linear-gradient(135deg, rgba(0,50,100,0.9), rgba(0,80,160,0.9));
  border-color: #00d4ff;
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0,212,255,0.4);
}

.game-btn.active {
  background: linear-gradient(135deg, #0080ff, #00d4ff);
  color: #000;
  font-weight: 700;
  border-color: #00ffff;
  box-shadow: 0 0 30px rgba(0,212,255,0.8);
}

.game-icon {
  font-size: 20px;
}

/* —— 游戏显示区域 —— */
.game-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 0;   /* 取消原本的 400px 最小高度，让它随内容自适应 */
}

/* —— 主信息面板 —— */
.info-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
  color: #d3d3d3;
  font-family: 'Segoe UI', sans-serif;
}

/* —— 房间信息 & 背包内部 —— */
.room-info .room-title,
.section-title {
  color: #00d4ff;
  font-family: 'Orbitron', monospace;
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 12px;
  text-shadow: 0 0 15px rgba(0,212,255,0.6);
}

.room-details {
  background: rgba(0,0,0,0.3);
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.room-details p {
  margin: 6px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.label {
  color: #7aa7ff;
  font-weight: 600;
}

.value {
  color: #00d4ff;
  font-family: 'Share Tech Mono', monospace;
}

.divider {
  border: none;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0,168,232,0.5), transparent);
  margin: 16px 0;
}

/* —— 物品和背包区 —— */
.room-items,
.inventory-panel {
  background: rgba(0,0,0,0.3);
  border: 2px dashed rgba(0,168,232,0.3);
  border-radius: 16px;
  padding: 16px;
  min-height: 120px;
  position: relative;
  width: 100%;
  box-sizing: border-box;
}

.inventory-items,
.room-items {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.room-item,
.inventory-item {
  width: 50px;
  height: 50px;
  position: relative;
  cursor: grab;
  transition: all 0.3s ease;
}

.item-icon {
  width: 32px;
  height: 32px;
  object-fit: contain;
  background: linear-gradient(135deg, rgba(0,168,232,0.2), rgba(123,47,247,0.2));
  border: 2px solid rgba(0,168,232,0.4);
  border-radius: 12px;
  padding: 4px;
  filter: drop-shadow(0 0 8px rgba(0,212,255,0.6));
  transition: all 0.3s ease;
}

.item-quantity {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 20px;
  height: 20px;
  background: linear-gradient(135deg, #ff006e, #ff4d4d);
  border: 2px solid rgba(0,0,0,0.8);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  box-shadow: 0 0 8px rgba(255,0,110,0.8);
  z-index: 10;
}

.item-tooltip {
  visibility: hidden;
  position: absolute;
  top: -28px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, rgba(0,168,232,0.9), rgba(123,47,247,0.9));
  color: #fff;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 6px;
  white-space: nowrap;
  font-size: 10px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.5);
  border: 1px solid rgba(0,212,255,0.5);
  z-index: 100;
}

.room-item:hover .item-tooltip,
.inventory-item:hover .item-tooltip {
  visibility: visible;
}

.no-room-items,
.empty-inventory {
  color: #4a5875;
  text-align: center;
  font-style: italic;
  padding: 20px;
  border: 1px dashed rgba(0,168,232,0.2);
  border-radius: 8px;
  background: rgba(0,0,0,0.2);
  width: 100%;
}

/* —— 消息区 —— */
.message-panel {
  width: 100%;
  padding: 16px;
  box-sizing: border-box;
  background: rgba(0,0,0,0.4);
  border: 2px solid rgba(0,168,232,0.4);
  border-radius: 12px;
}

.message-text {
  color: #a0c4ff;
  font-family: 'Share Tech Mono', monospace;
  line-height: 1.6;
}
</style>
