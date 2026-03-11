<template>
    <div class="view-section">
      <div class="scrollable-content">
        <div class="upload-form">
          <h2 style="margin-bottom: 20px;">发布新作品</h2>
          
          <!-- 视频文件 -->
          <div class="form-group">
            <label class="form-label">视频文件</label>
            <div class="file-drop-zone" @click="selectVideo">
              <div style="font-size: 40px;">📹</div>
              <p>{{ videoFile ? videoFile.name : '点击或拖拽视频文件到此处' }}</p>
              <div style="font-size: 12px; margin-top: 5px; color: #666;">
                支持 mp4, webm, mkv
              </div>
            </div>
          </div>
  
          <!-- 封面设置 -->
          <div class="form-group">
            <label class="form-label">封面设置</label>
            <div class="cover-upload-container">
              <div class="cover-preview">
                <img v-if="coverUrl" :src="coverUrl" alt="封面">
                <img v-else-if="coverImage" :src="coverPreviewUrl" alt="封面">
                <span v-else>封面预览</span>
              </div>
              <div class="cover-actions">
                <div style="font-size: 13px; color: var(--text-secondary); margin-bottom: 8px; text-align: left;">
                  上传引人注目的封面，吸引更多观众点击。
                </div>
                <div class="cover-buttons">
                  <button class="btn-primary" @click="openVideoFrameSelector" v-if="videoUrl">🎬 从视频截取</button>
                  <button class="btn-secondary" @click="selectCover">📷 本地上传</button>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 视频帧选择模态框 -->
          <div class="modal" v-if="showFrameSelector" @click="closeFrameSelector">
            <div class="modal-content" @click.stop>
              <div class="modal-header">
                <h3>从视频中选择封面</h3>
                <button class="modal-close" @click="closeFrameSelector">×</button>
              </div>
              <div class="modal-body">
                <div class="video-frame-selector">
                  <video 
                    ref="videoPlayer"
                    :src="videoUrl" 
                    controls
                    @loadedmetadata="onVideoLoaded"
                    @timeupdate="onTimeUpdate"
                    class="video-preview"
                  ></video>
                  <div class="video-controls">
                    <input 
                      type="range" 
                      min="0" 
                      :max="videoDuration" 
                      v-model="currentVideoTime" 
                      @input="onSliderChange"
                      class="time-slider"
                      step="0.1"
                    >
                    <div class="time-display">{{ formatTime(currentVideoTime) }} / {{ formatTime(videoDuration) }}</div>
                  </div>
                  <div class="frame-preview">
                    <canvas ref="frameCanvas" class="frame-canvas" style="display:none;"></canvas>
                    <img :src="currentFrame" v-if="currentFrame" class="current-frame">
                  </div>
                  <div class="frame-actions">
                    <button class="btn-primary" @click="captureAndUploadFrame">✅ 使用此帧作为封面</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
  
          <!-- 标题 -->
          <div class="form-group">
            <label class="form-label">标题</label>
            <input 
              type="text" 
              class="form-input" 
              placeholder="给视频起个吸引人的标题 (必填)"
              v-model="formData.title"
            >
          </div>
  
          <!-- 分类 -->
          <div class="form-group">
            <label class="form-label">分类 (类型)</label>
            <select class="form-select" v-model="formData.category">
              <option value="" disabled selected>请选择视频分类</option>
              <option value="1">生活</option>
              <option value="2">娱乐</option>
              <option value="3">游戏</option>
              <option value="4">科技</option>
              <option value="5">音乐</option>
              <option value="6">美食</option>
              <option value="7">运动</option>
              <option value="8">教育</option>
            </select>
          </div>
  
          <!-- 描述 -->
          <div class="form-group">
            <label class="form-label">简介描述</label>
            <textarea 
              class="form-textarea" 
              placeholder="介绍一下你的视频内容，添加 #标签 可以增加曝光哦..."
              v-model="formData.description"
            ></textarea>
          </div>
          
          <!-- 标签选择 -->
          <div class="form-group">
            <label class="form-label">标签</label>
            <div class="tag-input-container">
              <div class="tag-input" @click="focusInput">
                <span 
                  v-for="tag in selectedTags" 
                  :key="tag.id || tag.name" 
                  class="tag-badge"
                >
                  {{ tag.name || tag }}
                  <button @click="removeTag(tag, $event)" class="tag-remove">×</button>
                </span>
                <input
                  ref="tagInput"
                  v-model="currentTagInput"
                  @input="filterTags"
                  @keydown="handleKeydown"
                  @blur="handleInputBlur"
                  @focus="showDropdown = true"
                  type="text"
                  class="tag-input-field"
                  placeholder="输入标签并按回车添加..."
                />
              </div>
              
              <!-- 自动补全下拉菜单 -->
              <div v-show="showDropdown && (filteredTags.length > 0 || currentTagInput)" class="tag-dropdown">
                <div 
                  v-for="tag in filteredTags" 
                  :key="tag.id" 
                  class="tag-option"
                  :class="{ 'selected': selectedTagIndex === $index }"
                  @click="selectTag(tag)"
                  @mouseover="selectedTagIndex = $index"
                >
                  {{ tag.name }}
                </div>
                
                <!-- 创建新标签选项 -->
                <div 
                  v-if="currentTagInput && !isExistingTag(currentTagInput)"
                  class="tag-option new-tag"
                  :class="{ 'selected': selectedTagIndex === filteredTags.length }"
                  @click="addNewTag()"
                  @mouseover="selectedTagIndex = filteredTags.length"
                >
                  添加新标签: {{ currentTagInput }}
                </div>
              </div>
            </div>
          </div>
  
          <button 
            class="btn-primary" 
            style="width: 100%; padding: 12px; margin-top: 10px;"
            @click="handleSubmit"
          >
            立即发布
          </button>
        </div>
      </div>
    </div>
  </template>
  
  <script>
  import { videoAPI } from '../api/video';
  
  export default {
    name: 'Upload',
    data() {
      return {
        videoFile: null,
        coverImage: null,
        coverPreviewUrl: null,
        videoUrl: null,
        coverUrl: null,
        uploadingVideo: false,
        uploadingCover: false,
        allTags: [], // 所有可用标签
        selectedTags: [], // 已选中的标签
        currentTagInput: '', // 当前输入的标签
        filteredTags: [], // 过滤后的标签列表
        showDropdown: false, // 是否显示下拉菜单
        selectedTagIndex: -1, // 当前选中的标签索引
        showFrameSelector: false, // 显示视频帧选择器
        videoDuration: 0, // 视频总时长
        currentVideoTime: 0, // 当前视频播放时间
        currentFrame: '', // 当前帧的图片数据（保留用于预览）
        capturedFrame: null, // 捕获的帧图片文件（保留但可能不再需要）
        formData: {
          title: '',
          category: '',
          description: ''
        }
      }
    },
    async mounted() {
      await this.loadAllTags();
    },
    methods: {
      async loadAllTags() {
        try {
          const response = await videoAPI.getVideoTags();
          if (response.data.code === 200) {
            this.allTags = response.data.data;
            this.filteredTags = this.allTags;
          }
        } catch (error) {
          console.error('加载标签失败:', error);
        }
      },
      
      focusInput() {
        this.$refs.tagInput.focus();
      },
      
      filterTags() {
        if (this.currentTagInput.trim()) {
          this.filteredTags = this.allTags.filter(tag => 
            tag.name.toLowerCase().includes(this.currentTagInput.toLowerCase()) &&
            !this.selectedTags.some(selected => 
              (selected.id && selected.id === tag.id) || 
              (selected.name === tag.name)
            )
          );
        } else {
          this.filteredTags = this.allTags.filter(tag => 
            !this.selectedTags.some(selected => 
              (selected.id && selected.id === tag.id) || 
              (selected.name === tag.name)
            )
          );
        }
        this.selectedTagIndex = -1;
        this.showDropdown = true;
      },
      
      selectTag(tag) {
        // 检查标签是否已存在
        const exists = this.selectedTags.some(selected => 
          (selected.id && selected.id === tag.id) || 
          (selected.name === tag.name)
        );
        
        if (!exists) {
          this.selectedTags.push({ ...tag });
        }
        
        this.currentTagInput = '';
        this.filterTags();
        this.showDropdown = false;
      },
      
      addNewTag() {
        if (this.currentTagInput.trim() && !this.isExistingTag(this.currentTagInput)) {
          // 添加新标签（临时的，后端会在发布时创建）
          this.selectedTags.push({ name: this.currentTagInput.trim() });
          this.currentTagInput = '';
          this.filterTags();
          this.showDropdown = false;
        }
      },
      
      isExistingTag(tagName) {
        return this.allTags.some(tag => tag.name.toLowerCase() === tagName.toLowerCase());
      },
      
      removeTag(tag, event) {
        event.stopPropagation();
        const index = this.selectedTags.findIndex(selected => 
          (tag.id && selected.id === tag.id) || 
          (tag.name === selected.name)
        );
        
        if (index > -1) {
          this.selectedTags.splice(index, 1);
        }
      },
      
      handleKeydown(event) {
        // 处理键盘事件
        if (event.key === 'Enter') {
          event.preventDefault();
          
          if (this.currentTagInput.trim()) {
            if (this.selectedTagIndex >= 0 && this.filteredTags[this.selectedTagIndex]) {
              // 选择高亮的标签
              this.selectTag(this.filteredTags[this.selectedTagIndex]);
            } else if (!this.isExistingTag(this.currentTagInput)) {
              // 添加新标签
              this.addNewTag();
            } else {
              // 选择已存在的标签
              const existingTag = this.allTags.find(tag => tag.name.toLowerCase() === this.currentTagInput.toLowerCase());
              if (existingTag) {
                this.selectTag(existingTag);
              } else {
                this.addNewTag();
              }
            }
          }
        } else if (event.key === 'Backspace' && this.currentTagInput === '' && this.selectedTags.length > 0) {
          // 删除最后一个标签
          this.selectedTags.pop();
        } else if (event.key === 'ArrowDown') {
          event.preventDefault();
          this.selectedTagIndex = Math.min(this.selectedTagIndex + 1, this.filteredTags.length);
        } else if (event.key === 'ArrowUp') {
          event.preventDefault();
          this.selectedTagIndex = Math.max(this.selectedTagIndex - 1, -1);
        }
      },
      
      handleInputBlur() {
        // 延迟隐藏下拉菜单，以便点击选项可以生效
        setTimeout(() => {
          this.showDropdown = false;
        }, 200);
      },
      
      selectVideo() {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = 'video/*';
        input.onchange = (e) => {
          this.videoFile = e.target.files[0];
          if (this.videoFile) {
            this.uploadVideo();
          }
        };
        input.click();
      },
      
      selectCover() {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = 'image/*';
        input.onchange = (e) => {
          this.coverImage = e.target.files[0];
          if (this.coverImage) {
            // 生成本地预览URL
            this.coverPreviewUrl = URL.createObjectURL(this.coverImage);
            this.uploadCover();
          }
        };
        input.click();
      },
      
      async uploadVideo() {
        if (!this.videoFile) return;
        
        this.uploadingVideo = true;
        try {
          const response = await videoAPI.uploadVideo(this.videoFile);
          if (response.data.code === 200) {
            this.videoUrl = response.data.data;
            console.log('视频上传成功:', response.data.data);
            alert('视频上传成功');
          } else {
            alert('视频上传失败: ' + response.data.message);
          }
        } catch (error) {
          console.error('视频上传失败:', error);
          alert('视频上传失败: ' + error.message);
        } finally {
          this.uploadingVideo = false;
        }
      },
      
      async uploadCover() {
        if (!this.coverImage) return;
        
        this.uploadingCover = true;
        try {
          const response = await videoAPI.uploadCover(this.coverImage);
          if (response.data.code === 200) {
            this.coverUrl = response.data.data;
            console.log('封面上传成功:', response.data.data);
            alert('封面上传成功');
            // 清除本地预览URL
            if (this.coverPreviewUrl) {
              URL.revokeObjectURL(this.coverPreviewUrl);
              this.coverPreviewUrl = null;
            }
          } else {
            alert('封面上传失败: ' + response.data.message);
          }
        } catch (error) {
          console.error('封面上传失败:', error);
          alert('封面上传失败: ' + error.message);
        } finally {
          this.uploadingCover = false;
        }
      },
      
      async handleSubmit() {
        if (!this.formData.title) {
          alert('请输入视频标题');
          return;
        }
        
        if (!this.videoUrl) {
          alert('请先上传视频文件');
          return;
        }
        
        if (!this.coverUrl) {
          alert('请先上传封面图片');
          return;
        }
        
        try {
          // 分离现有标签和新标签，以确保后端能正确处理
          const existingTagNames = this.selectedTags
            .filter(tag => tag.id)       // 有 ID 的是老标签
            .map(tag => tag.name || tag);
          
          const newTagNames = this.selectedTags
            .filter(tag => !tag.id)      // 没 ID 的是新标签
            .map(tag => tag.name || tag);  // 兼容对象或字符串格式
          
          // 将所有标签名合并成一个逗号分隔的字符串，后端会根据名称查找或创建标签
          const allTagNames = [...existingTagNames, ...newTagNames].join(',');
          
          const publishData = {
            title: this.formData.title,
            description: this.formData.description,
            coverUrl: this.coverUrl,
            videoUrl: this.videoUrl,
            categoryId: this.getCategoryId(this.formData.category),
            tags: allTagNames, // 后端会根据名称查找或创建标签
            isPrivate: 0
          };
          
          const response = await videoAPI.publishVideo(publishData);
          if (response.data.code === 200) {
            alert('视频发布成功');
            // 提交后跳转到个人主页
            this.$router.push('/profile/me');
          } else {
            alert('视频发布失败: ' + response.data.message);
          }
        } catch (error) {
          console.error('视频发布失败:', error);
          alert('视频发布失败: ' + error.message);
        }
      },
      
      getCategoryId(categoryId) {
        // 直接返回分类ID，因为现在formData.category存储的就是ID
        return parseInt(categoryId) || 1;
      },
      
      openVideoFrameSelector() {
        this.showFrameSelector = true;
      },
      
      closeFrameSelector() {
        this.showFrameSelector = false;
        this.currentFrame = '';
        this.capturedFrame = null;
        // 重置视频播放器
        if (this.$refs.videoPlayer) {
          this.$refs.videoPlayer.pause();
        }
      },
      
      onVideoLoaded() {
        this.videoDuration = this.$refs.videoPlayer.duration;
      },
      
      onTimeUpdate() {
        // 当用户拖动滑块时，我们不希望时间更新覆盖滑块位置
        // 所以这里不更新currentVideoTime
      },
      
      onSliderChange() {
        this.$refs.videoPlayer.currentTime = this.currentVideoTime;
        // 暂停视频以确保停留在指定帧
        this.$refs.videoPlayer.pause();
      },
      
      formatTime(seconds) {
        if (isNaN(seconds)) return '00:00';
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
      },
      
      async captureAndUploadFrame() {
        const video = this.$refs.videoPlayer;
        const canvas = this.$refs.frameCanvas;
        const ctx = canvas.getContext('2d');
        
        try {
          // 设置canvas尺寸为视频尺寸
          canvas.width = video.videoWidth;
          canvas.height = video.videoHeight;
          
          // 绘制当前视频帧到canvas
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
          
          // 转换为图片数据
          const frameDataUrl = canvas.toDataURL('image/jpeg', 0.9);
          
          // 更新当前帧预览
          this.currentFrame = frameDataUrl;
          
          // 将图片数据转换为File对象
          const response = await fetch(frameDataUrl);
          const blob = await response.blob();
          const frameFile = new File([blob], 'frame-cover.jpg', { type: 'image/jpeg' });
          
          // 上传帧作为封面
          this.uploadingCover = true;
          try {
            const response = await videoAPI.uploadCover(frameFile);
            if (response.data.code === 200) {
              this.coverUrl = response.data.data;
              console.log('帧封面上传成功:', response.data.data);
              alert('帧封面上传成功');
              
              // 清除之前的预览URL
              if (this.coverPreviewUrl) {
                URL.revokeObjectURL(this.coverPreviewUrl);
                this.coverPreviewUrl = null;
              }
              
              this.closeFrameSelector();
            } else {
              alert('帧封面上传失败: ' + response.data.message);
            }
          } catch (error) {
            console.error('帧封面上传失败:', error);
            alert('帧封面上传失败: ' + error.message);
          } finally {
            this.uploadingCover = false;
          }
        } catch (error) {
          console.error('捕获视频帧失败，尝试使用临时视频元素:', error);
          
          // 如果直接绘制失败，尝试使用临时视频元素
          try {
            await this.captureFrameWithTempVideo();
          } catch (tempError) {
            console.error('使用临时视频元素捕获帧也失败:', tempError);
            alert('捕获视频帧失败，请尝试重新上传视频或手动上传封面: ' + tempError.message);
          }
        }
      },
      
      // 使用临时视频元素来捕获帧，避免跨域问题
      async captureFrameWithTempVideo() {
        const tempVideo = document.createElement('video');
        tempVideo.src = this.videoUrl;
        tempVideo.crossOrigin = 'anonymous';
        
        // 等待视频元数据加载
        await new Promise((resolve) => {
          tempVideo.onloadedmetadata = () => {
            tempVideo.currentTime = this.currentVideoTime;
            tempVideo.onseeked = resolve;
          };
          tempVideo.onerror = () => {
            throw new Error('视频加载失败');
          };
        });
        
        // 创建canvas并绘制视频帧
        const canvas = this.$refs.frameCanvas;
        const ctx = canvas.getContext('2d');
        canvas.width = tempVideo.videoWidth;
        canvas.height = tempVideo.videoHeight;
        
        ctx.drawImage(tempVideo, 0, 0, canvas.width, canvas.height);
        
        try {
          const frameDataUrl = canvas.toDataURL('image/jpeg', 0.9);
          
          // 更新当前帧预览
          this.currentFrame = frameDataUrl;
          
          // 将图片数据转换为File对象
          const response = await fetch(frameDataUrl);
          const blob = await response.blob();
          const frameFile = new File([blob], 'frame-cover.jpg', { type: 'image/jpeg' });
          
          // 上传帧作为封面
          this.uploadingCover = true;
          try {
            const response = await videoAPI.uploadCover(frameFile);
            if (response.data.code === 200) {
              this.coverUrl = response.data.data;
              console.log('帧封面上传成功:', response.data.data);
              alert('帧封面上传成功');
              
              // 清除之前的预览URL
              if (this.coverPreviewUrl) {
                URL.revokeObjectURL(this.coverPreviewUrl);
                this.coverPreviewUrl = null;
              }
              
              this.closeFrameSelector();
            } else {
              alert('帧封面上传失败: ' + response.data.message);
            }
          } catch (error) {
            console.error('帧封面上传失败:', error);
            alert('帧封面上传失败: ' + error.message);
          } finally {
            this.uploadingCover = false;
          }
        } catch (error) {
          console.error('捕获视频帧失败:', error);
          alert('捕获视频帧失败: ' + error.message);
        }
      }
    },
    beforeUnmount() {
      // 组件销毁前清理预览URL
      if (this.coverPreviewUrl) {
        URL.revokeObjectURL(this.coverPreviewUrl);
      }
    }
  }
  </script>
  
  <style scoped>
  .tag-input-container {
    position: relative;
    width: 100%;
  }
  
  .tag-input {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    min-height: 40px;
    padding: 8px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    background-color: white;
    cursor: text;
  }
  
  .tag-input:focus-within {
    border-color: #3b82f6;
    outline: none;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }
  
  .tag-badge {
    display: inline-flex;
    align-items: center;
    padding: 4px 8px;
    margin: 2px;
    background-color: #dbeafe;
    color: #1d4ed8;
    border-radius: 16px;
    font-size: 14px;
  }
  
  .tag-remove {
    margin-left: 6px;
    background: none;
    border: none;
    color: #1d4ed8;
    cursor: pointer;
    font-size: 16px;
    line-height: 1;
    padding: 0 2px;
  }
  
  .tag-remove:hover {
    color: #dc2626;
  }
  
  .tag-input-field {
    flex: 1;
    border: none;
    outline: none;
    padding: 4px;
    margin: 2px;
    font-size: 14px;
    min-width: 100px;
  }
  
  .tag-dropdown {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    z-index: 10;
    background: white;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    max-height: 200px;
    overflow-y: auto;
    box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
    color: #000; /* 确保文字在深色模式下清晰可见 */
  }
  
  .tag-option {
    padding: 8px 12px;
    cursor: pointer;
    border-bottom: 1px solid #f3f4f6;
    color: #000; /* 确保文字颜色 */
  }
  
  .tag-option:last-child {
    border-bottom: none;
  }
  
  .tag-option:hover, .tag-option.selected {
    background-color: #f3f4f6;
    color: #000; /* 确保悬停时文字颜色 */
  }
  
  .new-tag {
    color: #10b981;
    font-style: italic;
  }
  
  .new-tag:hover, .new-tag.selected {
    background-color: #ecfdf5;
    color: #065f46; /* 深一点的绿色确保清晰度 */
  }
  
  /* 视频帧选择器模态框样式 */
  .modal {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
  }
  
  .modal-content {
    background-color: white;
    border-radius: 8px;
    width: 80%;
    max-width: 800px;
    max-height: 90vh;
    overflow-y: auto;
    position: relative;
  }
  
  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 24px;
    border-bottom: 1px solid #e5e7eb;
  }
  
  .modal-header h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }
  
  .modal-close {
    background: none;
    border: none;
    font-size: 24px;
    cursor: pointer;
    color: #666;
    width: 30px;
    height: 30px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  .modal-close:hover {
    color: #000;
    background-color: #f5f5f5;
    border-radius: 50%;
  }
  
  .modal-body {
    padding: 24px;
  }
  
  .video-frame-selector {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  
  .video-preview {
    width: 100%;
    max-height: 400px;
    background-color: #000;
  }
  
  .video-controls {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  
  .time-slider {
    width: 100%;
  }
  
  .time-display {
    text-align: center;
    font-size: 14px;
    color: #666;
  }
  
  .frame-preview {
    display: flex;
    justify-content: center;
    padding: 16px;
    background-color: #f9f9f9;
    border-radius: 8px;
  }
  
  .current-frame {
    max-width: 100%;
    max-height: 200px;
    border-radius: 4px;
  }
  
  .frame-actions {
    display: flex;
    justify-content: center;
    gap: 12px;
    margin-top: 16px;
  }
  

  </style>