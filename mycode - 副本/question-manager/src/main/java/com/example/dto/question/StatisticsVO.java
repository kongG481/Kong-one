package com.example.dto.question;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 统计分析VO
 */
@Data
public class StatisticsVO {
    // 总数统计
    private TotalCount totalCount;
    // 按题型统计
    private List<TypeStat> typeStats;
    // 按学科统计
    private List<SubjectStat> subjectStats;
    // 按难度统计
    private List<DifficultyStat> difficultyStats;
    // 按教师统计（排行榜）
    private List<TeacherStat> teacherStats;
    // 趋势分析（每日新增）
    private List<TrendStat> trendStats;
    // 热门试题
    private List<HotQuestion> hotQuestions;
    // 质量分析
    private QualityAnalysis qualityAnalysis;

    @Data
    public static class TotalCount {
        private Integer total;
        private Integer singleCount;   // 单选
        private Integer multiCount;    // 多选
        private Integer blankCount;    // 填空
        private Integer essayCount;    // 简答
        private Integer easyCount;     // 简单
        private Integer mediumCount;   // 中等
        private Integer hardCount;     // 困难
    }

    @Data
    public static class TypeStat {
        private Integer type;
        private String typeName;
        private Integer count;
        private Double percentage;
    }

    @Data
    public static class SubjectStat {
        private Integer subjectId;
        private String subjectName;
        private Integer count;
        private Double percentage;
    }

    @Data
    public static class DifficultyStat {
        private Integer difficulty;
        private String difficultyName;
        private Integer count;
        private Double percentage;
    }

    @Data
    public static class TeacherStat {
        private Integer teacherId;
        private String teacherName;
        private String college;
        private Integer questionCount;
        private Integer rank;
    }

    @Data
    public static class TrendStat {
        private String date;
        private Integer count;
    }

    @Data
    public static class HotQuestion {
        private Integer questionId;
        private String content;
        private Integer type;
        private String typeName;
        private Integer viewCount;
        private String subjectName;
    }

    @Data
    public static class QualityAnalysis {
        private Integer totalQuestions;
        private Integer duplicateQuestions;
        private Double duplicateRate;      // 重复率
        private Integer pendingCount;      // 待处理重复数
        private Integer ignoredCount;      // 已忽略数
        private Integer mergedCount;       // 已合并数
    }
}
