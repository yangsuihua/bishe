<template>
  <div class="register-container">
    <div class="register-form">
      <h2>用户注册</h2>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label for="reg-username">用户名</label>
          <input 
            type="text" 
            id="reg-username" 
            v-model="registerForm.username" 
            placeholder="请输入用户名"
            required
          >
        </div>
        <div class="form-group">
          <label for="reg-email">邮箱</label>
          <input 
            type="email" 
            id="reg-email" 
            v-model="registerForm.email" 
            placeholder="请输入邮箱"
            required
          >
        </div>
        <div class="form-group">
          <label for="reg-password">密码</label>
          <input 
            type="password" 
            id="reg-password" 
            v-model="registerForm.password" 
            placeholder="请输入密码"
            required
          >
        </div>
        <div class="form-group">
          <label for="reg-confirm-password">确认密码</label>
          <input 
            type="password" 
            id="reg-confirm-password" 
            v-model="registerForm.confirmPassword" 
            placeholder="请再次输入密码"
            required
          >
        </div>
        <button type="submit" class="register-btn" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>
      <p class="login-link">
        已有账号？<router-link to="/login">立即登录</router-link>
      </p>
    </div>
  </div>
</template>

<script>
import { mapMutations, mapActions } from 'vuex'
import authAPI from '../api/auth'

export default {
  name: 'Register',
  data() {
    return {
      registerForm: {
        username: '',
        email: '',
        password: '',
        confirmPassword: ''
      },
      loading: false
    }
  },
  methods: {
    ...mapMutations(['CLOSE_DROPDOWN']),
    ...mapActions(['login']),
    async handleRegister() {
      // 验证密码是否一致
      if (this.registerForm.password !== this.registerForm.confirmPassword) {
        alert('两次输入的密码不一致');
        return;
      }
      
      // 验证邮箱格式
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(this.registerForm.email)) {
        alert('请输入有效的邮箱地址');
        return;
      }
      
      // 验证用户名和密码长度
      if (this.registerForm.username.length < 3) {
        alert('用户名至少需要3个字符');
        return;
      }
      
      if (this.registerForm.password.length < 6) {
        alert('密码至少需要6个字符');
        return;
      }
      
      this.loading = true;
      
      try {
        const { confirmPassword, ...userData } = this.registerForm;
        const response = await authAPI.register(userData);
        
        if (response.data.code === 200 || response.data.success) { // 根据后端实际返回格式调整
          const data = response.data.data || response.data;
          
          // 注册成功，自动登录，保存token和用户信息到store
          this.login({ 
            token: data.accessToken || data.token, 
            user: { 
              username: this.registerForm.username,
              id: data.userId || data.id || 'me' // 如果后端返回userId则使用，否则使用默认值
            } 
          });
          
          // 关闭下拉菜单
          this.CLOSE_DROPDOWN();
          
          // 跳转到首页
          this.$router.push('/');
          
          // 显示成功消息
          alert('注册成功');
        } else {
          alert(response.data.message || response.data.msg || '注册失败');
        }
      } catch (error) {
        console.error('注册错误:', error);
        
        // 检查是否有响应数据
        if (error.response) {
          alert(error.response.data.message || error.response.data.msg || '注册失败，请检查输入信息');
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
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  padding: 20px;
}

.register-form {
  background: white;
  padding: 40px;
  border-radius: 10px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

.register-form h2 {
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
  border-color: #f093fb;
  box-shadow: 0 0 5px rgba(240, 147, 251, 0.3);
}

.register-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  border: none;
  border-radius: 5px;
  font-size: 16px;
  cursor: pointer;
  transition: opacity 0.3s;
}

.register-btn:hover {
  opacity: 0.9;
}

.login-link {
  text-align: center;
  margin-top: 20px;
  color: #666;
}

.login-link a {
  color: #f093fb;
  text-decoration: none;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>