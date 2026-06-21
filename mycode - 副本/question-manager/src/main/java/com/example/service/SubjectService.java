// SubjectService.java
package com.example.service;

import com.example.entity.question.Subject;
import java.util.List;
import java.util.Map;

public interface SubjectService {
    List<Subject> getSubjectTree();
    Subject getSubjectById(Integer id);
    // SubjectService.java 中添加

    /**
     * 添加学科
     */
    void addSubject(Subject subject);

    /**
     * 更新学科
     */
    void updateSubject(Subject subject);

    /**
     * 删除学科
     */
    void deleteSubject(Integer id);

    /**
     * 获取学科统计信息
     */
    Map<String, Object> getSubjectStats();
}