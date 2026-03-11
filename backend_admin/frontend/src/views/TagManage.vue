<template>
  <div class="tag-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>标签管理</span>
          <el-button type="primary" @click="showAddDialog">新增标签</el-button>
        </div>
      </template>
      
      <el-table :data="tagList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="标签名称" />
        <el-table-column prop="videoCount" label="关联视频数" width="120" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteTag(row.id)">删除</el-button>
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
          @size-change="loadTagList"
          @current-change="loadTagList"
        />
      </div>
    </el-card>
    
    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑标签' : '新增标签'"
      width="500px"
    >
      <el-form :model="tagForm" :rules="tagRules" ref="tagFormRef" label-width="100px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="tagForm.name" placeholder="请输入标签名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTag">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { tagAPI } from '@/api/tag'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tagList = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const isEdit = ref(false)
const tagFormRef = ref(null)
const tagForm = ref({
  id: null,
  name: ''
})

const tagRules = {
  name: [
    { required: true, message: '请输入标签名称', trigger: 'blur' }
  ]
}

const loadTagList = async () => {
  loading.value = true
  try {
    const response = await tagAPI.getTagList({ page: page.value, size: size.value })
    if (response.data.code === 200) {
      const data = response.data.data
      tagList.value = data.records || []
      total.value = data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载标签列表失败')
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  isEdit.value = false
  tagForm.value = { id: null, name: '' }
  dialogVisible.value = true
}

const showEditDialog = (tag) => {
  isEdit.value = true
  tagForm.value = { id: tag.id, name: tag.name }
  dialogVisible.value = true
}

const submitTag = async () => {
  if (!tagFormRef.value) return
  
  await tagFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          const response = await tagAPI.updateTag(tagForm.value.id, { name: tagForm.value.name })
          if (response.data.code === 200) {
            ElMessage.success('更新成功')
            dialogVisible.value = false
            loadTagList()
          }
        } else {
          const response = await tagAPI.createTag({ name: tagForm.value.name })
          if (response.data.code === 200) {
            ElMessage.success('创建成功')
            dialogVisible.value = false
            loadTagList()
          }
        }
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '操作失败')
      }
    }
  })
}

const deleteTag = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该标签吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const response = await tagAPI.deleteTag(id)
    if (response.data.code === 200) {
      ElMessage.success('删除成功')
      loadTagList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadTagList()
})
</script>

<style scoped>
.tag-manage {
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
