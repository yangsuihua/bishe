<template>
  <div class="category-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>分类管理</span>
          <el-button type="primary" @click="showAddDialog">新增分类</el-button>
        </div>
      </template>
      
      <el-tree
        :data="categoryTree"
        :props="{ children: 'children', label: 'name' }"
        default-expand-all
        node-key="id"
      >
        <template #default="{ node, data }">
          <div class="tree-node">
            <span>{{ node.label }}</span>
            <span class="node-actions">
              <el-button size="small" link @click="showEditDialog(data)">编辑</el-button>
              <el-button size="small" link type="danger" @click="deleteCategory(data.id)">删除</el-button>
            </span>
          </div>
        </template>
      </el-tree>
    </el-card>
    
    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : (parentCategory ? '添加子分类' : '新增分类')"
      width="500px"
    >
      <el-form :model="categoryForm" :rules="categoryRules" ref="categoryFormRef" label-width="100px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <div class="icon-field">
            <!-- 中文 / 英文模糊搜索 Lucide 图标 -->
            <el-autocomplete
              v-model="iconSearch"
              :fetch-suggestions="querySearchIcon"
              placeholder="请输入中文或英文搜索图标，例如：咖啡 / music / cpu"
              @select="handleIconSelect"
              popper-class="icon-autocomplete-popper"
              style="width: 100%;"
            >
              <template #default="{ item }">
                <div class="icon-suggestion">
                  <component
                    v-if="item.value && LucideIcons[item.value]"
                    :is="LucideIcons[item.value]"
                    size="20"
                    class="icon-suggestion__icon"
                  />
                  <span class="icon-suggestion__label">{{ item.label }}</span>
                </div>
              </template>
            </el-autocomplete>

            <!-- 英文图标名称（最终提交到后端的字段） -->
            <el-input
              v-model="categoryForm.icon"
              placeholder="图标英文名称（会根据上方选择自动填充，如 Coffee / Music / Cpu）"
              style="margin-top: 8px;"
              readonly
            >
              <template #suffix>
                <component
                  v-if="categoryForm.icon && LucideIcons[categoryForm.icon]"
                  :is="LucideIcons[categoryForm.icon]"
                  size="18"
                />
              </template>
            </el-input>
          </div>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="categoryForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="categoryForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCategory">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { categoryAPI } from '@/api/category'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as LucideIcons from 'lucide-vue-next'
const categoryTree = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const parentCategory = ref(null)
const categoryFormRef = ref(null)
const categoryForm = ref({
  id: null,
  name: '',
  parentId: 0,
  icon: '',
  sort: 0,
  status: 1
})

// 图标搜索输入（中文/英文模糊）
const iconSearch = ref('')

// 所有 Lucide 图标名称
const allIconOptions = Object.keys(LucideIcons).map(name => ({
  label: name,
  value: name
}))

// 简单的中文关键字 -> 英文关键词映射，用于中文模糊匹配
const zhToKeywords = {
  '咖啡': ['coffee', 'cup', 'mug'],
  '音乐': ['music', 'audio', 'headphones', 'playlist'],
  '科技': ['cpu', 'chip', 'circuit', 'monitor', 'laptop'],
  '生活': ['home', 'house', 'sofa', 'bed'],
  '运动': ['sport', 'ball', 'trophy', 'bike', 'running'],
  '教育': ['book', 'school', 'graduation', 'graduationcap'],
  '游戏': ['game', 'controller', 'joystick', 'gamepad'],
  '美食': ['food', 'pizza', 'burger', 'utensils', 'chef'],
  '娱乐': ['clapperboard', 'tv', 'movie', 'film', 'music']
}

const hasChinese = (str) => /[\u3400-\u9FFF]/.test(str)

const categoryRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' }
  ]
}

// 图标自动补全：支持中文 -> 英文关键词映射 + 英文模糊
const querySearchIcon = (queryString, cb) => {
  if (!queryString) {
    // 无输入时返回前若干个图标，避免一次性太多
    cb(allIconOptions.slice(0, 50))
    return
  }

  const q = queryString.trim().toLowerCase()
  let keywords = [q]

  // 如果包含中文，根据映射表转成一组英文关键词
  if (hasChinese(q)) {
    const extra = []
    Object.entries(zhToKeywords).forEach(([zh, kws]) => {
      if (q.includes(zh)) {
        extra.push(...kws)
      }
    })
    if (extra.length) {
      keywords = extra
    }
  }

  const result = allIconOptions.filter(opt =>
    keywords.some(k => opt.label.toLowerCase().includes(k))
  )

  // 限制最多返回 50 个，防止列表过长
  cb(result.slice(0, 50))
}

// 选择某个图标后，把英文名称写回真正的 icon 字段
const handleIconSelect = (item) => {
  if (item && item.value) {
    categoryForm.value.icon = item.value
  }
}

// 扁平化分类列表，用于父分类选择
const flatCategories = computed(() => {
  const flatten = (arr) => {
    let result = []
    arr.forEach(item => {
      result.push({ id: item.id, name: item.name })
      if (item.children && item.children.length > 0) {
        result = result.concat(flatten(item.children))
      }
    })
    return result
  }
  return flatten(categoryTree.value)
})

const loadCategoryTree = async () => {
  try {
    const response = await categoryAPI.getCategoryTree()
    if (response.data.code === 200) {
      categoryTree.value = response.data.data || []
    }
  } catch (error) {
    ElMessage.error('加载分类列表失败')
  }
}

const showAddDialog = () => {
  isEdit.value = false
  parentCategory.value = null
  categoryForm.value = {
    id: null,
    name: '',
    parentId: 0,
    icon: '',
    sort: 0,
    status: 1
  }
  dialogVisible.value = true
}

const showAddChildDialog = (parent) => {
  isEdit.value = false
  parentCategory.value = parent
  categoryForm.value = {
    id: null,
    name: '',
    parentId: parent.id,
    icon: '',
    sort: 0,
    status: 1
  }
  dialogVisible.value = true
}

const showEditDialog = (category) => {
  isEdit.value = true
  parentCategory.value = null
  categoryForm.value = {
    id: category.id,
    name: category.name,
    parentId: category.parentId || 0,
    icon: category.icon || '',
    sort: category.sort || 0,
    status: category.status !== undefined ? category.status : 1
  }
  dialogVisible.value = true
}

const submitCategory = async () => {
  if (!categoryFormRef.value) return
  
  await categoryFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          const response = await categoryAPI.updateCategory(categoryForm.value.id, categoryForm.value)
          if (response.data.code === 200) {
            ElMessage.success('更新成功')
            dialogVisible.value = false
            loadCategoryTree()
          }
        } else {
          const response = await categoryAPI.createCategory(categoryForm.value)
          if (response.data.code === 200) {
            ElMessage.success('创建成功')
            dialogVisible.value = false
            loadCategoryTree()
          }
        }
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '操作失败')
      }
    }
  })
}

const deleteCategory = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该分类吗？如果该分类下有子分类或视频，将无法删除。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const response = await categoryAPI.deleteCategory(id)
    if (response.data.code === 200) {
      ElMessage.success('删除成功')
      loadCategoryTree()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadCategoryTree()
})
</script>

<style scoped>
.category-manage {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-right: 8px;
}

.node-actions {
  margin-left: auto;
}

.icon-field {
  display: flex;
  flex-direction: column;
}

.icon-autocomplete-popper {
  min-width: 280px;
}

.icon-suggestion {
  display: flex;
  align-items: center;
  padding: 4px 8px;
}

.icon-suggestion__icon {
  margin-right: 8px;
}

.icon-suggestion__label {
  font-size: 13px;
}
</style>
