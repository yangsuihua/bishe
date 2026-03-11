<template>
  <div class="login-container">
    <div class="login-form">
      <h2>用户登录</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">用户名</label>
          <input 
            type="text" 
            id="username" 
            v-model="loginForm.username" 
            placeholder="请输入用户名"
            required
          >
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input 
            type="password" 
            id="password" 
            v-model="loginForm.password" 
            placeholder="请输入密码"
            required
          >
        </div>
        <button type="submit" class="login-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
      <p class="register-link">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<script>
import { mapMutations, mapActions } from 'vuex'
import authAPI from '../api/auth'

export default {
  name: 'Login',
  data() {
    return {
      loginForm: {
        username: '',
        password: ''
      },
      loading: false
    }
  },
  methods: {
    ...mapMutations(['CLOSE_DROPDOWN']),
    ...mapActions(['login']),
    async handleLogin() {
      if (!this.loginForm.username || !this.loginForm.password) {
        alert('请输入用户名和密码');
        return;
      }
      
      this.loading = true;
      
      try {
        const response = await authAPI.login(this.loginForm);
        console.log('登录响应:', response);
        if (response.data.code === 200 || response.data.success) { // 根据后端实际返回格式调整
          const data = response.data.data || response.data;
          
          // 登录成功，保存token和用户信息到store
          this.login({ 
            token: data.accessToken || data.token, 
            user: { 
              username: this.loginForm.username,
              id: data.userId || data.id || 'me' // 如果后端返回userId则使用，否则使用默认值
            } 
          });
          
          // 关闭下拉菜单
          this.CLOSE_DROPDOWN();
          
          // 跳转到首页
          this.$router.push('/');
          
          // 显示成功消息
          alert('登录成功');
        } else {
          alert(response.data.message || response.data.msg || '登录失败');
        }
      } catch (error) {
        console.error('登录错误:', error);
        
        // 检查是否有响应数据
        if (error.response) {
          alert(error.response.data.message || error.response.data.msg || '登录失败，请检查用户名和密码');
        } else {
          alert('网络错误，请稍后重试');
        }
      } finally {
        this.loading = false;
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-form {
  background: white;
  padding: 40px;
  border-radius: 10px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

.login-form h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  color: #555;
  font-weight: bold;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 16px;
  box-sizing: border-box;
  color: #333; /* 添加文本颜色 */
  background-color: #fff; /* 确保背景色为白色 */
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 5px rgba(102, 126, 234, 0.3);
}

.login-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 5px;
  font-size: 16px;
  cursor: pointer;
  transition: opacity 0.3s;
}

.login-btn:hover {
  opacity: 0.9;
}

.register-link {
  text-align: center;
  margin-top: 20px;
  color: #666;
}

.register-link a {
  color: #667eea;
  text-decoration: none;
}

.register-link a:hover {
  text-decoration: underline;
}
</style>