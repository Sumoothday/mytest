<template>
  <div class="register-wrapper">
    <div class="form-container">
      <h2 class="form-title">
        📝 用户注册
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
        <label for="email">邮箱</label>
        <input
          id="email"
          v-model="email"
          type="email"
          placeholder="请输入邮箱"
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

      <div class="form-field">
        <label for="confirmPassword">确认密码</label>
        <input
          id="confirmPassword"
          v-model="confirmPassword"
          type="password"
          placeholder="再次输入密码"
        >
      </div>

      <button
        class="form-button"
        @click="onRegister"
      >
        注册
      </button>

      <p class="switch-text">
        已有账号？
        <span
          class="switch-link"
          @click="goToLogin"
        >
          去登录
        </span>
      </p>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'RegisterComponent',
  data() {
    return {
      username: '',
      email: '',
      password: '',
      confirmPassword: ''
    };
  },
  methods: {
    async onRegister() {
      // 前端简单校验
      if (!this.username.trim() || !this.email.trim()) {
        alert('用户名和邮箱不能为空');
        return;
      }
      if (this.password.length < 6) {
        alert('密码长度至少 6 位');
        return;
      }
      if (this.password !== this.confirmPassword) {
        alert('两次密码不一致');
        return;
      }

      // 构造注册请求体
      const payload = {
        username: this.username.trim(),
       // email: this.email.trim(),
        password: this.password
      };

      try {
        // 调用后端注册接口，并显式带上 Content-Type: application/json
        const response = await axios.post(
          'http://localhost:8080/api/register',
          payload,
          {
            headers: {
              'Content-Type': 'application/json'
            }
          }
        );

        // 根据后端返回的状态码或业务字段判断是否注册成功
        if (response.status === 201 || response.data.success) {
          alert('注册成功，即将跳转到登录页面');
          this.$router.push('/login');
        } else {
          const msg = response.data.message || '注册失败，请重试';
          alert(msg);
        }
      } catch (error) {
        // 捕获网络错误或后端返回的 4xx/5xx
        if (error.response) {
          const errMsg = error.response.data.message || `注册失败，状态码：${error.response.status}`;
          alert(errMsg);
        } else {
          alert('无法连接到服务器，请稍后再试');
        }
        console.error('注册接口错误：', error);
      }
    },
    goToLogin() {
      this.$router.push('/login');
    }
  }
};
</script>

<style scoped>
/* ===== 外层容器，玻璃面板风格 ===== */
.register-wrapper {
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

/* 注册表单容器 */
.form-container {
  width: 360px;
  background: rgba(2, 4, 15, 0.5); /* 深色半透明 */
  border: 2px dashed rgba(0, 168, 232, 0.6); /* 虚线边框 */
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

/* 注册按钮 */
.form-button {
  width: 100%;
  padding: 10px 0;
  background: linear-gradient(
    to bottom,
    rgba(0, 126, 167, 0.9),
    rgba(0, 52, 89, 0.9)
  );
  border: 2px solid rgba(0, 23, 31, 0.8);
  border-radius: 8px;
  color: #ffffff;
  font-size: 16px;
  cursor: pointer;
  font-family: 'Segoe UI', sans-serif;
  box-shadow: 0 0 10px rgba(0, 168, 232, 0.4);
  transition: background 0.2s ease, box-shadow 0.2s ease, transform 0.1s ease;
}

.form-button:hover {
  background: linear-gradient(
    to bottom,
    rgba(0, 168, 232, 0.9),
    rgba(0, 52, 89, 0.9)
  );
  box-shadow: 0 0 15px rgba(0, 168, 232, 0.6);
}

.form-button:active {
  transform: translateY(2px);
  box-shadow: 0 0 8px rgba(0, 168, 232, 0.4);
}

/* “切换登录” 文本 */
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
