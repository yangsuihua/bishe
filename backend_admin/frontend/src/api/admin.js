import api from './index'

export const adminAPI = {
  getStats() {
    return api.get('/admin/stats')
  }
}
