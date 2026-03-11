import api from './index'

export const authAPI = {
  login(data) {
    return api.post('/auth/login', data)
  }
}
