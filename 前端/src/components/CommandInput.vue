
<template>
  <div class="game-box command-panel">
    <!-- 移动指令组 -->
    <div class="block">
      <div class="move-buttons">
        <button @click="send('go north')">
          ⬆
        </button>
        <div class="middle">
          <button @click="send('go west')">
            ⬅
          </button>
          <button @click="send('look')">
            🔍
          </button>
          <button @click="send('go east')">
            ➡
          </button>
        </div>
        <button @click="send('go south')">
          ⬇
        </button>
      </div>
    </div>

    <!-- 功能指令组 -->
    <div class="block">
      <div class="action-buttons">
        <button @click="send('items')">
          🎒 背包
        </button>
        <button @click="send('back')">
          🔙 返回
        </button>
        <button @click="send('take')">
          📥 拾取
        </button>
        <button @click="send('drop')">
          📤 丢弃
        </button>
        <button @click="send('eat cookie')">
          🍪 魔法饼干
        </button>
      </div>
    </div>

    <!-- 自定义输入 -->
    <div class="block input-block">
      <input
        v-model="command"
        type="text"
        placeholder="请输入指令…"
        @keyup.enter="submit"
      >
      <button @click="submit">
        执行
      </button>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      command: '',
      clickSound: null
    };
  },
  mounted() {
    this.clickSound = new Audio(require('@/assets/sound2.mp3'));
  },
  methods: {
    submit() {
      if (this.command.trim() !== '') {
        this.playSound();
        this.$emit('on-command', this.command.trim());
        this.command = '';
      }
    },
    send(cmd) {
      this.playSound();

      // 如果是移动指令
      if (cmd.startsWith('go')) {
        const directionMap = {
          'go east': { dx: 100, dy: 0 },
          'go west': { dx: -100, dy: 0 },
          'go north': { dx: 0, dy: -100 },
          'go south': { dx: 0, dy: 100 }
        };

        const offset = directionMap[cmd];
        if (!offset) return this.$emit('on-command', cmd);

        const currentRoom = this.$store.state.roomMap.rooms.find(r => r.name === this.$store.state.roomName);
        if (!currentRoom) return this.$store.commit('addMessage', '⚠️ 当前房间不存在！');

        const targetX = currentRoom.x + offset.dx;
        const targetY = currentRoom.y + offset.dy;

        const connectionExists = this.$store.state.roomMap.connections.some(conn => {
          const from = conn.from;
          const to = conn.to;
          return (
            (from.x === currentRoom.x && from.y === currentRoom.y && to.x === targetX && to.y === targetY) ||
            (to.x === currentRoom.x && to.y === currentRoom.y && from.x === targetX && from.y === targetY)
          );
        });

        if (!connectionExists) {
          this.$store.commit('addMessage', '🚫 前方无通路！');
          return;
        }

        // 找目标房间名
        const targetRoom = this.$store.state.roomMap.rooms.find(r => r.x === targetX && r.y === targetY);
        if (!targetRoom) {
          this.$store.commit('addMessage', '❓ 前方是未知空间，无法进入。');
          return;
        }

        // 切换房间
        this.$store.commit('changeRoom', targetRoom.name);
        this.$store.commit('addMessage', `你走向了 ${targetRoom.name}`);
        return;
      }

      // 非移动指令
      this.$emit('on-command', cmd);
    },
    playSound() {
      if (this.clickSound) {
        this.clickSound.currentTime = 0;
        this.clickSound.play();
      }
    }
  }
};
</script>

<style scoped>
.command-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  border-radius: 16px;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(10px);
  border: 2px solid #ffc107;
  box-shadow: 0 0 12px rgba(255, 193, 7, 0.3);
}

.block {
  border: 1px dashed #ffaa00;
  border-radius: 12px;
  padding: 14px;
  background-color: rgba(255, 255, 255, 0.03);
}

.move-buttons {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.move-buttons .middle {
  display: flex;
  gap: 16px;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
}

.input-block {
  display: flex;
  gap: 12px;
  align-items: center;
}

button {
  font-family: 'Press Start 2P', monospace !important;
  font-size: 12px;
  padding: 10px 16px;
  color: #000;
  background: linear-gradient(to bottom, #ffd54f, #ffca28);
  border: 2px solid #222;
  border-radius: 10px;
  box-shadow: 0 4px #c19a00;
  cursor: pointer;
  transition: transform 0.1s ease;
}

button:active {
  transform: translateY(2px);
  box-shadow: 0 2px #c19a00;
}

input[type="text"] {
  flex: 1;
  background: #222;
  color: #ffd54f;
  border: 2px solid #555;
  border-radius: 10px;
  padding: 10px;
  font-family: 'Courier New', monospace;
  font-size: 14px;
}
</style>


