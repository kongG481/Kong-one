<!-- src/views/teacher/Dashboard.vue -->
<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon" style="background: #ecf5ff; color: #409eff">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">156</div>
              <div class="stat-label">我的试题</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon" style="background: #f0f9eb; color: #67c23a">
              <el-icon><Star /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">23</div>
              <div class="stat-label">我的收藏</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon" style="background: #fdf6ec; color: #e6a23c">
              <el-icon><Download /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">12</div>
              <div class="stat-label">导出次数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon" style="background: #fef0f0; color: #f56c6c">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">2025-03-10</div>
              <div class="stat-label">最后登录</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最近创建的试题</span>
              <el-link type="primary">查看更多</el-link>
            </div>
          </template>
          <el-table :data="recentQuestions" style="width: 100%">
            <el-table-column prop="title" label="题目" show-overflow-tooltip />
            <el-table-column prop="type" label="题型" width="80" />
            <el-table-column prop="createTime" label="创建时间" width="100" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>学科分布</span>
            </div>
          </template>
          <div style="height: 300px; display: flex; justify-content: center; align-items: center; color: #999;">
            图表区域（可集成ECharts）
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { Document, Star, Download, Clock } from '@element-plus/icons-vue';

const recentQuestions = ref([
  { title: 'Java中final关键字的作用', type: '单选', createTime: '2025-03-09' },
  { title: '什么是面向对象的三大特性', type: '简答', createTime: '2025-03-08' },
  { title: '数据库事务的ACID特性', type: '简答', createTime: '2025-03-07' },
  { title: 'HTML中创建超链接的标签', type: '填空', createTime: '2025-03-06' }
]);
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stat-card {
  margin-bottom: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #999;
  margin-top: 4px;
}

.mt-20 {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>