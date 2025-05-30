<template>
  <div class="room-map-container">
    width="100%"
    height="100%"
    :viewBox="computeViewBox"
    preserveAspectRatio="xMidYMid meet"
    ref="mapSvg"
    >
    <defs>
      <!-- 你的渐变定义保持不变 -->
    </defs>

    <!-- 只渲染 filteredConnections -->
    <line
      v-for="(conn, index) in scaledConnections"
      :key="'conn' + index"
      :x1="conn.from.x"
      :y1="conn.from.y"
      :x2="conn.to.x"
      :y2="conn.to.y"
      stroke="#aaa"
      stroke-width="2"
      stroke-dasharray="4,2"
    />

    <!-- 渲染 filteredRooms，但区分已访问/未访问样式 -->
    <g
      v-for="room in scaledRooms"
      :key="room.name"
    >
      <circle
        :cx="room.x"
        :cy="room.y"
        r="35"
        :fill="
          room.name === currentRoom
            ? '#ff9900'
            : visitedSet.has(room.name)
              ? '#aaaaaa'
              : '#444444'
        "
        stroke="#333"
        stroke-width="1.5"
      />
      <!-- 只有已访问的房间才显示名称 -->
      <text
        v-if="visitedSet.has(room.name) || room.name === currentRoom"
        :x="room.x"
        :y="room.y + 6"
        text-anchor="middle"
        :fill="room.name === currentRoom ? '#ffff00' : '#555555'"
        class="room-label"
      >
        {{ room.name }}
      </text>
    </g>

  </div>
</template>

<script>
import { mapState } from 'vuex';

export default {
  name: 'RoomMap',
  props: {
    currentRoom: String,
    scale: { type: Number, default: 1.0 },
    mode: { type: String, default: 'small' }
  },
  computed: {
    ...mapState({
      rooms: state => state.roomMap.rooms,
      connections: state => state.roomMap.connections,
      visitedRooms: state => state.visitedRooms
    }),
    // 方便快速查验
    visitedSet() {
      return new Set(this.visitedRooms);
    },

    // 还原你原来的 small 逻辑；full 模式下再加一次 visited 过滤
    filteredRooms() {
      if (this.mode === 'full') {
        return this.rooms.filter(r => this.visitedSet.has(r.name));
      }
      // small 模式：3×3 网格内所有房间（无论是否访问过）
      const current = this.rooms.find(r => r.name === this.currentRoom);
      if (!current) return [];
      const xs = [...new Set(this.rooms.map(r => r.x))].sort((a, b) => a - b);
      const ys = [...new Set(this.rooms.map(r => r.y))].sort((a, b) => a - b);
      const xi = xs.indexOf(current.x);
      const yi = ys.indexOf(current.y);
      const x0 = Math.max(0, Math.min(xi - 1, xs.length - 3));
      const y0 = Math.max(0, Math.min(yi - 1, ys.length - 3));
      const xRange = xs.slice(x0, x0 + 3);
      const yRange = ys.slice(y0, y0 + 3);
      return this.rooms.filter(r => xRange.includes(r.x) && yRange.includes(r.y));
    },

    // 连接线依旧只保留两个端点都在 filteredRooms 中的
    filteredConnections() {
      const names = new Set(this.filteredRooms.map(r => r.name));
      return this.connections.filter(c =>
        names.has(c.from.name) && names.has(c.to.name)
      );
    },

    // 按比例缩放
    scaledRooms() {
      return this.filteredRooms.map(r => ({ ...r, x: r.x * this.scale, y: r.y * this.scale }));
    },
    scaledConnections() {
      return this.filteredConnections.map(c => ({
        from: { x: c.from.x * this.scale, y: c.from.y * this.scale },
        to:   { x: c.to.x   * this.scale, y: c.to.y   * this.scale }
      }));
    },

    // 视口包裹当前可见区域
    boundingBox() {
      const xs = this.scaledRooms.map(r => r.x);
      const ys = this.scaledRooms.map(r => r.y);
      const minX = Math.min(...xs) - 50;
      const minY = Math.min(...ys) - 50;
      const maxX = Math.max(...xs) + 50;
      const maxY = Math.max(...ys) + 50;
      return { minX, minY, width: maxX - minX, height: maxY - minY };
    },
    computeViewBox() {
      const { minX, minY, width, height } = this.boundingBox;
      return `${minX} ${minY} ${width} ${height}`;
    }
  },
  mounted() {
    this.$refs.mapSvg.addEventListener('wheel', e => e.preventDefault(), { passive: false });
  }
};
</script>

<style scoped>
.room-map-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
.room-label {
  font-family: 'Segoe UI', sans-serif;
  font-size: 14px;
  font-weight: bold;
  paint-order: stroke;
  stroke-width: 1px;
  text-shadow: 0 0 2px rgba(0, 0, 0, 0.7);
}
</style>
