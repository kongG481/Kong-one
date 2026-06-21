<!-- src/views/teacher/Questions.vue -->
<template>
  <div class="questions-container">
    <!-- 顶部操作栏 -->
    <div class="operation-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增试题
      </el-button>
      <el-button type="success" @click="handleBatchImport">
        <el-icon><Upload /></el-icon>批量导入
      </el-button>
      <!-- 重复检测按钮 - 使用组件 -->
      <el-button type="warning" @click="duplicateDialogVisible = true">
        <el-icon><Warning /></el-icon>
        重复检测
      </el-button>
      <!-- 统计分析按钮 - 使用组件 -->
      <el-button type="success" @click="statisticsDialogVisible = true">
        <el-icon><DataAnalysis /></el-icon>
        统计分析
      </el-button>
      <el-button @click="handleDownloadTemplate">
        <el-icon><Download /></el-icon>下载模板
      </el-button>
      <el-button type="warning" @click="showDrafts = true" v-if="draftCount > 0">
        <el-icon><Document /></el-icon>暂存箱({{ draftCount }})
      </el-button>
      <el-button type="info" @click="showRecycleBin = true" v-if="userRole === '1'">
        <el-icon><Delete /></el-icon>回收站
      </el-button>

      <!-- ========== 新增：查询和导出按钮 ========== -->
      <el-button type="primary" plain @click="showAdvancedQuery = true">
        <el-icon><Search /></el-icon>高级查询
      </el-button>
      <el-button type="success" plain @click="showExportDialog = true">
        <el-icon><Download /></el-icon>导出
      </el-button>
      <el-button type="info" plain @click="showExportHistoryDialog = true">
        <el-icon><Document /></el-icon>导出历史
      </el-button>
      <div class="search-bar">
        <el-input
            v-model="keyword"
            placeholder="搜索题目内容"
            clearable
            @keyup.enter="fetchQuestionList"
        >
          <template #append>
            <el-button @click="fetchQuestionList">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>
    </div>

    <!-- 试题列表 -->
    <el-card class="question-list">
      <template #header>
        <div class="card-header">
          <span>试题列表</span>
          <div class="header-right">
            <el-radio-group v-model="questionType" size="small" @change="fetchQuestionList">
              <el-radio-button :value="0">全部</el-radio-button>
              <el-radio-button :value="1">单选题</el-radio-button>
              <el-radio-button :value="2">多选题</el-radio-button>
              <el-radio-button :value="3">填空题</el-radio-button>
              <el-radio-button :value="4">简答题</el-radio-button>
            </el-radio-group>
            <el-checkbox v-model="showBatch" @change="handleBatchChange">批量操作</el-checkbox>
          </div>
        </div>
      </template>

      <el-table
          :data="questionList"
          stripe
          style="width: 100%"
          v-loading="loading"
          @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" v-if="showBatch" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="题型" width="100">
          <template #default="{ row }">
            <el-tag :type="getQuestionTypeTag(row.questionType)">
              {{ getQuestionTypeName(row.questionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="题目内容" show-overflow-tooltip min-width="200">
          <template #default="{ row }">
            <span v-html="highlightContent(row.content)"></span>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="120">
          <template #default="{ row }">
            <el-rate v-model="row.difficulty" :colors="difficultyColors" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="subjectName" label="学科" width="120" />
        <el-table-column prop="creatorName" label="创建人" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" link @click="handleView(row)">预览</el-button>
            <el-button type="info" link @click="handleHistory(row)">历史</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            <!-- 单题重复检测 - 打开重复检测组件并自动填入ID -->
            <el-button type="warning" link @click="checkSingleDuplicate(row.id)">
              重复检测
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 批量操作栏 -->
      <div v-if="showBatch && selectedIds.length > 0" class="batch-bar">
        <span>已选择 {{ selectedIds.length }} 条试题</span>
        <el-select v-model="batchDifficulty" placeholder="批量修改难度" clearable size="small">
          <el-option :value="1" label="简单" />
          <el-option :value="2" label="中等" />
          <el-option :value="3" label="困难" />
        </el-select>
        <el-button type="primary" size="small" @click="handleBatchUpdateDifficulty">应用</el-button>

        <el-cascader
            v-model="batchSubject"
            :options="subjectOptions"
            :props="{ value: 'id', label: 'name', checkStrictly: true, emitPath: false }"
            placeholder="批量修改学科"
            clearable
            size="small"
            style="width: 200px; margin-left: 10px;"
        />
        <el-button type="primary" size="small" @click="handleBatchUpdateSubject">应用</el-button>
      </div>

      <!-- 分页 -->
      <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
      />
    </el-card>

    <!-- 试题入库/编辑对话框 -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="800px"
        @close="handleDialogClose"
    >
      <el-form
          ref="formRef"
          :model="questionForm"
          :rules="formRules"
          label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="题型" prop="questionType">
              <el-select
                  v-model="questionForm.questionType"
                  placeholder="请选择题型"
                  @change="handleQuestionTypeChange"
                  :disabled="!!questionForm.id"
              >
                <el-option :value="1" label="单选题" />
                <el-option :value="2" label="多选题" />
                <el-option :value="3" label="填空题" />
                <el-option :value="4" label="简答题" />
              </el-select>
              <div v-if="questionForm.id" class="type-tip">
                <el-tag size="small" type="info">题型不可修改</el-tag>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属学科" prop="subjectId">
              <el-cascader
                  v-model="questionForm.subjectId"
                  :options="subjectOptions"
                  :props="{ value: 'id', label: 'name', checkStrictly: true, emitPath: false }"
                  placeholder="请选择学科"
                  clearable
                  style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="难度等级" prop="difficulty">
              <el-rate v-model="questionForm.difficulty" :colors="difficultyColors" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="建议用时" prop="suggestedTime">
              <el-input-number v-model="questionForm.suggestedTime" :min="0" :max="300" />
              <span class="unit">分钟</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="题目来源" prop="source">
          <el-input v-model="questionForm.source" placeholder="请输入题目来源" />
        </el-form-item>

        <el-form-item label="题目内容" prop="content">
          <el-input
              v-model="questionForm.content"
              type="textarea"
              :rows="4"
              placeholder="请输入题目内容"
              @blur="checkDuplicate"
          />
          <div v-if="duplicateWarning" class="duplicate-warning">
            <el-alert type="warning" :closable="false" show-icon>
              检测到相似题目，请确认是否重复添加
            </el-alert>
          </div>
        </el-form-item>

        <!-- 选择题选项 -->
        <div v-if="isChoiceQuestion">
          <el-divider>选项设置</el-divider>
          <div v-for="(choice, index) in questionForm.choices" :key="index" class="choice-item">
            <el-row :gutter="10" align="middle">
              <el-col :span="2">
                <span class="choice-label">{{ choice.optionLabel }}</span>
              </el-col>
              <el-col :span="16">
                <el-input v-model="choice.optionContent" placeholder="选项内容" />
              </el-col>
              <el-col :span="4">
                <el-checkbox v-model="choice.isCorrect" :true-value="1" :false-value="0">
                  正确答案
                </el-checkbox>
              </el-col>
              <el-col :span="2">
                <el-button type="danger" :icon="Delete" circle size="small" @click="removeChoice(index)" />
              </el-col>
            </el-row>
          </div>
          <el-button type="primary" link @click="addChoice">
            <el-icon><Plus /></el-icon>添加选项
          </el-button>
        </div>

        <!-- 填空题/简答题答案 -->
        <div v-if="!isChoiceQuestion && questionForm.questionType">
          <el-divider>答案设置</el-divider>
          <div v-for="(answer, index) in questionForm.answers" :key="index" class="answer-item">
            <el-row :gutter="10" align="top">
              <el-col :span="2">
                <span class="answer-label">答案{{ index + 1 }}</span>
              </el-col>
              <el-col :span="16">
                <el-input
                    v-model="answer.answerText"
                    type="textarea"
                    :rows="questionForm.questionType === 3 ? 2 : 3"
                    placeholder="答案内容"
                />
              </el-col>
              <el-col :span="4">
                <div v-if="questionForm.questionType === 4">
                  <el-input-number v-model="answer.keyPointScore" :min="0" :max="100" size="small" />
                  <span class="unit">分</span>
                </div>
                <el-checkbox v-else v-model="answer.isCorrect" :true-value="1" :false-value="0">
                  正确答案
                </el-checkbox>
              </el-col>
              <el-col :span="2">
                <el-button type="danger" :icon="Delete" circle size="small" @click="removeAnswer(index)" />
              </el-col>
            </el-row>
          </div>
          <el-button type="primary" link @click="addAnswer">
            <el-icon><Plus /></el-icon>添加答案
          </el-button>
        </div>

        <el-divider />
        <el-form-item label="试题解析" prop="analysis">
          <el-input
              v-model="questionForm.analysis"
              type="textarea"
              :rows="3"
              placeholder="请输入试题解析"
          />
        </el-form-item>

        <el-form-item label="修改说明" prop="changeNote" v-if="questionForm.id">
          <el-input
              v-model="questionForm.changeNote"
              type="textarea"
              :rows="2"
              placeholder="请输入修改说明（可选）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="info" @click="saveAsDraft" v-if="!questionForm.id">暂存</el-button>
        <el-button type="primary" @click="submitForm">{{ questionForm.id ? '保存修改' : '保存并入库' }}</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="batchDialogVisible" title="批量导入试题" width="500px">
      <el-upload
          class="upload-demo"
          drag
          :http-request="handleUpload"
          :before-upload="beforeUpload"
          accept=".csv"
          :show-file-list="false"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          拖拽文件到此处或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            只能上传CSV文件，请先下载模板填写后再上传
          </div>
        </template>
      </el-upload>
      <div v-if="importResult" class="import-result">
        <el-alert
            :title="`导入完成：成功 ${importResult.success} 条，失败 ${importResult.fail} 条`"
            :type="importResult.fail > 0 ? 'warning' : 'success'"
            :closable="false"
        />
        <div v-if="importResult.errorMessages && importResult.errorMessages.length" class="error-list">
          <p v-for="(msg, idx) in importResult.errorMessages" :key="idx" class="error-item">
            {{ msg }}
          </p>
        </div>
      </div>
      <template #footer>
        <el-button @click="batchDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 使用重复检测组件 -->
    <DuplicateCheckDialog
        v-model="duplicateDialogVisible"
        :default-question-id="pendingCheckQuestionId"
        @success="loadQuestions"
    />

    <!-- 使用统计分析组件 -->
    <StatisticsDialog v-model="statisticsDialogVisible" />


    <!-- 暂存箱对话框 -->
    <el-dialog v-model="showDrafts" title="暂存箱" width="700px">
      <el-table :data="draftList" stripe v-loading="draftLoading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="题型" width="100">
          <template #default="{ row }">
            {{ getQuestionTypeName(row.questionType) }}
          </template>
        </el-table-column>
        <el-table-column prop="content" label="题目内容" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="最后编辑" width="160" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" link @click="continueEdit(row)">继续编辑</el-button>
            <el-button type="danger" link @click="handleDeleteDraft(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 回收站对话框 -->
    <RecycleBin
        v-model="showRecycleBin"
        @restore="handleRestoreComplete"
        @delete="handlePermanentDeleteComplete"
    />

    <!-- 试题预览对话框 -->
    <el-dialog v-model="previewVisible" title="试题预览" width="600px">
      <div class="preview-content">
        <div class="preview-header">
          <el-tag :type="getQuestionTypeTag(previewData.questionType)">
            {{ getQuestionTypeName(previewData.questionType) }}
          </el-tag>
          <el-rate v-model="previewData.difficulty" :colors="difficultyColors" disabled />
        </div>
        <div class="preview-question">
          <h4>题目：</h4>
          <div v-html="previewData.content"></div>
        </div>
        <div v-if="previewData.choices && previewData.choices.length" class="preview-choices">
          <h4>选项：</h4>
          <div v-for="choice in previewData.choices" :key="choice.id" class="choice-item">
            <span class="choice-label">{{ choice.optionLabel }}.</span>
            <span>{{ choice.optionContent }}</span>
            <el-tag v-if="choice.isCorrect" size="small" type="success" class="correct-tag">正确</el-tag>
          </div>
        </div>
        <div v-if="previewData.answers && previewData.answers.length" class="preview-answers">
          <h4>答案：</h4>
          <div v-for="(answer, index) in previewData.answers" :key="index" class="answer-item">
            <div v-if="previewData.questionType === 4">
              <span class="answer-point">要点{{ index + 1 }}：</span>
              <span>{{ answer.answerText }}</span>
              <span class="answer-score">({{ answer.keyPointScore }}分)</span>
            </div>
            <div v-else>
              <span>答案{{ index + 1 }}：{{ answer.answerText }}</span>
            </div>
          </div>
        </div>
        <div v-if="previewData.analysis" class="preview-analysis">
          <h4>解析：</h4>
          <div>{{ previewData.analysis }}</div>
        </div>
      </div>
    </el-dialog>

    <!-- 历史版本对话框 -->
    <HistoryDialog
        v-model="historyVisible"
        :question-id="currentQuestionId"
        :history-list="historyList"
        @rollback="handleRollbackToVersion"
    />

    <!-- ========== 新增：高级查询对话框 ========== -->
    <AdvancedQueryDialog
        v-model="showAdvancedQuery"
        @search="handleAdvancedSearch"
    />

    <!-- ========== 新增：导出对话框 ========== -->
    <ExportDialog
        v-model="showExportDialog"
        :selected-ids="selectedIds"
        @success="handleExportSuccess"
    />

    <!-- ========== 新增：导出历史对话框 ========== -->
    <ExportHistoryDialog
        v-model="showExportHistoryDialog"
        :is-admin="isAdmin"
    />

    <!-- 版本对比对话框 -->
    <el-dialog v-model="compareVisible" title="版本对比" width="800px">
      <el-tabs>
        <el-tab-pane label="题目内容">
          <div class="compare-content">
            <div class="compare-left">
              <h4>版本1</h4>
              <div v-html="compareData.content1"></div>
            </div>
            <div class="compare-right">
              <h4>版本2</h4>
              <div v-html="compareData.content2"></div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="试题解析">
          <div class="compare-content">
            <div class="compare-left">{{ compareData.analysis1 }}</div>
            <div class="compare-right">{{ compareData.analysis2 }}</div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="难度">
          <div class="compare-content">
            <div class="compare-left">
              <el-rate v-model="compareData.difficulty1" :colors="difficultyColors" disabled />
            </div>
            <div class="compare-right">
              <el-rate v-model="compareData.difficulty2" :colors="difficultyColors" disabled />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed ,watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import HistoryDialog from '@/views/question/HistoryDialog.vue'
import RecycleBin from '@/views/question/RecycleBin.vue'
import {
  Plus, Upload, UploadFilled, Delete, Warning, DataAnalysis,
  RefreshRight, Refresh, Download, Document, List, Grid,
  Edit, ChatLineSquare, WarningFilled
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
// ========== 新增：导入查询和导出组件 ==========
import AdvancedQueryDialog from '@/components/AdvancedQueryDialog.vue'
import ExportDialog from '@/components/ExportDialog.vue'
import ExportHistoryDialog from '@/components/ExportHistoryDialog.vue'

// ========== 新增：重复判断和统计分析组件 ==========
import DuplicateCheckDialog from '@/components/DuplicateCheckDialog.vue'
import StatisticsDialog from '@/components/StatisticsDialog.vue'
// API 导入 - 全部重命名避免冲突
import {
  getQuestionList,
  getQuestionDetail,
  insertQuestion,
  updateQuestion,
  deleteQuestion as deleteQuestionApi,        // 重命名
  checkDuplicate as checkDuplicateApi,        // 重命名
  getDrafts as getDraftsApi,                   // 重命名
  getDraftDetail as getDraftDetailApi,         // 重命名
  deleteDraft as deleteDraftApi,               // 重命名
  batchImport as batchImportApi,                // 重命名
  downloadTemplate as downloadTemplateApi,
  getSubjectTree as getSubjectTreeApi,
  getQuestionHistory as getQuestionHistoryApi,  // 重命名
  compareHistoryVersions as compareHistoryVersionsApi, // 重命名
  rollbackToVersion as rollbackToVersionApi,    // 重命名
  batchUpdateDifficulty as batchUpdateDifficultyApi, // 重命名
  batchUpdateSubject as batchUpdateSubjectApi,  // 重命名
  getRecycleBin as getRecycleBinApi,            // 重命名
  restoreQuestion as restoreQuestionApi,        // 重命名
  permanentDelete as permanentDeleteApi,         // 重命名

  // ========== 新增：导入高级查询和导出API ==========
  advancedQuery as advancedQueryApi,
  exportQuestions as exportQuestionsApi,
  getExportHistory as getExportHistoryApi,
  deleteExportTask as deleteExportTaskApi,
  downloadExportFile as downloadExportFileApi,

} from '@/api/question'

// 用户信息
const userRole = localStorage.getItem('userRole')

// 数据定义
const loading = ref(false)
const draftLoading = ref(false)
const recycleLoading = ref(false)
const questionList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const questionType = ref(0)
const keyword = ref('')
const dialogVisible = ref(false)
const batchDialogVisible = ref(false)
const showDrafts = ref(false)
const showRecycleBin = ref(false)
const previewVisible = ref(false)
const historyVisible = ref(false)
const compareVisible = ref(false)
const draftCount = ref(0)
const draftList = ref([])
const recycleBinList = ref([])
const duplicateWarning = ref(false)
const formRef = ref(null)
const showBatch = ref(false)
const selectedIds = ref([])
const batchDifficulty = ref(null)
const batchSubject = ref(null)
const importResult = ref(null)
const historyList = ref([])
const compareData = ref({})

// ========== 新增：查询和导出相关变量 ==========
const showAdvancedQuery = ref(false)
const showExportDialog = ref(false)
const showExportHistoryDialog = ref(false)
const isAdmin = computed(() => userRole === '1')

// 重复检测组件
const duplicateDialogVisible = ref(false)
const pendingCheckQuestionId = ref(null)

// 统计分析组件
const statisticsDialogVisible = ref(false)

const difficultyColors = ['#99A9BF', '#F7BA2A', '#FF9900']
const subjectOptions = ref([])

// 表单数据
const questionForm = reactive({
  id: null,
  questionType: null,
  subjectId: null,
  difficulty: 1,
  suggestedTime: 5,
  source: '自编',
  content: '',
  analysis: '',
  choices: [],
  answers: [],
  status: 1,
  changeNote: '',
  _originalType: null
})

const previewData = ref({})

// 计算属性
const isChoiceQuestion = computed(() => {
  return questionForm.questionType === 1 || questionForm.questionType === 2
})

const dialogTitle = computed(() => {
  return questionForm.id ? '编辑试题' : '新增试题'
})

// 表单验证规则
const formRules = {
  questionType: [{ required: true, message: '请选择题型', trigger: 'change' }],
  subjectId: [{ required: true, message: '请选择所属学科', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度等级', trigger: 'change' }],
  content: [{ required: true, message: '请输入题目内容', trigger: 'blur' }]
}

// 生命周期
onMounted(() => {
  fetchQuestionList()
  fetchSubjectOptions()
  fetchDraftCount()
})

// 获取试题列表
const fetchQuestionList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      type: questionType.value || undefined,
      keyword: keyword.value || undefined
    }
    const res = await getQuestionList(params)
    questionList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('获取试题列表失败')
  } finally {
    loading.value = false
  }
}

// 获取学科选项
const fetchSubjectOptions = async () => {
  try {
    const res = await getSubjectTreeApi()
    subjectOptions.value = res.data || []
  } catch (error) {
    console.error('获取学科列表失败', error)
    subjectOptions.value = [
      {
        id: 1,
        name: '计算机科学',
        children: [
          { id: 6, name: '编程语言' },
          { id: 7, name: '数据结构与算法' }
        ]
      },
      {
        id: 2,
        name: '数学',
        children: [
          { id: 12, name: '高等数学' },
          { id: 13, name: '线性代数' }
        ]
      }
    ]
  }
}

// 获取暂存数量
const fetchDraftCount = async () => {
  try {
    const res = await getDraftsApi()
    draftList.value = res.data || []
    draftCount.value = res.data?.length || 0
  } catch (error) {
    console.error('获取暂存数量失败', error)
  }
}

// 获取回收站列表
const fetchRecycleBin = async () => {
  recycleLoading.value = true
  try {
    const res = await getRecycleBinApi()
    recycleBinList.value = res.data || []
  } catch (error) {
    ElMessage.error('获取回收站列表失败')
  } finally {
    recycleLoading.value = false
  }
}

// 新增试题
const handleAdd = () => {
  resetForm()
  dialogVisible.value = true
}

// 编辑试题
const handleEdit = async (row) => {
  try {
    const res = await getQuestionDetail(row.id)
    Object.assign(questionForm, res.data)
    questionForm._originalType = res.data.questionType
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取试题详情失败')
  }
}

// 预览试题
const handleView = async (row) => {
  try {
    const res = await getQuestionDetail(row.id)
    previewData.value = res.data
    previewVisible.value = true
  } catch (error) {
    ElMessage.error('获取试题详情失败')
  }
}

// 添加当前试题ID
const currentQuestionId = ref(null)
// 查看历史
const handleHistory = async (row) => {
  currentQuestionId.value = row.id
  try {
    const res = await getQuestionHistoryApi(row.id)
    historyList.value = res.data || []
    historyVisible.value = true
  } catch (error) {
    ElMessage.error('获取历史记录失败')
  }
}

// 删除试题
const handleDelete = (row) => {
  ElMessageBox.prompt('请输入删除原因', '删除试题', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /\S/,
    inputErrorMessage: '删除原因不能为空'
  }).then(async ({ value }) => {
    try {
      await deleteQuestionApi(row.id, value)
      ElMessage.success('删除成功')
      fetchQuestionList()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 检查重复
const checkDuplicate = async () => {
  if (!questionForm.content || questionForm.id) return
  try {
    const res = await checkDuplicateApi(questionForm.content)
    duplicateWarning.value = res.data > 0
  } catch (error) {
    console.error('检查重复失败', error)
  }
}

// 题型变化
const handleQuestionTypeChange = () => {
  questionForm.answers = []

  if (isChoiceQuestion.value) {
    if (questionForm.choices.length === 0) {
      ;['A', 'B', 'C', 'D'].forEach(label => {
        questionForm.choices.push({
          optionLabel: label,
          optionContent: '',
          isCorrect: 0,
          sortOrder: questionForm.choices.length
        })
      })
    }
  } else {
    questionForm.choices = []
    addAnswer()
  }
}

// 添加选项
const addChoice = () => {
  const nextLabel = String.fromCharCode(65 + questionForm.choices.length)
  questionForm.choices.push({
    optionLabel: nextLabel,
    optionContent: '',
    isCorrect: 0,
    sortOrder: questionForm.choices.length
  })
}

// 删除选项
const removeChoice = (index) => {
  questionForm.choices.splice(index, 1)
  questionForm.choices.forEach((choice, i) => {
    choice.optionLabel = String.fromCharCode(65 + i)
    choice.sortOrder = i
  })
}

// 添加答案
const addAnswer = () => {
  const answer = {
    blankIndex: questionForm.answers.length + 1,
    answerText: '',
    matchType: 1,
    caseSensitive: 0
  }

  if (questionForm.questionType === 4) {
    answer.isKeyPoint = 1
    answer.keyPointScore = 5
  } else {
    answer.isCorrect = 1
  }

  questionForm.answers.push(answer)
}

// 删除答案
const removeAnswer = (index) => {
  questionForm.answers.splice(index, 1)
}

// 暂存
const saveAsDraft = () => {
  questionForm.status = 0
  submitForm()
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (questionForm.id && questionForm._originalType) {
          if (questionForm.questionType !== questionForm._originalType) {
            ElMessage.warning('题型不可修改，已恢复原题型')
            questionForm.questionType = questionForm._originalType
          }
        }

        const submitData = { ...questionForm }
        delete submitData._originalType

        if (questionForm.id) {
          await updateQuestion(submitData)
          ElMessage.success('更新成功')
        } else {
          await insertQuestion(submitData)
          ElMessage.success('入库成功')
        }
        dialogVisible.value = false
        fetchQuestionList()
        fetchDraftCount()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '操作失败')
      }
    }
  })
}

// 重置表单
const resetForm = () => {
  Object.assign(questionForm, {
    id: null,
    questionType: null,
    subjectId: null,
    difficulty: 1,
    suggestedTime: 5,
    source: '自编',
    content: '',
    analysis: '',
    choices: [],
    answers: [],
    status: 1,
    changeNote: '',
    _originalType: null
  })
  duplicateWarning.value = false
}

// 对话框关闭
const handleDialogClose = () => {
  resetForm()
  formRef.value?.clearValidate()
}

// 批量导入
const handleBatchImport = () => {
  batchDialogVisible.value = true
  importResult.value = null
}

// 下载模板
const handleDownloadTemplate = async () => {
  try {
    const res = await downloadTemplateApi()
    const blob = new Blob([res.data], { type: 'text/csv' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = '试题导入模板.csv'
    link.click()
    URL.revokeObjectURL(link.href)
  } catch (error) {
    ElMessage.error('下载模板失败')
  }
}

// 自定义上传
const handleUpload = async (options) => {
  const formData = new FormData()
  formData.append('file', options.file)

  try {
    const res = await batchImportApi(formData)
    importResult.value = res.data
    if (res.data.fail === 0) {
      ElMessage.success(`导入成功${res.data.success}条`)
      setTimeout(() => {
        batchDialogVisible.value = false
        fetchQuestionList()
      }, 2000)
    }
  } catch (error) {
    ElMessage.error('导入失败')
  }
}

// 上传前验证
const beforeUpload = (file) => {
  const isCSV = file.type === 'text/csv' || file.name.endsWith('.csv')
  if (!isCSV) {
    ElMessage.error('只能上传CSV文件')
    return false
  }
  return true
}

// 继续编辑暂存
const continueEdit = async (row) => {
  try {
    const res = await getDraftDetailApi(row.id)
    Object.assign(questionForm, res.data)
    questionForm._originalType = res.data.questionType
    showDrafts.value = false
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取暂存详情失败')
  }
}

// 删除暂存
const handleDeleteDraft = (row) => {
  ElMessageBox.confirm('确定要删除该暂存试题吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteDraftApi(row.id, false)
      ElMessage.success('删除成功')
      fetchDraftCount()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

// 恢复完成后的处理
const handleRestoreComplete = (id) => {
  fetchQuestionList()
  fetchDraftCount()
}

// 彻底删除完成后的处理
const handlePermanentDeleteComplete = (id) => {
  fetchQuestionList()
}

// 查看历史版本
const viewHistoryVersion = (item) => {
  previewData.value = {
    questionType: item.questionType,
    content: item.content,
    analysis: item.analysis,
    difficulty: item.difficulty
  }
  previewVisible.value = true
}

// 对比版本
const compareWithCurrent = async (item) => {
  try {
    const res = await getQuestionDetail(item.questionId)
    compareData.value = {
      content1: item.content,
      content2: res.data.content,
      analysis1: item.analysis,
      analysis2: res.data.analysis,
      difficulty1: item.difficulty,
      difficulty2: res.data.difficulty
    }
    compareVisible.value = true
  } catch (error) {
    ElMessage.error('获取当前版本失败')
  }
}

// 回滚版本
const handleRollbackToVersion = async (item) => {
  try {
    await rollbackToVersionApi(item.questionId, item.id)
    ElMessage.success('回滚成功')
    historyVisible.value = false
    fetchQuestionList()
  } catch (error) {
    ElMessage.error('回滚失败')
  }
}

// 批量操作
const handleBatchChange = (val) => {
  if (!val) {
    selectedIds.value = []
  }
}

const handleSelectionChange = (val) => {
  selectedIds.value = val.map(item => item.id)
}

const handleBatchUpdateDifficulty = async () => {
  if (!batchDifficulty.value) {
    ElMessage.warning('请选择要修改的难度')
    return
  }

  try {
    await batchUpdateDifficultyApi({
      ids: selectedIds.value,
      difficulty: batchDifficulty.value,
      changeNote: `批量修改难度为${batchDifficulty.value}`
    })
    ElMessage.success('批量更新成功')
    batchDifficulty.value = null
    fetchQuestionList()
  } catch (error) {
    ElMessage.error('批量更新失败')
  }
}

const handleBatchUpdateSubject = async () => {
  if (!batchSubject.value) {
    ElMessage.warning('请选择要修改的学科')
    return
  }

  try {
    await batchUpdateSubjectApi({
      ids: selectedIds.value,
      subjectId: batchSubject.value,
      changeNote: '批量修改学科'
    })
    ElMessage.success('批量更新成功')
    batchSubject.value = null
    fetchQuestionList()
  } catch (error) {
    ElMessage.error('批量更新失败')
  }
}

// ========== 新增：高级查询方法 ==========
const handleAdvancedSearch = async (queryData) => {
  loading.value = true
  try {
    const data = {
      ...queryData,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    const res = await advancedQueryApi(data)
    if (res.code === 0) {
      questionList.value = res.data.list || []
      total.value = res.data.total || 0
      ElMessage.success(`找到 ${total.value} 条记录`)
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    console.error('高级查询失败', error)
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

// ========== 新增：导出成功回调 ==========
const handleExportSuccess = () => {
  showExportHistoryDialog.value = true
  ElMessage.success('导出任务已提交，可在导出历史中查看')
}


// 单题重复检测 - 打开组件并传入试题ID
const checkSingleDuplicate = (questionId) => {
  pendingCheckQuestionId.value = questionId
  duplicateDialogVisible.value = true
}

// 分页相关
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchQuestionList()
}

const handleCurrentChange = (val) => {
  pageNum.value = val
  fetchQuestionList()
}

// 工具函数
const getQuestionTypeName = (type) => {
  const map = { 1: '单选题', 2: '多选题', 3: '填空题', 4: '简答题' }
  return map[type] || '未知'
}

const getQuestionTypeTag = (type) => {
  const map = { 1: 'primary', 2: 'success', 3: 'warning', 4: 'info' }
  return map[type] || ''
}

const getDuplicateLevel = (similarity) => {
  if (similarity >= 95) return '完全相同'
  if (similarity >= 85) return '高度相似'
  if (similarity >= 70) return '中度相似'
  return '低度相似'
}
const getDuplicateRateType = (rate) => {
  if (rate >= 10) return 'danger'
  if (rate >= 5) return 'warning'
  return 'success'
}

const truncateText = (text, maxLen) => {
  if (!text) return ''
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

const getOperationTypeName = (type) => {
  const map = {
    'UPDATE': '修改',
    'DELETE': '删除',
    'BATCH_UPDATE': '批量修改',
    'ROLLBACK': '回滚'
  }
  return map[type] || type
}

const highlightContent = (content) => {
  return content
}

// 监听回收站对话框打开
watch(showRecycleBin, (val) => {
  if (val) {
    fetchRecycleBin()
  }
})
</script>

<style scoped>
.questions-container {
  padding: 20px;
}

.operation-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.search-bar {
  flex: 1;
  max-width: 300px;
  margin-left: auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.batch-bar {
  margin-top: 15px;
  padding: 10px;
  background-color: #f0f9ff;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.choice-item {
  margin-bottom: 10px;
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.choice-label {
  font-weight: bold;
  color: #409eff;
}

.answer-item {
  margin-bottom: 15px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.unit {
  margin-left: 5px;
  color: #999;
}

.type-tip {
  margin-top: 5px;
}

.duplicate-warning {
  margin-top: 10px;
}

.import-result {
  margin-top: 20px;
}

.error-list {
  margin-top: 10px;
  max-height: 200px;
  overflow-y: auto;
  padding: 10px;
  background-color: #fef0f0;
  border-radius: 4px;
}

.error-item {
  color: #f56c6c;
  margin: 5px 0;
  font-size: 12px;
}

.preview-content {
  padding: 10px;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.preview-question,
.preview-choices,
.preview-answers,
.preview-analysis {
  margin-bottom: 20px;
}

.preview-choices .choice-item {
  margin: 5px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.correct-tag {
  margin-left: 10px;
}

.answer-score {
  margin-left: 10px;
  color: #e6a23c;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.history-header {
  flex: 1;
}

.history-operator {
  font-weight: bold;
  margin-right: 10px;
}

.history-operation {
  color: #409eff;
  margin-right: 10px;
}

.history-note {
  color: #999;
  font-size: 12px;
}

.history-actions {
  display: flex;
  gap: 10px;
}

.compare-content {
  display: flex;
  gap: 20px;
}

.compare-left,
.compare-right {
  flex: 1;
  padding: 10px;
  border: 1px solid #eee;
  border-radius: 4px;
  min-height: 200px;
}

.compare-left h4,
.compare-right h4 {
  margin-top: 0;
  padding-bottom: 5px;
  border-bottom: 1px solid #eee;
}

:deep(.el-rate) {
  display: inline-block;
}
</style>