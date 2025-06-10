<template>
  <div class="app-wrapper">
    <!-- 旧背景层（渐隐） -->
    <div
      class="bg-layer old-bg"
      :style="{
        backgroundImage: `url('${oldBgImageUrl}')`,
        opacity: isTransitioning ? 0 : 1
      }"
    />

    <!-- 新背景层（渐显） -->
    <div
      class="bg-layer new-bg"
      :style="{
        backgroundImage: `url('${bgImageUrl}')`,
        opacity: isTransitioning ? 1 : 0
      }"
    />

    <!-- 顶部标题 -->
    <div class="game-header">
      <h1 class="animated-title">
        🌍 World of Zuul
      </h1>
    </div>

    <!-- 雪花天气效果（可选） -->
    <div class="weather-effect">
      <div
        v-for="n in 20"
        :key="n"
        class="snowflake"
        :style="getSnowflakeStyle()"
      >
        ❄
      </div>
    </div>

    <!-- 中央游戏区域 -->
    <div class="page-layout">
      <div class="center-game">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';

export default {
  name: 'App',
  data() {
    return {
      oldBgImageUrl: '',
      isTransitioning: false,
      prevRoomName: '',
      teleportAudio: null
    };
  },
  computed: {
    ...mapState(['roomName']),
    bgImageUrl() {
      const allowedRooms = [
        'outside',
        'theater',
        'campus pub',
        'computing lab',
        'computing department office',
        'teleportation chamber'
      ];
      return allowedRooms.includes(this.roomName)
        ? `/static/rooms/${this.roomName}.png`
        : '/static/rooms/default.png';
    }
  },
  watch: {
    roomName(newRoomName) {
      if (newRoomName !== this.prevRoomName) {
        setTimeout(() => {
          this.teleportAudio.currentTime = 0;
          this.teleportAudio.play().catch(err => {
            console.warn('播放传送音效失败：', err);
          });
        }, 200);

        this.prevRoomName = newRoomName;
        this.startTransition();
      }
    }
  },
  created() {
    this.$store.dispatch('startGame');
    this.preloadAllBackgrounds();
    this.teleportAudio = new Audio('/static/sounds/传送.mp3');
    this.teleportAudio.load();
  },
  methods: {
    preloadAllBackgrounds() {
      const allowedRooms = [
        'outside',
        'theater',
        'campus pub',
        'computing lab',
        'computing department office',
        'teleportation chamber'
      ];
      allowedRooms.forEach(room => {
        new Image().src = `/static/rooms/${room}.png`;
      });
    },
    startTransition() {
      this.oldBgImageUrl = this.bgImageUrl;
      this.isTransitioning = true;
      setTimeout(() => {
        this.isTransitioning = false;
      }, 1500); // 延长到1.5秒，匹配更慢的过渡效果
    },
    getSnowflakeStyle() {
      const left = Math.random() * 100;
      const duration = (6 + Math.random() * 4).toFixed(2) + 's';
      return {
        left: `${left}%`,
        animationDuration: duration
      };
    }
  }
};
</script>

<style>
@font-face {
  font-family: 'Press Start 2P';
  src: url('/static/fonts/PressStart2P-Regular.ttf') format('truetype');
  font-weight: normal;
  font-style: normal;
}

body {
  margin: 0;
  font-family: 'Press Start 2P', monospace;
  font-size: 10px;
  letter-spacing: 0.5px;
  color: #f0f0f0;
  background-color: #0a1128;
  overflow-y: auto;
  overflow-x: hidden;
}

#app {
  position: relative;
  min-height: 100vh;
  background-color: rgba(10, 17, 40, 0.8);
  box-sizing: border-box;
}

.app-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  z-index: 0;
}

/* ✅ 更平缓的淡入淡出背景层 */
.bg-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-size: cover;
  background-repeat: no-repeat;
  background-position: center;
  /* 使用更慢、更平滑的过渡效果 */
  transition: opacity 1.5s cubic-bezier(0.4, 0.0, 0.2, 1);
  z-index: -1;
}

/* 可选：添加轻微的缩放效果让过渡更自然 */
.bg-layer.old-bg {
  transform: scale(1);
  transition: opacity 1.5s cubic-bezier(0.4, 0.0, 0.2, 1),
  transform 1.5s cubic-bezier(0.4, 0.0, 0.2, 1);
}

.bg-layer.new-bg {
  transform: scale(1.02);
  transition: opacity 1.5s cubic-bezier(0.4, 0.0, 0.2, 1),
  transform 1.5s cubic-bezier(0.4, 0.0, 0.2, 1);
}

/* 当过渡开始时，调整缩放 */
.bg-layer.old-bg {
  opacity: 1;
}

.bg-layer.new-bg {
  opacity: 0;
  transform: scale(1);
}

.game-header {
  width: 100%;
  text-align: center;
  margin-top: 16px;
  animation: fadeIn 2s ease-in-out;
  z-index: 1;
}
.animated-title {
  font-size: 28px;
  color: #00a8e8;
  text-shadow: 2px 2px #00171f;
  animation: glow 2s infinite alternate ease-in-out;
}

@keyframes glow {
  from {
    text-shadow: 0 0 5px #00a8e8, 0 0 10px #007ea7;
  }
  to {
    text-shadow: 0 0 20px #98c1d9, 0 0 30px #003459;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 雪花效果（可按需保留） */
.weather-effect {
  position: fixed;
  top: 0;
  left: 0;
  pointer-events: none;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;
}
.snowflake {
  position: absolute;
  top: -2em;
  color: #98c1d9;
  font-size: 1.2em;
  animation: fall linear infinite;
}
@keyframes fall {
  0% {
    transform: translateY(0) rotate(0deg);
    opacity: 1;
  }
  100% {
    transform: translateY(100vh) rotate(720deg);
    opacity: 0.5;
  }
}

.page-layout {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  overflow-y: auto;
  height: calc(100vh - 90px);
  padding: 0 20px;
  color: white;
  z-index: 1;
}

.center-game {
  flex: none;
  width: 100%;
  max-width: 900px;
  margin: auto;
  padding: 24px;
  background: rgba(10, 25, 55, 0.8);
  border-radius: 16px;
  box-shadow: 0 0 20px rgba(0, 126, 167, 0.4);
  z-index: 2;
}
</style>
