package com.example.utils;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 文本相似度计算工具类
 * 提供多种算法（编辑距离、Jaccard、余弦相似度）计算文本相似度，并给出重复级别和建议
 */
@Component
public class SimilarityUtil {

    /**
     * 停用词集合，用于在相似度计算中过滤常见词汇
     * 包含中文和英文的常见停用词
     */
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "的", "了", "是", "在", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
            "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
            "the", "a", "an", "and", "or", "but", "so", "for", "nor", "yet", "of", "to", "in",
            "for", "on", "by", "with", "without", "is", "are", "was", "were", "be", "been"
    ));

    /**
     * 标点符号正则表达式，用于预处理时移除标点
     * 匹配中英文常见标点符号
     */
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[\\p{Punct}，。！？；：“”‘’《》【】、]");
    /**
     * 空白字符正则表达式，用于将连续空白字符替换为单个空格
     */
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * 综合相似度计算（加权平均）
     * 结合编辑距离、Jaccard和余弦相似度三种算法，返回加权后的相似度百分比
     *
     * @param text1 第一个文本字符串
     * @param text2 第二个文本字符串
     * @return 相似度百分比，范围0-100，保留两位小数
     */
    public static double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0;
        if (text1.equals(text2)) return 100;

        // 预处理文本：转小写、移除标点、规范化空白字符
        String processed1 = preprocess(text1);
        String processed2 = preprocess(text2);

        if (processed1.isEmpty() || processed2.isEmpty()) return 0;
        if (processed1.equals(processed2)) return 100;

        // 分别计算三种相似度
        double levenshteinSim = levenshteinSimilarity(processed1, processed2);
        double jaccardSim = jaccardSimilarity(processed1, processed2);
        double cosineSim = cosineSimilarity(processed1, processed2);

        // 加权平均：编辑距离权重0.4，Jaccard权重0.3，余弦相似度权重0.3
        double weightedSim = levenshteinSim * 0.4 + jaccardSim * 0.3 + cosineSim * 0.3;
        return new BigDecimal(weightedSim).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 文本预处理
     * 将文本转为小写，移除所有标点符号，并将连续空白字符替换为单个空格
     *
     * @param text 原始文本
     * @return 预处理后的文本
     */
    private static String preprocess(String text) {
        if (text == null) return "";
        String processed = text.toLowerCase();
        processed = PUNCTUATION_PATTERN.matcher(processed).replaceAll(" ");
        processed = WHITESPACE_PATTERN.matcher(processed).replaceAll(" ");
        return processed.trim();
    }

    /**
     * 基于编辑距离（Levenshtein距离）计算相似度
     * 通过计算两个字符串之间的最小编辑操作次数（插入、删除、替换）来衡量相似度
     *
     * @param s1 第一个字符串
     * @param s2 第二个字符串
     * @return 相似度百分比，范围0-100
     */
    public static double levenshteinSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0;
        if (s1.equals(s2)) return 100;
        if (s1.isEmpty() || s2.isEmpty()) return 0;

        int distance = levenshteinDistance(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        return (1 - (double) distance / maxLen) * 100;
    }

    /**
     * 计算两个字符串之间的Levenshtein编辑距离
     * 使用动态规划算法实现
     *
     * @param s1 第一个字符串
     * @param s2 第二个字符串
     * @return 编辑距离（最小操作次数）
     */
    private static int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length(), len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];
        // 初始化边界条件
        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;
        // 动态规划计算最小编辑距离
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[len1][len2];
    }

    /**
     * 基于Jaccard系数计算相似度
     * 通过比较两个文本分词后的词语集合的交集与并集的比例来衡量相似度
     *
     * @param s1 第一个字符串
     * @param s2 第二个字符串
     * @return 相似度百分比，范围0-100
     */
    public static double jaccardSimilarity(String s1, String s2) {
        // 提取文本中的词语集合
        Set<String> set1 = extractWords(s1);
        Set<String> set2 = extractWords(s2);
        if (set1.isEmpty() && set2.isEmpty()) return 100;
        if (set1.isEmpty() || set2.isEmpty()) return 0;

        // 移除停用词
        set1.removeAll(STOP_WORDS);
        set2.removeAll(STOP_WORDS);

        // 计算交集和并集
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size() * 100;
    }

    /**
     * 从文本中提取词语集合
     * 支持中英文混合文本，对中文按字符切分，对英文按空白字符切分
     *
     * @param text 输入文本
     * @return 词语集合
     */
    private static Set<String> extractWords(String text) {
        Set<String> words = new HashSet<>();
        String[] parts = text.split("\\s+");
        for (String part : parts) {
            if (part.length() > 0) {
                // 使用正则表达式切分中文字符，保持英文单词完整
                String[] chineseWords = part.split("(?<![\\u4e00-\\u9fa5])(?=[\\u4e00-\\u9fa5])|(?<=[\\u4e00-\\u9fa5])(?![\\u4e00-\\u9fa5])");
                for (String word : chineseWords) {
                    if (word.length() > 0) words.add(word);
                }
            }
        }
        return words;
    }

    /**
     * 基于余弦相似度计算文本相似度
     * 将文本表示为词频向量，计算两个向量的夹角余弦值
     *
     * @param s1 第一个字符串
     * @param s2 第二个字符串
     * @return 相似度百分比，范围0-100
     */
    public static double cosineSimilarity(String s1, String s2) {
        // 获取词频向量
        Map<String, Integer> vector1 = getTermFrequency(s1);
        Map<String, Integer> vector2 = getTermFrequency(s2);
        if (vector1.isEmpty() || vector2.isEmpty()) return 0;

        // 移除停用词
        vector1.keySet().removeAll(STOP_WORDS);
        vector2.keySet().removeAll(STOP_WORDS);

        // 计算向量点积
        double dotProduct = 0;
        for (String key : vector1.keySet()) {
            if (vector2.containsKey(key)) {
                dotProduct += vector1.get(key) * vector2.get(key);
            }
        }

        // 计算向量模长
        double norm1 = Math.sqrt(vector1.values().stream().mapToDouble(v -> v * v).sum());
        double norm2 = Math.sqrt(vector2.values().stream().mapToDouble(v -> v * v).sum());

        if (norm1 == 0 || norm2 == 0) return 0;
        return (dotProduct / (norm1 * norm2)) * 100;
    }

    /**
     * 获取文本的词频统计
     * 将文本按空白字符切分，统计每个词语出现的次数
     *
     * @param text 输入文本
     * @return 词频映射表
     */
    private static Map<String, Integer> getTermFrequency(String text) {
        Map<String, Integer> freq = new HashMap<>();
        String[] words = text.split("\\s+");
        for (String word : words) {
            if (word.length() > 0) {
                freq.put(word, freq.getOrDefault(word, 0) + 1);
            }
        }
        return freq;
    }

    /**
     * 根据相似度百分比获取重复级别描述
     *
     * @param similarity 相似度百分比
     * @return 重复级别描述字符串
     */
    public static String getDuplicateLevel(double similarity) {
        if (similarity >= 95) return "完全相同";
        if (similarity >= 85) return "高度相似";
        if (similarity >= 70) return "中度相似";
        if (similarity >= 50) return "低度相似";
        return "不相似";
    }

    /**
     * 根据相似度百分比获取处理建议
     *
     * @param similarity 相似度百分比
     * @return 处理建议字符串
     */
    public static String getSuggestion(double similarity) {
        if (similarity >= 95) return "建议直接合并或删除重复题目";
        if (similarity >= 85) return "建议人工审核，考虑合并或保留";
        if (similarity >= 70) return "建议人工复核，可能为变体题目";
        return "相似度较低，可保留";
    }
}
