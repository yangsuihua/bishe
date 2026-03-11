<template>
  <div class="user-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <div>
            <el-input
              v-model="query.keyword"
              placeholder="搜索用户名、昵称或邮箱"
              style="width: 200px; margin-right: 10px"
              clearable
              @keyup.enter="loadUserList"
            />
            <el-select v-model="query.status" placeholder="状态" style="width: 120px; margin-right: 10px" clearable>
              <el-option label="正常" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
            <el-select v-model="query.role" placeholder="角色" style="width: 120px; margin-right: 10px" clearable>
              <el-option label="普通用户" value="user" />
              <el-option label="管理员" value="admin" />
            </el-select>
            <el-button type="primary" @click="loadUserList">搜索</el-button>
          </div>
        </div>
      </template>
      
      <el-table :data="userList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="头像" width="80">
          <template #default="{ row }">
            <el-avatar :src="row.avatar" />
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'warning' : ''">
              {{ row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewUserVideos(row.id)">视频</el-button>
            <el-button size="small" @click="toggleUserStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" @click="changeUserRole(row)">
              {{ row.role === 'admin' ? '移除管理员' : '设为管理员' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadUserList"
          @current-change="loadUserList"
        />
      </div>
    </el-card>
    
    <!-- 用户视频对话框 -->
    <el-dialog v-model="videosDialogVisible" title="用户视频" width="800px">
      <el-table :data="userVideos" v-loading="videosLoading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination" style="margin-top: 20px">
        <el-pagination
          v-model:current-page="videosPage"
          v-model:page-size="videosSize"
          :total="videosTotal"
          layout="total, prev, pager, next"
          @current-change="loadUserVideos"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { userAPI } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const userList = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const query = ref({
  keyword: '',
  status: null,
  role: null
})

const videosDialogVisible = ref(false)
const videosLoading = ref(false)
const userVideos = ref([])
const videosPage = ref(1)
const videosSize = ref(10)
const videosTotal = ref(0)
const currentUserId = ref(null)

const loadUserList = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: size.value,
      ...query.value
    }
    const response = await userAPI.getUserList(params)
    if (response.data.code === 200) {
      const data = response.data.data
      userList.value = data.records || []
      total.value = data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const toggleUserStatus = async (user) => {
  try {
    const action = user.status === 1 ? '禁用' : '启用'
    await ElMessageBox.confirm(`确定要${action}该用户吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const newStatus = user.status === 1 ? 0 : 1
    const response = await userAPI.updateUserStatus(user.id, newStatus)
    if (response.data.code === 200) {
      ElMessage.success(`${action}成功`)
      loadUserList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '操作失败')
    }
  }
}

const changeUserRole = async (user) => {
  try {
    const action = user.role === 'admin' ? '移除管理员权限' : '设为管理员'
    await ElMessageBox.confirm(`确定要${action}吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const newRole = user.role === 'admin' ? 'user' : 'admin'
    const response = await userAPI.updateUserRole(user.id, newRole)
    if (response.data.code === 200) {
      ElMessage.success('角色更新成功')
      loadUserList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '操作失败')
    }
  }
}

const viewUserVideos = (userId) => {
  currentUserId.value = userId
  videosPage.value = 1
  videosDialogVisible.value = true
  loadUserVideos()
}

const loadUserVideos = async () => {
  if (!currentUserId.value) return
  
  videosLoading.value = true
  try {
    const response = await userAPI.getUserVideos(currentUserId.value, {
      page: videosPage.value,
      size: videosSize.value
    })
    if (response.data.code === 200) {
      const data = response.data.data
      userVideos.value = data.records || []
      videosTotal.value = data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载用户视频失败')
  } finally {
    videosLoading.value = false
  }
}

const getStatusText = (status) => {
  const statusMap = {
    0: '待审核',
    1: '已审核',
    2: '审核失败'
  }
  return statusMap[status] || '未知'
}

const getStatusType = (status) => {
  const typeMap = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return typeMap[status] || ''
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadUserList()
})
</script>

<style scoped>
.user-manage {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
