import api from './index'

export const tagAPI = {
  getTagList(params) {
    return api.get('/admin/tags', { params })
  },
  getTagDetail(id) {
    return api.get(`/admin/tags/${id}`)
  },
  createTag(data) {
    return api.post('/admin/tags', data)
  },
  updateTag(id, data) {
    return api.put(`/admin/tags/${id}`, data)
  },
  deleteTag(id) {
    return api.delete(`/admin/tags/${id}`)
  }
}
