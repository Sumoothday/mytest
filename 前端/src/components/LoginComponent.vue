<template>
  <div class="login-wrapper">
    <div class="form-container">
      <h2 class="form-title">
        🔐 用户登录
      </h2>

      <div class="form-field">
        <label for="username">用户名</label>
        <input
          id="username"
          v-model="username"
          type="text"
          placeholder="请输入用户名"
        >
      </div>

      <div class="form-field">
        <label for="password">密码</label>
        <input
          id="password"
          v-model="password"
          type="password"
          placeholder="请输入密码"
        >
      </div>

      <button
        class="form-button"
        @click="onLogin"
      >
        登录
      </button>

      <p
        v-if="errorMsg"
        class="error-text"
      >
        {{ errorMsg }}
      </p>

      <p class="switch-text">
        还没有账号？
        <span
          class="switch-link"
          @click="goToRegister"
        >
          注册
        </span>
      </p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LoginComponent',
  data() {
    return {
      username: '',
      password: '',
      errorMsg: ''
    };
  },
  methods: {
    async onLogin() {
      this.errorMsg = '';

      if (!this.username.trim()) {
        this.errorMsg = '请输入用户名';
        return;
      }
      if (!this.password) {
        this.errorMsg = '请输入密码';
        return;
      }

      console.log('尝试登录：', this.username, this.password);

      try {
        // 调用 Vuex action startGame，将用户名和密码传递给后端 /login
        const data = await this.$store.dispatch('startGame', {
          username: this.username,
          password: this.password
        });
        // 如果登录成功，data 里应该包含 sessionId、currentRoom 等信息
        console.log('登录成功，返回数据：', data);
        // 跳转到游戏主界面
        this.$router.push('/game');
      } catch (err) {
        // 如果后端返回错误或者登录失败，则捕获并提示
        console.error('登录失败：', err);
        this.errorMsg = '登录失败，请检查用户名或密码。';
      }
    },
    goToRegister() {
      this.$router.push('/register');
    }
  }
};
</script>

<style scoped>
/* ===== 外层容器，玻璃面板风格 ===== */
.login-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  /* 半透明黑蓝背景 */
  background: rgba(10, 17, 40, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  box-sizing: border-box;
}

/* 登录表单容器 */
.form-container {
  width: 320px;
  background: rgba(2, 4, 15, 0.5); /* 深色半透明 */
  border: 2px dashed rgba(0, 168, 232, 0.6); /* 虚线边框，霓虹蓝 */
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 0 20px rgba(0, 168, 232, 0.4); /* 发光效果 */
}

/* 表单标题 */
.form-title {
  font-size: 22px;
  color: #00a8e8; /* 霓虹蓝色 */
  text-align: center;
  margin-bottom: 20px;
  text-shadow: 0 0 8px rgba(0, 168, 232, 0.6);
  font-family: 'Press Start 2P', monospace;
}

/* 单个输入行 */
.form-field {
  display: flex;
  flex-direction: column;
  margin-bottom: 16px;
}

.form-field label {
  font-size: 14px;
  color: #98c1d9;
  margin-bottom: 6px;
  font-family: 'Segoe UI', sans-serif;
}

.form-field input {
  padding: 8px 12px;
  background: rgba(10, 25, 55, 0.6); /* 半透明深蓝 */
  border: 2px solid rgba(0, 52, 89, 0.8);
  border-radius: 6px;
  color: #f0f0f0;
  font-size: 14px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  font-family: 'Courier New', monospace;
}

.form-field input::placeholder {
  color: #4a5875;
}

.form-field input:focus {
  outline: none;
  border-color: rgba(0, 168, 232, 0.8);
  box-shadow: 0 0 10px rgba(0, 168, 232, 0.4);
}

/* 登录按钮 */
.form-button {
  width: 100%;
  padding: 10px 0;
  background: linear-gradient(to bottom, rgba(0, 126, 167, 0.9), rgba(0, 52, 89, 0.9));
  border: 2px solid rgba(0, 23, 31, 0.8);
  border-radius: 8px;
  color: #ffffff;
  font-size: 16px;
  cursor: pointer;
  font-family: 'Segoe UI', sans-serif;
  box-shadow: 0 0 10px rgba(0, 168, 232, 0.4);
  transition: background 0.2s ease, box-shadow 0.2s ease, transform 0.1s ease;
  margin-bottom: 12px;
}

.form-button:hover {
  background: linear-gradient(to bottom, rgba(0, 168, 232, 0.9), rgba(0, 52, 89, 0.9));
  box-shadow: 0 0 15px rgba(0, 168, 232, 0.6);
}

.form-button:active {
  transform: translateY(2px);
  box-shadow: 0 0 8px rgba(0, 168, 232, 0.4);
}

/* 错误提示文本 */
.error-text {
  color: #ff6b6b;
  font-size: 13px;
  text-align: center;
  margin-bottom: 12px;
  font-family: 'Segoe UI', sans-serif;
}

/* “切换注册” 文本 */
.switch-text {
  margin-top: 14px;
  text-align: center;
  font-size: 13px;
  color: #98c1d9;
  font-family: 'Segoe UI', sans-serif;
}

.switch-link {
  color: #00a8e8;
  cursor: pointer;
  text-decoration: underline;
  margin-left: 4px;
  transition: color 0.2s ease;
}

.switch-link:hover {
  color: #4af0ff;
}
</style>
