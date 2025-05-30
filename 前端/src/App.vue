<template>
  <div class="app-wrapper">
    <!-- 顶部标题 -->
    <div class="game-header">
      <h1 class="animated-title">🌍 World of Zuul</h1>
    </div>

    <!-- 雪花天气效果 -->
    <div class="weather-effect">
      <div class="snowflake" v-for="n in 20" :key="n">❄</div>
    </div>

    <!-- 页面布局：只保留中间游戏主视图 -->
    <div class="page-layout">
      <div class="center-game">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 可根据需要添加逻辑
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Press+Start+2P&display=swap');

body {
  margin: 0;
  font-family: 'Press Start 2P', monospace;
  font-size: 10px;
  letter-spacing: 0.5px;
  color: #f0f0f0;
  background: url('/static/img_1.png') no-repeat center center fixed;
  background-size: cover;
  background-attachment: fixed;
}

#app {
  background-color: rgba(0, 0, 0, 0.6);
  min-height: 100vh;
  box-sizing: border-box;
}

.app-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* 顶部标题 */
.game-header {
  width: 100%;
  text-align: center;
  margin-top: 16px;
  animation: fadeIn 2s ease-in-out;
}

.animated-title {
  font-size: 28px;
  color: #ffd700;
  text-shadow: 2px 2px #000;
  animation: glow 2s infinite alternate ease-in-out;
}

@keyframes glow {
  from {
    text-shadow: 0 0 5px #ffd700, 0 0 10px #ff9900;
  }
  to {
    text-shadow: 0 0 20px #ffff66, 0 0 30px #ffcc00;
  }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 雪花天气特效 */
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
  color: #ffffffaa;
  font-size: 1.2em;
  animation: fall linear infinite;
}

@keyframes fall {
  0% {
    transform: translateY(0) rotate(0deg);
    opacity: 1;
  }
  100% {
    transform: translateY(100vh) rotate(360deg);
    opacity: 0.3;
  }
}

.snowflake:nth-child(1) { left: 5%;  animation-duration: 12s; }
.snowflake:nth-child(2) { left: 15%; animation-duration: 8s; }
.snowflake:nth-child(3) { left: 25%; animation-duration: 10s; }
.snowflake:nth-child(4) { left: 35%; animation-duration: 7s; }
.snowflake:nth-child(5) { left: 45%; animation-duration: 9s; }
.snowflake:nth-child(6) { left: 55%; animation-duration: 11s; }
.snowflake:nth-child(7) { left: 65%; animation-duration: 6s; }
.snowflake:nth-child(8) { left: 75%; animation-duration: 10s; }
.snowflake:nth-child(9) { left: 85%; animation-duration: 8s; }
.snowflake:nth-child(10) { left: 95%; animation-duration: 7s; }

/* 页面布局，仅保留中央区域 */
.page-layout {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  height: calc(100vh - 90px);
  padding: 0 40px;
  color: white;
  z-index: 1;
}

/* 中央游戏视图区域样式 */
.center-game {
  flex: none;
  width: 100%;
  max-width: 900px;
  margin: auto;
  padding: 24px;
}
</style>

