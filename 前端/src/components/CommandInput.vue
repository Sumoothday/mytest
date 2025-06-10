<template>
  <div class="command-panel">
    <!-- 方向控制区 -->
    <div class="control-section direction-section">
      <div class="section-header">
        <span class="header-icon">🧭</span>
        <span class="header-text">方向控制</span>
      </div>
      <div class="direction-pad">
        <button
          class="dir-btn north"
          @click="send('go north')"
        >
          <span class="btn-icon">⬆</span>
          <span class="btn-label">北</span>
        </button>
        <button
          class="dir-btn west"
          @click="send('go west')"
        >
          <span class="btn-icon">⬅</span>
          <span class="btn-label">西</span>
        </button>
        <button
          class="dir-btn center"
          @click="send('look')"
        >
          <span class="btn-icon">👁️</span>
          <span class="btn-label">查看</span>
        </button>
        <button
          class="dir-btn east"
          @click="send('go east')"
        >
          <span class="btn-icon">➡</span>
          <span class="btn-label">东</span>
        </button>
        <button
          class="dir-btn south"
          @click="send('go south')"
        >
          <span class="btn-icon">⬇</span>
          <span class="btn-label">南</span>
        </button>
      </div>
    </div>

    <!-- 快捷操作区 -->
    <div class="control-section action-section">
      <div class="section-header">
        <span class="header-icon">⚡</span>
        <span class="header-text">快捷操作</span>
      </div>
      <div class="action-buttons">
        <button
          class="action-btn"
          @click="send('back')"
        >
          <span class="action-icon">🔙</span>
          <span class="action-text">返回</span>
        </button>
        <button
          class="action-btn"
          @click="send('eat magic cake')"
        >
          <span class="action-icon">🍪</span>
          <span class="action-text">魔法饼干</span>
        </button>
      </div>
    </div>

    <!-- 命令输入区 -->
    <div class="control-section input-section">
      <div class="section-header">
        <span class="header-icon">💻</span>
        <span class="header-text">命令输入</span>
      </div>
      <div class="command-input-wrapper">
        <div class="input-container">
          <span class="input-prefix">></span>
          <input
            v-model="command"
            type="text"
            class="command-input"
            placeholder="输入指令..."
            @keyup.enter="submit"
          >
        </div>
        <button
          class="submit-btn"
          @click="submit"
        >
          <span class="submit-icon">⚡</span>
          <span class="submit-text">执行</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      command: '',
      clickSound: null
    };
  },
  mounted() {
    this.clickSound = new Audio(require('@/assets/按钮.mp3'));
  },
  methods: {
    submit() {
      if (this.command.trim() === '') return;
      this.playSound();
      this.send(this.command.trim());
      this.command = '';
    },
    send(fullCmd) {
      this.playSound();
      const parts = fullCmd.split(/\s+/);
      const verb = parts[0].toLowerCase();
      const param = parts.slice(1).join(' ');

      const currentRoom = this.$store.state.roomMap.rooms.find(
        r => r.name === this.$store.state.roomName
      );

      if (verb === 'go') {
        if (!currentRoom) {
          this.$store.commit('addMessage', '⚠️ 当前房间不存在！');
          return;
        }

        const exits = currentRoom.exits || {};
        const targetRoomName = exits[param];

        if (!targetRoomName) {
          this.$store.commit('addMessage', '🚧 前方没有通路');
          return;
        }

        this.$store.commit('changeRoom', targetRoomName);
        this.$store.commit('addMessage', `你走向了 ${targetRoomName}`);
      }

      this.sendToBackend(verb, param);
    },
    async sendToBackend(cmdVerb, cmdParam) {
      try {
        const sessionId = this.$store.state.sessionId || 'demo-session';
        const response = await axios.post(
          'http://localhost:8080/api/command',
          null,
          {
            headers: { 'X-Session-Id': sessionId },
            params: { command: cmdVerb, parameter: cmdParam }
          }
        );

        this.$store.commit('updateGameState', response.data);
        this.$emit('on-command', response.data);

        if (cmdVerb === 'back') {
          const lookResp = await axios.post(
            'http://localhost:8080/api/command',
            null,
            {
              headers: { 'X-Session-Id': sessionId },
              params: { command: 'look', parameter: '' }
            }
          );
          this.$store.commit('updateGameState', lookResp.data);
          this.$emit('on-command', lookResp.data);
        }
      } catch (error) {
        console.error('❌ 指令请求失败:', error);
        this.$store.commit('addMessage', '⚠️ 请求失败，请检查网络或后端状态');
      }
    },
    playSound() {
      if (!this.clickSound) return;
      this.clickSound.currentTime = 0;
      this.clickSound.play();
    }
  }
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700&family=Share+Tech+Mono&display=swap');

.command-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px;
  background: linear-gradient(135deg, rgba(0, 10, 20, 0.9) 0%, rgba(0, 20, 40, 0.9) 100%);
  border: 2px solid rgba(0, 168, 232, 0.3);
  border-radius: 20px;
  backdrop-filter: blur(20px);
  box-shadow:
    0 0 40px rgba(0, 168, 232, 0.2),
    inset 0 0 20px rgba(0, 168, 232, 0.1);
}

/* 控制区块 */
.control-section {
  position: relative;
  padding: 20px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(0, 168, 232, 0.2);
  border-radius: 16px;
  transition: all 0.3s ease;
}

.control-section:hover {
  border-color: rgba(0, 168, 232, 0.4);
  box-shadow: inset 0 0 20px rgba(0, 168, 232, 0.1);
}

/* 区块头部 */
.section-header {
  position: absolute;
  top: -12px;
  left: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  background: linear-gradient(135deg, rgba(0, 10, 20, 0.95) 0%, rgba(0, 20, 40, 0.95) 100%);
  border: 1px solid rgba(0, 168, 232, 0.4);
  border-radius: 20px;
}

.header-icon {
  font-size: 16px;
  filter: drop-shadow(0 0 8px rgba(0, 212, 255, 0.8));
}

.header-text {
  font-family: 'Orbitron', sans-serif;
  font-size: 12px;
  font-weight: 700;
  color: #00d4ff;
  text-transform: uppercase;
  letter-spacing: 1px;
}

/* 方向键盘 */
.direction-pad {
  display: grid;
  grid-template-columns: repeat(3, 60px);
  grid-template-rows: repeat(3, 60px);
  gap: 8px;
  justify-content: center;
  margin-top: 8px;
}

.dir-btn {
  position: relative;
  background: linear-gradient(135deg, rgba(0, 168, 232, 0.2) 0%, rgba(123, 47, 247, 0.2) 100%);
  border: 2px solid rgba(0, 168, 232, 0.4);
  border-radius: 12px;
  color: #00d4ff;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  overflow: hidden;
}

.dir-btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.4) 0%, transparent 70%);
  transition: all 0.3s ease;
  transform: translate(-50%, -50%);
}

.dir-btn:hover::before {
  width: 100%;
  height: 100%;
}

.dir-btn:hover {
  transform: scale(1.05);
  border-color: #00d4ff;
  box-shadow:
    0 0 20px rgba(0, 212, 255, 0.6),
    inset 0 0 15px rgba(0, 212, 255, 0.2);
}

.dir-btn:active {
  transform: scale(0.95);
}

.dir-btn.north { grid-column: 2; grid-row: 1; }
.dir-btn.west { grid-column: 1; grid-row: 2; }
.dir-btn.center { grid-column: 2; grid-row: 2; }
.dir-btn.east { grid-column: 3; grid-row: 2; }
.dir-btn.south { grid-column: 2; grid-row: 3; }

.btn-icon {
  font-size: 20px;
  filter: drop-shadow(0 0 4px currentColor);
}

.btn-label {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  opacity: 0.8;
}

/* 快捷操作按钮 */
.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 8px;
}

.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 20px;
  background: linear-gradient(135deg, rgba(0, 168, 232, 0.2) 0%, rgba(123, 47, 247, 0.2) 100%);
  border: 2px solid rgba(0, 168, 232, 0.4);
  border-radius: 12px;
  color: #00d4ff;
  font-family: 'Share Tech Mono', monospace;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.action-btn::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(0, 212, 255, 0.4), transparent);
  transition: left 0.5s ease;
}

.action-btn:hover::after {
  left: 100%;
}

.action-btn:hover {
  transform: translateY(-2px);
  border-color: #00d4ff;
  box-shadow:
    0 8px 24px rgba(0, 212, 255, 0.4),
    inset 0 0 15px rgba(0, 212, 255, 0.2);
}

.action-icon {
  font-size: 18px;
  filter: drop-shadow(0 0 6px currentColor);
}

/* 命令输入 */
.command-input-wrapper {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.input-container {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  background: rgba(0, 0, 0, 0.5);
  border: 2px solid rgba(0, 168, 232, 0.3);
  border-radius: 12px;
  padding: 0 16px;
  transition: all 0.3s ease;
}

.input-container:focus-within {
  border-color: #00d4ff;
  box-shadow:
    0 0 20px rgba(0, 212, 255, 0.4),
    inset 0 0 10px rgba(0, 212, 255, 0.1);
}

.input-prefix {
  color: #00d4ff;
  font-family: 'Share Tech Mono', monospace;
  font-size: 16px;
  font-weight: 700;
  margin-right: 8px;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0.3; }
}

.command-input {
  flex: 1;
  background: transparent;
  border: none;
  color: #98c1d9;
  font-family: 'Share Tech Mono', monospace;
  font-size: 14px;
  padding: 12px 0;
  outline: none;
}

.command-input::placeholder {
  color: rgba(152, 193, 217, 0.4);
}

.submit-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #00d4ff 0%, #7b2ff7 100%);
  border: none;
  border-radius: 12px;
  color: #fff;
  font-family: 'Orbitron', sans-serif;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  text-transform: uppercase;
  letter-spacing: 1px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.submit-btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  transition: all 0.5s ease;
}

.submit-btn:hover::before {
  width: 200%;
  height: 200%;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(0, 212, 255, 0.5);
}

.submit-btn:active {
  transform: translateY(0);
}

.submit-icon {
  font-size: 16px;
  filter: drop-shadow(0 0 4px rgba(255, 255, 255, 0.8));
}

/* 响应式设计 */
@media (max-width: 480px) {
  .command-panel {
    padding: 16px;
  }

  .direction-pad {
    grid-template-columns: repeat(3, 50px);
    grid-template-rows: repeat(3, 50px);
  }

  .action-buttons {
    flex-direction: column;
  }
}
</style>
