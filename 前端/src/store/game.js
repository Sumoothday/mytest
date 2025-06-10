import Vue from 'vue';
import Vuex from 'vuex';
import axios from 'axios';

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    sessionId: 'demo-session',
    messages: ['欢迎来到文字冒险游戏！'],
    roomItems: [],    // 用来展示当前房间里的物品列表
    inventory: [],    // 玩家背包
    totalWeight: 0,   // 当前背包重量
    weightLimit: 10,  // 背包负重上限
    playerName: '玩家1',

    // 初始房间名置空，后续在 startGame() 里由后端填充
    roomName: '',
    visitedRooms: [],

    roomMap: {
      // rooms 保存 { name, visited, x, y, exits }
      rooms: [],
      // connections 保存 { from: {name}, to: {name} }
      connections: []
    }
  },

  mutations: {
    setSessionId(state, sessionId) {
      state.sessionId = sessionId;
    },

    addMessage(state, msg) {
      state.messages.push(msg);
    },

    changeRoom(state, newRoomName) {
      state.roomName = newRoomName;
      const room = state.roomMap.rooms.find(r => r.name === newRoomName);
      if (room) room.visited = true;
      if (!state.visitedRooms.includes(newRoomName)) {
        state.visitedRooms.push(newRoomName);
      }
    },

    updateInventory(state, items) {
      state.inventory = items;
    },

    /**
     * 更新整体游戏状态（房间、背包、地图等）
     * payload 是后端返回的整个 response 对象
     */
    updateGameState(state, payload) {
      // —— 更新“当前房间内的物品” —— //
      // 后端如果把房间物品放到 payload.currentRoom.items，就用它。否则尝试 payload.roomItems。
      if (payload.currentRoom && Array.isArray(payload.currentRoom.items)) {
        state.roomItems = payload.currentRoom.items;
      } else if (Array.isArray(payload.roomItems)) {
        state.roomItems = payload.roomItems;
      } else {
        state.roomItems = [];
      }

      // —— 更新“玩家背包”和“当前重量/上限” —— //
      state.inventory = Array.isArray(payload.inventory) ? payload.inventory : [];

      // 后端返回的是 currentWeight，映射到 totalWeight
      state.totalWeight = typeof payload.currentWeight === 'number'
        ? payload.currentWeight
        : 0;

      // 后端返回的是 maxWeight，映射到 weightLimit
      state.weightLimit = typeof payload.maxWeight === 'number'
        ? payload.maxWeight
        : state.weightLimit; // 如果没给就保留原值

      // —— 打印一下调试信息（可选） —— //
      console.log('当前房间物品：', state.roomItems);
      console.log('背包列表：', state.inventory);
      console.log('当前负重：', state.totalWeight, '/', state.weightLimit);

      // —— 更新“当前房间名”并标记已访问 —— //
      let roomName = null;
      if (payload.currentRoom) {
        roomName = payload.currentRoom.name || payload.currentRoom.description;
      } else if (payload.roomName) {
        roomName = payload.roomName;
      }

      if (roomName) {
        state.roomName = roomName;
        const existing = state.roomMap.rooms.find(r => r.name === roomName);
        if (existing) existing.visited = true;
        if (!state.visitedRooms.includes(roomName)) {
          state.visitedRooms.push(roomName);
        }
      }

      // —— 重新构造房间地图（rooms + connections），如果后端传来了 exits —— //
      if (payload.currentRoom && payload.currentRoom.exits) {
        const raw = payload.currentRoom;
        const currentRoomName = raw.name || raw.description;

        const dirOffsets = {
          north: [0, -1],
          south: [0, 1],
          east: [1, 0],
          west: [-1, 0]
        };

        // 用 Map 保证不重复写入同名节点
        const newRoomsMap = new Map();
        const newConnections = [];

        // 先把当前房间放进去，坐标 (0,0)
        newRoomsMap.set(currentRoomName, {
          name: currentRoomName,
          visited: true,
          x: 0,
          y: 0,
          exits: raw.exits
        });

        // 再遍历 exits，把相邻房间算出来
        Object.keys(raw.exits).forEach(direction => {
          const targetRoomName = raw.exits[direction];
          newConnections.push({
            from: { name: currentRoomName },
            to:   { name: targetRoomName }
          });

          const offset = dirOffsets[direction] || [0, 0];
          const nx = offset[0] * 100;
          const ny = offset[1] * 100;

          if (!newRoomsMap.has(targetRoomName)) {
            newRoomsMap.set(targetRoomName, {
              name: targetRoomName,
              visited: false,
              x: nx,
              y: ny,
              exits: {} // 先占位，等以后走到该房间时再更新 exits
            });
          }
        });

        // 把 Map 转成数组并更新到 state
        state.roomMap = {
          rooms: Array.from(newRoomsMap.values()),
          connections: newConnections
        };
      }
    }
  },

  actions: {
    /**
     * 游戏启动时调用，得到第一个房间的状态
     */
    async startGame({ commit }, { username, password }) {
      try {
        const res = await axios.post(
          'http://localhost:8080/api/login',
          null,
          {
            params: {
              username: username,
              password: password
            }
          }
        );
        const data = res.data;

        if (data.sessionId) commit('setSessionId', data.sessionId);
        if (data.message) commit('addMessage', data.message);

        // 让 updateGameState 把 currentRoom、inventory、currentWeight 等写入 store
        commit('updateGameState', data);

        // 调试输出
        console.log('🗺️ 初始房间地图结构:');
        console.log('📌 Rooms:', this.state.roomMap.rooms);
        console.log('🔗 Connections:', this.state.roomMap.connections);

        return data;
      } catch (error) {
        console.error('❌ 启动游戏（登录）失败:', error);
        commit('addMessage', '❌ 登录失败，请检查用户名或密码。');
        throw error;
      }
    },

    /**
     * 发送用户命令（go、look、items、take、drop 等）
     */
    async sendCommand({ commit, state }, command) {
      commit('addMessage', `> ${command}`);
      try {
        const res = await axios.post(
          'http://localhost:8080/api/command',
          null,
          {
            headers: {
              'X-Session-Id': state.sessionId
            },
            params: {
              command,
              parameter: ''
            }
          }
        );
        const data = res.data;
        if (data.message) commit('addMessage', data.message);

        // 关键：后端返回的 data 中包含 currentRoom、inventory、currentWeight、maxWeight 等字段
        commit('updateGameState', data);
      } catch (e) {
        console.error('❌ 指令执行错误:', e);
        commit('addMessage', '❌ 指令执行失败，请检查后端是否运行。');
      }
    },

    /**
     * 前端“切换房间”时的乐观更新（主要给 GameView.vue 的 watcher 调用）
     */
    enterRoom({ commit }, roomName) {
      commit('changeRoom', roomName);
      commit('addMessage', `你进入了 ${roomName}`);
    }
  }
});
