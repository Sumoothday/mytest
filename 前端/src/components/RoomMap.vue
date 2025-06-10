<template>
  <div class="room-map-wrapper">
    <!-- SVG 定义 -->
    <svg
      width="0"
      height="0"
      style="position: absolute;"
    >
      <defs>
        <!-- 渐变定义 -->
        <radialGradient id="currentRoomGradient">
          <stop
            offset="0%"
            stop-color="#00d4ff"
            stop-opacity="0.8"
          />
          <stop
            offset="50%"
            stop-color="#7b2ff7"
            stop-opacity="0.6"
          />
          <stop
            offset="100%"
            stop-color="#ff006e"
            stop-opacity="0.4"
          />
        </radialGradient>

        <radialGradient id="visitedRoomGradient">
          <stop
            offset="0%"
            stop-color="#00a8e8"
            stop-opacity="0.6"
          />
          <stop
            offset="100%"
            stop-color="#003459"
            stop-opacity="0.3"
          />
        </radialGradient>

        <radialGradient id="unvisitedRoomGradient">
          <stop
            offset="0%"
            stop-color="#1a1f3a"
            stop-opacity="0.8"
          />
          <stop
            offset="100%"
            stop-color="#0a0e1a"
            stop-opacity="0.4"
          />
        </radialGradient>

        <linearGradient
          id="connectionGradient"
          x1="0%"
          y1="0%"
          x2="100%"
          y2="0%"
        >
          <stop
            offset="0%"
            stop-color="#00d4ff"
            stop-opacity="0.2"
          />
          <stop
            offset="50%"
            stop-color="#00d4ff"
            stop-opacity="0.8"
          />
          <stop
            offset="100%"
            stop-color="#00d4ff"
            stop-opacity="0.2"
          />
        </linearGradient>

        <!-- 滤镜效果 -->
        <filter id="neonGlow">
          <feGaussianBlur
            stdDeviation="3"
            result="coloredBlur"
          />
          <feMerge>
            <feMergeNode in="coloredBlur" />
            <feMergeNode in="coloredBlur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>

        <filter id="roomShadow">
          <feDropShadow
            dx="0"
            dy="0"
            stdDeviation="8"
            flood-color="#00d4ff"
            flood-opacity="0.6"
          />
        </filter>
      </defs>
    </svg>

    <div
      class="map-container"
      :class="{ 'full-mode': mode === 'full' }"
    >
      <!-- 动态背景 -->
      <div class="map-background">
        <div class="grid-lines" />
        <div class="scan-line" />
      </div>

      <!-- 主地图 SVG -->
      <svg
        ref="mapSvg"
        class="map-svg"
        :viewBox="computeViewBox"
        preserveAspectRatio="xMidYMid meet"
      >
        <!-- 网格背景 -->
        <defs>
          <pattern
            id="gridPattern"
            width="50"
            height="50"
            patternUnits="userSpaceOnUse"
          >
            <path
              d="M 50 0 L 0 0 0 50"
              fill="none"
              stroke="#00d4ff"
              stroke-width="0.5"
              opacity="0.1"
            />
          </pattern>
        </defs>

        <rect
          :x="boundingBox.minX"
          :y="boundingBox.minY"
          :width="boundingBox.width"
          :height="boundingBox.height"
          fill="url(#gridPattern)"
        />

        <!-- 雾效果 -->
        <defs>
          <radialGradient id="fogGradient">
            <stop
              offset="0%"
              stop-color="transparent"
            />
            <stop
              offset="70%"
              stop-color="transparent"
            />
            <stop
              offset="100%"
              stop-color="#0a0e1a"
              stop-opacity="0.9"
            />
          </radialGradient>
        </defs>

        <rect
          v-if="fogCenter"
          :x="fogCenter.x - fogRadius * 2"
          :y="fogCenter.y - fogRadius * 2"
          :width="fogRadius * 4"
          :height="fogRadius * 4"
          fill="url(#fogGradient)"
          :transform="`translate(${fogCenter.x - fogRadius * 2}, ${fogCenter.y - fogRadius * 2})`"
        />

        <!-- 房间连接线 -->
        <g class="connections">
          <path
            v-for="(conn, index) in scaledConnections"
            :key="`conn-${index}`"
            :d="getConnectionPath(conn)"
            fill="none"
            stroke="url(#connectionGradient)"
            stroke-width="3"
            stroke-linecap="round"
            opacity="0.8"
            filter="url(#neonGlow)"
          >
            <animate
              attributeName="stroke-dasharray"
              values="0 20;20 0"
              dur="2s"
              repeatCount="indefinite"
            />
          </path>
        </g>

        <!-- 房间节点 -->
        <g class="rooms">
          <g
            v-for="room in scaledRooms"
            :key="room._uid"
            class="room-node"
            :class="{
              'current': room.name === currentRoom,
              'visited': visitedSet.has(room.name),
              'unvisited': !room.name || (!visitedSet.has(room.name) && room.name !== currentRoom)
            }"
          >
            <!-- 外圈光晕 -->
            <circle
              v-if="room.name === currentRoom"
              :cx="room.x"
              :cy="room.y"
              r="40"
              fill="none"
              stroke="#00d4ff"
              stroke-width="2"
              opacity="0.6"
              filter="url(#neonGlow)"
            >
              <animate
                attributeName="r"
                values="35;45;35"
                dur="2s"
                repeatCount="indefinite"
              />
              <animate
                attributeName="opacity"
                values="0.6;0.2;0.6"
                dur="2s"
                repeatCount="indefinite"
              />
            </circle>

            <!-- 房间主体 -->
            <circle
              :cx="room.x"
              :cy="room.y"
              r="30"
              :fill="getRoomFill(room)"
              stroke="#00d4ff"
              :stroke-width="room.name === currentRoom ? '3' : '2'"
              :filter="room.name ? 'url(#roomShadow)' : ''"
              :opacity="room.name ? 1 : 0.3"
            />

            <!-- 房间图标 -->
            <text
              v-if="room.name"
              :x="room.x"
              :y="room.y - 35"
              text-anchor="middle"
              class="room-icon"
              :opacity="visitedSet.has(room.name) || room.name === currentRoom ? 1 : 0.5"
            >
              {{ getRoomIcon(room.name) }}
            </text>

            <!-- 房间名称 -->
            <text
              v-if="room.name && (visitedSet.has(room.name) || room.name === currentRoom)"
              :x="room.x"
              :y="room.y + 5"
              text-anchor="middle"
              class="room-label"
              :class="{ 'current-label': room.name === currentRoom }"
            >
              {{ room.name }}
            </text>

            <!-- 未探索提示 -->
            <text
              v-if="room.name && !visitedSet.has(room.name) && room.name !== currentRoom"
              :x="room.x"
              :y="room.y + 5"
              text-anchor="middle"
              class="unknown-label"
            >
              ???
            </text>
          </g>
        </g>

        <!-- 当前位置指示器 -->
        <g
          v-if="fogCenter"
          class="position-indicator"
        >
          <circle
            :cx="fogCenter.x"
            :cy="fogCenter.y"
            r="5"
            fill="#ff006e"
            filter="url(#neonGlow)"
          >
            <animate
              attributeName="r"
              values="5;8;5"
              dur="1s"
              repeatCount="indefinite"
            />
          </circle>
        </g>
      </svg>

      <!-- 地图控制按钮 -->
      <div
        v-if="mode === 'full'"
        class="map-controls"
      >
        <button
          class="control-btn zoom-in"
          @click="$emit('zoom-in')"
        >
          <span>+</span>
        </button>
        <button
          class="control-btn zoom-out"
          @click="$emit('zoom-out')"
        >
          <span>−</span>
        </button>
        <button
          class="control-btn reset"
          @click="$emit('reset-view')"
        >
          <span>⟲</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';

export default {
  name: 'RoomMap',
  props: {
    currentRoom: {
      type: String,
      default: ''
    },
    scale: {
      type: Number,
      default: 1.0
    },
    mode: {
      type: String,
      default: 'small'
    }
  },
  computed: {
    ...mapState({
      rooms: state => state.roomMap.rooms || [],
      connections: state => state.roomMap.connections || [],
      visitedRooms: state => state.visitedRooms
    }),
    visitedSet() {
      return new Set(this.visitedRooms);
    },
    roomPositionMap() {
      const m = {};
      for (const r of this.rooms) {
        if (r.name && typeof r.x === 'number' && typeof r.y === 'number') {
          m[r.name] = { x: r.x, y: r.y };
        }
      }
      return m;
    },
    filteredRooms() {
      const gridOffsets = [
        [-100, -100], [0, -100], [100, -100],
        [-100,   0], [0,    0], [100,   0],
        [-100, 100], [0,   100], [100, 100]
      ];
      const coordToName = {};
      for (const name in this.roomPositionMap) {
        const { x, y } = this.roomPositionMap[name];
        coordToName[`${x},${y}`] = name;
      }
      return gridOffsets.map(([dx, dy]) => {
        const key = `${dx},${dy}`;
        if (coordToName[key]) {
          const realName = coordToName[key];
          const realRoom = this.rooms.find(r => r.name === realName);
          return { ...realRoom, _uid: `room-real-${realName}` };
        } else {
          return {
            name: '',
            visited: false,
            x: dx,
            y: dy,
            exits: {},
            _uid: `room-placeholder-${dx}-${dy}`
          };
        }
      });
    },
    filteredConnections() {
      const realCoords = new Set(
        this.filteredRooms
          .filter(r => r.name)
          .map(r => `${r.x},${r.y}`)
      );
      return this.connections
        .map(c => {
          const fromPos = this.roomPositionMap[c.from.name];
          const toPos = this.roomPositionMap[c.to.name];
          if (!fromPos || !toPos) return null;
          const fk = `${fromPos.x},${fromPos.y}`;
          const tk = `${toPos.x},${toPos.y}`;
          if (realCoords.has(fk) && realCoords.has(tk)) {
            return {
              from: { x: fromPos.x, y: fromPos.y },
              to:   { x: toPos.x,  y: toPos.y }
            };
          }
          return null;
        })
        .filter(Boolean);
    },
    scaledRooms() {
      return this.filteredRooms.map(r => ({
        ...r,
        x: r.x * this.scale,
        y: r.y * this.scale
      }));
    },
    scaledConnections() {
      return this.filteredConnections.map(c => ({
        from: { x: c.from.x * this.scale, y: c.from.y * this.scale },
        to:   { x: c.to.x * this.scale,   y: c.to.y * this.scale }
      }));
    },
    boundingBox() {
      const xs = this.scaledRooms.map(r => r.x).filter(x => typeof x === 'number' && !isNaN(x));
      const ys = this.scaledRooms.map(r => r.y).filter(y => typeof y === 'number' && !isNaN(y));
      if (!xs.length || !ys.length) {
        return { minX: 0, minY: 0, width: 500, height: 500 };
      }
      const minX = Math.min(...xs) - 80;
      const minY = Math.min(...ys) - 80;
      const maxX = Math.max(...xs) + 80;
      const maxY = Math.max(...ys) + 80;
      return { minX, minY, width: maxX - minX, height: maxY - minY };
    },
    computeViewBox() {
      const { minX, minY, width, height } = this.boundingBox;
      return `${minX} ${minY} ${width} ${height}`;
    },
    fogCenter() {
      if (!this.currentRoom) return null;
      const pos = this.roomPositionMap[this.currentRoom];
      if (!pos) return null;
      return { x: pos.x * this.scale, y: pos.y * this.scale };
    },
    fogRadius() {
      return 150 * this.scale;
    }
  },
  methods: {
    getRoomFill(room) {
      if (room.name === this.currentRoom) {
        return 'url(#currentRoomGradient)';
      } else if (this.visitedSet.has(room.name)) {
        return 'url(#visitedRoomGradient)';
      } else {
        return 'url(#unvisitedRoomGradient)';
      }
    },
    getRoomIcon(roomName) {
      const iconMap = {
        'outside': '🌳',
        'lecture_theatre': '🎭',
        'computing_lab': '💻',
        'library': '📚',
        'cafeteria': '🍽️',
        'dormitory': '🛏️'
      };
      return iconMap[roomName] || '📍';
    },
    getConnectionPath(conn) {
      const dx = conn.to.x - conn.from.x;
      const dy = conn.to.y - conn.from.y;
      const mx = conn.from.x + dx / 2;
      const my = conn.from.y + dy / 2;
      return `M ${conn.from.x} ${conn.from.y} Q ${mx} ${my} ${conn.to.x} ${conn.to.y}`;
    }
  }
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700&display=swap');

.room-map-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.map-container {
  position: relative;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(0, 10, 20, 0.9) 0%, rgba(0, 20, 40, 0.9) 100%);
  border: 2px solid rgba(0, 168, 232, 0.3);
  border-radius: 20px;
  overflow: hidden;
  box-shadow:
    0 0 40px rgba(0, 168, 232, 0.2),
    inset 0 0 20px rgba(0, 168, 232, 0.1);
}

.map-container.full-mode {
  border-radius: 0;
  border: none;
}

/* 动态背景 */
.map-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.grid-lines {
  position: absolute;
  width: 100%;
  height: 100%;
  background-image:
    linear-gradient(rgba(0, 168, 232, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 168, 232, 0.05) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: grid-drift 20s linear infinite;
}

@keyframes grid-drift {
  0% { transform: translate(0, 0); }
  100% { transform: translate(50px, 50px); }
}

.scan-line {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, transparent, rgba(0, 212, 255, 0.8), transparent);
  animation: scan 3s linear infinite;
}

@keyframes scan {
  0% { transform: translateY(0); }
  100% { transform: translateY(100vh); }
}

/* SVG 样式 */
.map-svg {
  width: 100%;
  height: 100%;
}

/* 房间标签 */
.room-label {
  font-family: 'Orbitron', sans-serif;
  font-size: 14px;
  font-weight: 400;
  fill: #98c1d9;
  text-shadow: 0 0 8px rgba(0, 168, 232, 0.8);
}

.room-label.current-label {
  font-weight: 700;
  fill: #fff;
  font-size: 16px;
  text-shadow: 0 0 12px rgba(0, 212, 255, 1);
}

.unknown-label {
  font-family: 'Orbitron', sans-serif;
  font-size: 14px;
  fill: #4a5875;
  opacity: 0.6;
}

.room-icon {
  font-size: 20px;
  filter: drop-shadow(0 0 8px rgba(0, 212, 255, 0.8));
}

/* 控制按钮 */
.map-controls {
  position: absolute;
  bottom: 20px;
  right: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.control-btn {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, rgba(0, 168, 232, 0.2) 0%, rgba(123, 47, 247, 0.2) 100%);
  border: 2px solid rgba(0, 168, 232, 0.4);
  border-radius: 12px;
  color: #00d4ff;
  font-size: 24px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.control-btn:hover {
  transform: scale(1.1);
  border-color: #00d4ff;
  box-shadow: 0 0 20px rgba(0, 212, 255, 0.6);
  background: linear-gradient(135deg, rgba(0, 168, 232, 0.3) 0%, rgba(123, 47, 247, 0.3) 100%);
}

.control-btn span {
  filter: drop-shadow(0 0 4px currentColor);
}

/* 响应式 */
@media (max-width: 768px) {
  .map-controls {
    bottom: 10px;
    right: 10px;
  }

  .control-btn {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }
}
</style>
