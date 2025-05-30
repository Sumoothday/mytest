import Vue from 'vue'
import Vuex from 'vuex'
import axios from 'axios'

Vue.use(Vuex)

function genEmptyRoomLayout() {
  const items = ['monster', 'trap', 'chest', 'door', '']
  const layout = []
  for (let i = 0; i < 5; i++) {
    const row = []
    for (let j = 0; j < 5; j++) {
      const r = Math.random()
      row.push(r < 0.2 ? items[Math.floor(Math.random() * 4)] : '')
    }
    layout.push(row)
  }
  return layout
}

const roomDefs = []
for (let i = 0; i < 8; i++) {
  for (let j = 0; j < 8; j++) {
    const x = i * 100
    const y = j * 100
    let name = `区域_${i}_${j}`
    if (i === 2 && j === 2) name = '起始房间'
    if (i === 1 && j === 1) name = '藏宝室'
    if (i === 3 && j === 3) name = '陷阱洞'
    if (i === 0 && j === 2) name = '图书馆'
    if (i === 0 && j === 4) name = '祭坛'
    if (i === 1 && j === 3) name = '密道入口'
    if (i === 2 && j === 0) name = '武器库'
    if (i === 3 && j === 1) name = '炼金房'
    if (i === 4 && j === 0) name = '守卫营地'
    if (i === 4 && j === 4) name = '魔法阵'
    if (i === 5 && j === 5) name = '神秘花园'
    if (i === 6 && j === 2) name = '毒蛇巢穴'
    if (i === 7 && j === 7) name = '终点之门'
    roomDefs.push({ name, x, y })
  }
}

const rooms = roomDefs.map(room => ({
  ...room,
  layout:
    room.name === '起始房间'
      ? [
          ['', '', '', '', ''],
          ['', '', '', '', ''],
          ['', '', 'player', '', ''],
          ['', '', 'monster', '', ''],
          ['', '', 'chest', '', '']
        ]
      : genEmptyRoomLayout(),
  visited: room.name === '起始房间'
}))

const connections = []
for (let i = 0; i < 8; i++) {
  for (let j = 0; j < 8; j++) {
    const x = i * 100
    const y = j * 100
    if (i < 7) connections.push({ from: { x, y }, to: { x: x + 100, y } })
    if (j < 7) connections.push({ from: { x, y }, to: { x, y: y + 100 } })
  }
}

export default new Vuex.Store({
  state: {
    messages: ['欢迎来到文字冒险游戏！'],
    roomItems: [],
    inventory: [],
    totalWeight: 0,
    weightLimit: 10,
    playerName: '玩家1',
    roomName: '起始房间',

    // <--- 初始就把起始房间放进去
    visitedRooms: ['起始房间'],

    roomMap: { rooms, connections }
  },

  mutations: {
    addMessage(state, msg) {
      state.messages.push(msg)
    },

    // 每次切换房间，都 push 到 visitedRooms
    changeRoom(state, newRoomName) {
      state.roomName = newRoomName
      const room = state.roomMap.rooms.find(r => r.name === newRoomName)
      if (room) room.visited = true

      if (!state.visitedRooms.includes(newRoomName)) {
        state.visitedRooms.push(newRoomName)
      }
    },

    // 后端通过 updateGameState 直接更新 roomName 时，也要记录
    updateGameState(state, payload) {
      state.roomItems = payload.roomItems
      state.inventory = payload.playerItems
      state.totalWeight = payload.playerWeight
      state.weightLimit = payload.playerWeightLimit

      if (payload.roomName) {
        state.roomName = payload.roomName
        // 标记该房间已访问
        const r = state.roomMap.rooms.find(r => r.name === payload.roomName)
        if (r) r.visited = true

        if (!state.visitedRooms.includes(payload.roomName)) {
          state.visitedRooms.push(payload.roomName)
        }
      }

      if (payload.roomMap) {
        state.roomMap = payload.roomMap
      }
    }
  },

  actions: {
    // 入口命令：依旧调用 updateGameState
    async sendCommand({ commit }, command) {
      commit('addMessage', `> ${command}`)
      try {
        const res = await axios.post('/api/command', { command })
        commit('addMessage', res.data.message)
        commit('updateGameState', res.data)
      } catch (e) {
        commit('addMessage', '指令执行失败，请检查服务器')
      }
    },

    // 方向移动：走通路后用 changeRoom 记录
    moveToDirection({ state, commit }, direction) {
      const map = {
        east: { dx: 100, dy: 0 },
        west: { dx: -100, dy: 0 },
        north: { dx: 0, dy: -100 },
        south: { dx: 0, dy: 100 }
      }
      const offset = map[direction]
      if (!offset) {
        commit('addMessage', `🚫 无效的方向: ${direction}`)
        return
      }
      const cur = state.roomMap.rooms.find(r => r.name === state.roomName)
      if (!cur) {
        commit('addMessage', '⚠️ 当前房间不存在！')
        return
      }
      const tx = cur.x + offset.dx
      const ty = cur.y + offset.dy
      const ok = state.roomMap.connections.some(c =>
        (c.from.x === cur.x && c.from.y === cur.y && c.to.x === tx && c.to.y === ty) ||
        (c.to.x === cur.x && c.to.y === cur.y && c.from.x === tx && c.from.y === ty)
      )
      if (!ok) {
        commit('addMessage', '🚧 前方没有通路')
        return
      }
      const target = state.roomMap.rooms.find(r => r.x === tx && r.y === ty)
      if (!target) {
        commit('addMessage', '❓ 前方是未知空间，无法进入。')
        return
      }
      commit('changeRoom', target.name)
      commit('addMessage', `你进入了 ${target.name}`)
    }
  }
})
