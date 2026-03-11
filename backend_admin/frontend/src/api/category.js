import api from './index'

export const categoryAPI = {
  getCategoryTree() {
    return api.get('/admin/categories')
  },
  createCategory(data) {
    return api.post('/admin/categories', data)
  },
  updateCategory(id, data) {
    return api.put(`/admin/categories/${id}`, data)
  },
  deleteCategory(id) {
    return api.delete(`/admin/categories/${id}`)
  },
  updateCategorySort(id, sort) {
    return api.put(`/admin/categories/${id}/sort`, null, { params: { sort } })
  }
}
