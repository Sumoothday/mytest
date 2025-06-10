<template>
  <div class="game-view">
    <!-- 动态背景效果 -->
    <div class="animated-bg">
      <div class="cyber-grid" />
      <div class="floating-particles" />
    </div>

    <!-- 返回登录按钮 -->
    <div class="top-bar">
      <div class="bar-glow" />
      <button
        class="logout-btn"
        @click="goToLogin"
      >
        <span class="btn-icon">🔙</span>
        <span class="btn-text">返回登录</span>
      </button>
    </div>

    <div class="main-panel">
      <!-- 左侧：游戏场景（对话/信息）-->
      <div class="scene-panel">
        <div class="panel-header">
          <span class="header-icon">🌟</span>
          <span class="header-text">游戏世界</span>
        </div>
        <GameDisplay
          :response="responseData"
          @move-to-inventory="handleMoveToInventory"
          @move-to-room="handleMoveToRoom"
          @drag-command-response="handleBackendUpdate"
        />
      </div>

      <!-- 右侧：地图 + 操作面板 -->
      <div class="right-panel">
        <div class="map-panel">
          <div class="panel-header">
            <span class="header-icon">🗺️</span>
            <span class="header-text">当前地图</span>
            <div class="status-indicator">
              <span class="status-dot" />
              <span class="status-text">在线</span>
            </div>
          </div>
          <div class="map-container">
            <RoomMap
              :current-room="roomName"
              mode="small"
              :scale="1"
            />
          </div>
          <div class="map-controls">
            <button
              class="map-btn"
              @click="toggleMap"
            >
              <span class="btn-icon">🔍</span>
              <span class="btn-text">完整地图</span>
            </button>
            <button
              class="info-btn"
              @click="toggleInfo"
            >
              <span class="btn-icon">📋</span>
              <span class="btn-text">信息面板</span>
            </button>
          </div>
        </div>

        <div class="controls-panel">
          <div class="panel-header">
            <span class="header-icon">⚡</span>
            <span class="header-text">控制终端</span>
            <div class="terminal-indicator">
              <span class="indicator-dot" />
            </div>
          </div>
          <CommandInput @on-command="handleCommandResponse" />
        </div>
      </div>
    </div>

    <!-- 信息侧边栏 -->
    <SidePanel
      :is-open="panelOpen"
      :name="playerName"
      :current-room="roomName"
      :inventory="inventory"
      :total-weight="totalWeight"
      :weight-limit="weightLimit"
      :room-items="roomItems"
      @close="panelOpen = false"
    />

    <!-- 全屏地图侧边栏 -->
    <transition name="slide">
      <div
        v-show="mapPanelOpen"
        class="map-sidepanel"
      >
        <div class="sidepanel-header">
          <h2 class="sidepanel-title">
            <span class="title-icon">🗺️</span>
            <span class="title-text">全屏地图</span>
          </h2>
          <button
            class="close-btn"
            @click="mapPanelOpen = false"
          >
            <span class="close-icon">❌</span>
          </button>
        </div>
        <RoomMap
          :current-room="roomName"
          mode="full"
          :scale="1"
          class="full-map"
        />
      </div>
    </transition>
  </div>
</template>

<script>
import { mapState, mapActions, mapMutations } from 'vuex';
import GameDisplay from './GameDisplay.vue';
import CommandInput from './CommandInput.vue';
import RoomMap from './RoomMap.vue';
import SidePanel from './SidePanel.vue';

export default {
  name: 'GameView',
  components: {
    GameDisplay,
    CommandInput,
    RoomMap,
    SidePanel
  },

  data() {
    return {
      panelOpen: false,
      mapPanelOpen: false
    };
  },

  computed: {
    ...mapState([
      'messages',
      'roomItems',
      'inventory',
      'totalWeight',
      'weightLimit',
      'playerName',
      'roomName'
    ]),

    rawCurrentRoom() {
      const found = this.$store.state.roomMap.rooms.find(r => r.name === this.roomName);
      return found || {};
    },

    responseData() {
      return {
        message: this.messages.length ? this.messages[this.messages.length - 1] : '',
        currentRoom: {
          name: this.roomName || '',
          items: Array.isArray(this.roomItems) ? this.roomItems : [],
          exits: this.rawCurrentRoom.exits || {},
          description: this.rawCurrentRoom.description || ''
        },
        inventory: Array.isArray(this.inventory) ? this.inventory : []
      };
    }
  },

  mounted() {
    this.startGame()
      .then(() => this.sendCommand('look'))
      .catch(err => console.error('启动游戏或 look 过程出错:', err));
  },

  methods: {
    ...mapActions(['startGame', 'enterRoom', 'sendCommand']),
    ...mapMutations([
      'addMessage',
      'changeRoom',
      'updateInventory',
      'updateGameState'
    ]),

    toggleInfo() {
      this.mapPanelOpen = false;
      this.panelOpen = !this.panelOpen;
    },
    toggleMap() {
      this.panelOpen = false;
      this.mapPanelOpen = !this.mapPanelOpen;
    },

    handleCommandResponse(response) {
      if (response.message) this.addMessage(response.message);
      if (response.inventory) this.updateInventory(response.inventory);
      if (response.currentRoom && response.currentRoom.name) this.changeRoom(response.currentRoom.name);
      this.updateGameState(response);
    },

    handleBackendUpdate(response) {
      if (response.message) this.addMessage(response.message);
      if (response.currentRoom && response.currentRoom.name) this.changeRoom(response.currentRoom.name);
      if (response.inventory) this.updateInventory(response.inventory);
      this.updateGameState(response);
    },

    handleMoveToRoom(item) {
      this.updateInventory(this.inventory.filter(i => i.name !== item.name));
      this.sendCommand(`drop ${item.name}`);
    },
    handleMoveToInventory(item) {
      this.updateInventory([...this.inventory, item]);
      this.sendCommand(`take ${item.name}`);
    },

    goToLogin() {
      this.$router.push('/login');
    }
  }
};
</script>
<style scoped>
/* 导入字体 */
@import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700&family=Share+Tech+Mono&display=swap');

.game-view {
  position: relative;
  max-width: 1600px;
  margin: 0 auto;
  padding: 16px;
  min-height: 100vh;
  box-sizing: border-box;
  overflow-x: hidden;
}

/* 动态背景 */
.animated-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: -1;
}

.cyber-grid {
  position: absolute;
  width: 100%;
  height: 100%;
  background-image:
    linear-gradient(rgba(0, 168, 232, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 168, 232, 0.05) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: grid-move 20s linear infinite;
}

@keyframes grid-move {
  0% { transform: translate(0, 0); }
  100% { transform: translate(50px, 50px); }
}

.floating-particles {
  position: absolute;
  width: 100%;
  height: 100%;
  background:
    radial-gradient(circle at 20% 30%, rgba(0, 168, 232, 0.1) 0%, transparent 40%),
    radial-gradient(circle at 80% 70%, rgba(123, 47, 247, 0.1) 0%, transparent 40%);
}

/* 顶部返回登录 */
.top-bar {
  position: relative;
  text-align: right;
  margin-bottom: 20px;
  padding: 12px 0;
  z-index: 10;
}

.bar-glow {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 212, 255, 0.4), transparent);
  transform: translateY(-50%);
}

.logout-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: linear-gradient(135deg, rgba(255, 0, 110, 0.2) 0%, rgba(123, 47, 247, 0.2) 100%);
  border: 2px solid rgba(255, 0, 110, 0.4);
  border-radius: 12px;
  color: #ff006e;
  font-family: 'Orbitron', sans-serif;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  text-transform: uppercase;
  letter-spacing: 1px;
  transition: all 0.3s ease;
  z-index: 1;
}

.logout-btn:hover {
  transform: translateY(-2px);
  border-color: #ff006e;
  box-shadow:
    0 8px 24px rgba(255, 0, 110, 0.4),
    inset 0 0 15px rgba(255, 0, 110, 0.2);
}

.btn-icon {
  font-size: 14px;
  filter: drop-shadow(0 0 6px currentColor);
}

/* 主面板布局优化 */
.main-panel {
  display: grid;
  grid-template-columns: 1fr 400px; /* 固定右侧宽度 */
  gap: 20px;
  align-items: start; /* 顶部对齐 */
  min-height: calc(100vh - 120px);
}

/* 面板基础样式优化 */
.scene-panel,
.map-panel,
.controls-panel {
  background: linear-gradient(135deg, rgba(0, 10, 20, 0.8) 0%, rgba(0, 20, 40, 0.8) 100%);
  border: 2px solid rgba(0, 168, 232, 0.3);
  border-radius: 16px;
  padding: 20px;
  backdrop-filter: blur(20px);
  box-shadow:
    0 0 40px rgba(0, 168, 232, 0.2),
    inset 0 0 20px rgba(0, 168, 232, 0.1);
  transition: all 0.3s ease;
  box-sizing: border-box;
  overflow: hidden;
}

.scene-panel {
  min-height: calc(100vh - 140px);
  height: fit-content;
}

.scene-panel:hover,
.map-panel:hover,
.controls-panel:hover {
  border-color: rgba(0, 168, 232, 0.5);
  box-shadow:
    0 0 60px rgba(0, 168, 232, 0.3),
    inset 0 0 30px rgba(0, 168, 232, 0.15);
}

/* 面板头部优化 */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 168, 232, 0.2);
  flex-wrap: wrap;
  gap: 8px;
}

.panel-header .header-icon {
  font-size: 18px;
  filter: drop-shadow(0 0 8px rgba(0, 212, 255, 0.8));
  flex-shrink: 0;
}

.panel-header .header-text {
  font-family: 'Orbitron', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: #00d4ff;
  text-transform: uppercase;
  letter-spacing: 1px;
  text-shadow: 0 0 15px rgba(0, 212, 255, 0.8);
  margin-left: 8px;
  flex: 1;
  min-width: 0; /* 允许文字换行 */
}

/* 状态指示器优化 */
.status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #00ff88;
  border-radius: 50%;
  box-shadow: 0 0 8px rgba(0, 255, 136, 0.8);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.status-text {
  font-size: 11px;
  color: #98c1d9;
  font-family: 'Share Tech Mono', monospace;
  white-space: nowrap;
}

.terminal-indicator {
  width: 12px;
  height: 12px;
  flex-shrink: 0;
}

.indicator-dot {
  display: block;
  width: 100%;
  height: 100%;
  background: #00d4ff;
  border-radius: 50%;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0.3; }
}

/* 地图容器优化 */
.map-container {
  width: 100%;
  height: 250px; /* 固定高度 */
  padding: 12px;
  border: 1px solid rgba(0, 168, 232, 0.2);
  border-radius: 12px;
  margin-bottom: 16px;
  background: rgba(0, 0, 0, 0.3);
  box-sizing: border-box;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 地图控制按钮优化 */
.map-controls {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.map-btn,
.info-btn {
  flex: 1;
  min-width: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 12px;
  border: 2px solid rgba(0, 168, 232, 0.4);
  border-radius: 10px;
  font-family: 'Share Tech Mono', monospace;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  box-sizing: border-box;
}





.map-btn {
  background: linear-gradient(135deg, rgba(0, 168, 232, 0.2) 0%, rgba(123, 47, 247, 0.2) 100%);
  color: #00d4ff;
}

.info-btn {
  background: linear-gradient(135deg, rgba(123, 47, 247, 0.2) 0%, rgba(255, 0, 110, 0.2) 100%);
  color: #7b2ff7;
  border-color: rgba(123, 47, 247, 0.4);
}

.map-btn::after,
.info-btn::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.map-btn:hover::after,
.info-btn:hover::after {
  left: 100%;
}

.map-btn:hover,
.info-btn:hover {
  transform: translateY(-1px);
  box-shadow:
    0 6px 20px rgba(0, 212, 255, 0.4),
    inset 0 0 15px rgba(0, 212, 255, 0.2);
}

.info-btn:hover {
  box-shadow:
    0 6px 20px rgba(123, 47, 247, 0.4),
    inset 0 0 15px rgba(123, 47, 247, 0.2);
}

/* 右侧面板优化 */
.right-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  max-width: 400px;
}

.map-panel {
  flex-shrink: 0;
}

.controls-panel {
  flex: 1;
  min-height: 200px;
}

/* 侧边栏样式优化 */
.map-sidepanel {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: linear-gradient(135deg, rgba(0, 10, 20, 0.95) 0%, rgba(0, 20, 40, 0.95) 100%);
  backdrop-filter: blur(20px);
  z-index: 1000;
  padding: 24px;
  border: 2px solid rgba(0, 168, 232, 0.3);
  box-shadow:
    0 0 60px rgba(0, 168, 232, 0.3),
    inset 0 0 40px rgba(0, 168, 232, 0.1);
  box-sizing: border-box;
  overflow: auto;
}

.sidepanel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid rgba(0, 168, 232, 0.3);
  flex-wrap: wrap;
  gap: 16px;
}

.sidepanel-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  font-family: 'Orbitron', sans-serif;
  font-size: 20px;
  font-weight: 700;
  color: #00d4ff;
  text-transform: uppercase;
  letter-spacing: 2px;
  text-shadow: 0 0 20px rgba(0, 212, 255, 0.8);
}

.title-icon {
  font-size: 24px;
  filter: drop-shadow(0 0 12px rgba(0, 212, 255, 0.8));
}

.close-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: linear-gradient(135deg, rgba(255, 0, 110, 0.2) 0%, rgba(123, 47, 247, 0.2) 100%);
  border: 2px solid rgba(255, 0, 110, 0.4);
  border-radius: 12px;
  color: #ff006e;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.close-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 0 20px rgba(255, 0, 110, 0.6);
}

.close-icon {
  filter: drop-shadow(0 0 6px currentColor);
}

.full-map {
  width: 100%;
  height: calc(100vh - 160px);
  border: 2px solid rgba(0, 168, 232, 0.3);
  border-radius: 16px;
  overflow: hidden;
  box-sizing: border-box;
}

/* 过渡动画 */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.5s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.slide-enter-from,
.slide-leave-to {
  transform: scale(0.8);
  opacity: 0;
}

/* 响应式设计优化 */
@media (max-width: 1400px) {
  .main-panel {
    grid-template-columns: 1fr 350px;
    gap: 16px;
  }
}

@media (max-width: 1200px) {
  .main-panel {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .right-panel {
    order: -1;
    max-width: none;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }

  .map-container {
    height: 200px;
  }
}

@media (max-width: 768px) {
  .game-view {
    padding: 12px;
  }

  .main-panel {
    gap: 16px;
  }

  .right-panel {
    grid-template-columns: 1fr;
  }

  .scene-panel,
  .map-panel,
  .controls-panel {
    padding: 16px;
  }

  .map-controls {
    flex-direction: column;
    gap: 8px;
  }

  .map-btn,
  .info-btn {
    min-width: auto;
    flex: none;
  }

  .map-container {
    height: 180px;
  }

  .map-sidepanel {
    padding: 16px;
  }

  .sidepanel-title {
    font-size: 16px;
  }

  .full-map {
    height: calc(100vh - 140px);
  }

  .panel-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .status-indicator,
  .terminal-indicator {
    align-self: flex-end;
  }
}

@media (max-width: 480px) {
  .game-view {
    padding: 8px;
  }

  .main-panel {
    gap: 12px;
  }

  .scene-panel,
  .map-panel,
  .controls-panel {
    padding: 12px;
  }

  .panel-header .header-text {
    font-size: 12px;
  }

  .map-container {
    height: 150px;
  }

  .logout-btn {
    padding: 8px 12px;
    font-size: 11px;
  }
}
/* 在 GameView.vue 的 <style> 最后补一行 */
.scene-panel {
  overflow: visible !important;
}

</style>
