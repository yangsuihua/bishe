import api from './index'

export const userAPI = {
  getUserList(params) {
    return api.get('/admin/users', { params })
  },
  getUserDetail(id) {
    return api.get(`/admin/users/${id}`)
  },
  updateUser(id, data) {
    return api.put(`/admin/users/${id}`, data)
  },
  updateUserStatus(id, status) {
    return api.post(`/admin/users/${id}/status`, null, { params: { status } })
  },
  updateUserRole(id, role) {
    return api.post(`/admin/users/${id}/role`, null, { params: { role } })
  },
  getUserVideos(id, params) {
    return api.get(`/admin/users/${id}/videos`, { params })
  }
}
