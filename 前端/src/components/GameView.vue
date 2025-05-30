<template>
  <div class="game-view">
    <div class="main-panel">
      <!-- 左侧：游戏场景 + 指令输入 -->
      <div class="scene-panel">
        <GameDisplay :messages="messages" />
        <CommandInput @on-command="sendCommand" />
      </div>

      <!-- 右侧：地图 + 操作面板 -->
      <div class="right-panel">
        <div class="map-panel">
          <h3 class="panel-title">🗺 当前地图</h3>
          <div class="map-container">
            <RoomMap :currentRoom="roomName" mode="small" :scale="1" />
          </div>
          <div class="map-controls">
            <button @click="toggleMap" class="map-btn">🔍 完整地图</button>
            <button @click="toggleInfo" class="info-btn">📋 信息面板</button>
          </div>
        </div>
        <div class="controls-panel">
          <slot name="controls"></slot>
        </div>
      </div>
    </div>

    <!-- 信息侧边栏 -->
    <transition name="slide">
      <div v-show="panelOpen" class="info-sidepanel">
        <SidePanel
          @close="panelOpen = false"
          :name="playerName"
          :currentRoom="roomName"
          :inventory="inventory"
          :totalWeight="totalWeight"
          :weightLimit="weightLimit"
          :roomItems="roomItems"
        />
      </div>
    </transition>

    <!-- 全屏地图侧边栏 -->
    <transition name="slide">
      <div v-show="mapPanelOpen" class="map-sidepanel">
        <div class="sidepanel-header">
          <h2>🗺 全屏地图</h2>
          <button class="close-btn" @click="mapPanelOpen = false">❌</button>
        </div>
        <RoomMap :currentRoom="roomName" mode="full" :scale="1" class="full-map" />
      </div>
    </transition>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import GameDisplay from './GameDisplay.vue'
import CommandInput from './CommandInput.vue'
import RoomMap from './RoomMap.vue'
import SidePanel from './SidePanel.vue'

export default {
  name: 'GameView',
  components: { GameDisplay, CommandInput, RoomMap, SidePanel },
  data() {
    return {
      panelOpen: false,
      mapPanelOpen: false
    }
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
    ])
  },
  methods: {
    ...mapActions(['sendCommand', 'enterRoom']),
    toggleInfo() {
      this.mapPanelOpen = false
      this.panelOpen = !this.panelOpen
    },
    toggleMap() {
      this.panelOpen = false
      this.mapPanelOpen = !this.mapPanelOpen
    }
  },
  watch: {
    // 只要 roomName 变了，就把它记到 visitedRooms
    roomName(newRoom) {
      this.enterRoom(newRoom)
    }
  }
}
</script>



<style scoped>
.game-view {
  max-width: 1100px;
  margin: 0 auto 20px;
  padding: 12px;
  transform: scale(0.9);
  transform-origin: top center;
}
.main-panel {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 12px;
}
.scene-panel,
.map-panel,
.controls-panel {
  background: rgba(30, 30, 30, 0.6);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.5);
}
.panel-title {
  color: #ffc107;
  font-size: 14px;
  margin-bottom: 6px;
  text-shadow: 0 0 4px rgba(0, 0, 0, 0.7);
}
.map-container {
  width: 100%;
  min-height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.map-controls {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
}
.map-btn,
.info-btn {
  background: #ffc107;
  border: none;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 11px;
  cursor: pointer;
  color: #222;
  transition: transform 0.2s, background 0.2s;
}
.map-btn:hover,
.info-btn:hover {
  transform: translateY(-1px);
  background: #ffdb4d;
}

/* 全屏地图侧边面板 */
.map-sidepanel {
  position: fixed;
  top: 0;
  right: 0;
  width: 100%; /* 修改宽度为100% */
  height: 100vh;
  background: rgba(20, 20, 20, 0.9);
  backdrop-filter: blur(6px);
  box-shadow: -4px 0 16px rgba(0, 0, 0, 0.7);
  padding: 8px;
  z-index: 2000;
  display: flex;
  flex-direction: column;
}
.sidepanel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.sidepanel-header h2 {
  color: #ffc107;
  font-size: 16px;
}
.close-btn {
  background: #ffc107;
  border: none;
  padding: 2px 6px;
  border-radius: 8px;
  cursor: pointer;
}
.full-map {
  flex: 1;
  width: 100%;
}

/* 侧边滑动动画 */
.slide-enter-active,
.slide-leave-active {
  transition: transform 0.001s ease;
}
.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
}
</style>
