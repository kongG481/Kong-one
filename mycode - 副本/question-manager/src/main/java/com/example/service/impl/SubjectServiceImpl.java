// SubjectServiceImpl.java
package com.example.service.impl;

import com.example.entity.question.Subject;
import com.example.exception.BusinessException;
import com.example.mapper.exam.QuestionMapper;
import com.example.mapper.exam.SubjectMapper;
import com.example.service.SubjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SubjectServiceImpl implements SubjectService {

    @Autowired
    private SubjectMapper subjectMapper;

    @Autowired
    private QuestionMapper questionMapper;  // 注入 QuestionMapper

    @Override
    public List<Subject> getSubjectTree() {
        List<Subject> topLevelSubjects = subjectMapper.getTopLevelSubjects();

        for (Subject subject : topLevelSubjects) {
            subject.setChildren(getChildrenSubjects(subject.getId()));
        }

        return topLevelSubjects;
    }

    private List<Subject> getChildrenSubjects(Integer parentId) {
        List<Subject> children = subjectMapper.getSubjectsByParentId(parentId);
        for (Subject child : children) {
            child.setChildren(getChildrenSubjects(child.getId()));
        }
        return children;
    }

    @Override
    public Subject getSubjectById(Integer id) {
        return subjectMapper.findById(id);
    }

    // SubjectServiceImpl.java 中添加

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSubject(Subject subject) {
        // 检查学科名称是否已存在
        if (subjectMapper.countByName(subject.getName(), 0) > 0) {
            throw new BusinessException("学科名称已存在");
        }

        // 计算层级
        if (subject.getParentId() != null && subject.getParentId() > 0) {
            Subject parent = subjectMapper.findById(subject.getParentId());
            if (parent == null) {
                throw new BusinessException("上级学科不存在");
            }
            subject.setLevel(parent.getLevel() + 1);
        } else {
            subject.setParentId(0);
            subject.setLevel(1);
        }

        subject.setStatus(subject.getStatus() == null ? 1 : subject.getStatus());
        subject.setSortOrder(subject.getSortOrder() == null ? 0 : subject.getSortOrder());

        subjectMapper.insert(subject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSubject(Subject subject) {
        // 检查学科是否存在
        Subject existing = subjectMapper.findById(subject.getId());
        if (existing == null) {
            throw new BusinessException("学科不存在");
        }

        // 检查名称是否重复
        if (subjectMapper.countByName(subject.getName(), subject.getId()) > 0) {
            throw new BusinessException("学科名称已存在");
        }

        // 不能将学科设置为自己为父级
        if (subject.getParentId() != null && subject.getParentId().equals(subject.getId())) {
            throw new BusinessException("不能将自己设为上级学科");
        }

        // 检查父级是否存在
        if (subject.getParentId() != null && subject.getParentId() > 0) {
            Subject parent = subjectMapper.findById(subject.getParentId());
            if (parent == null) {
                throw new BusinessException("上级学科不存在");
            }
            subject.setLevel(parent.getLevel() + 1);
        }

        subjectMapper.update(subject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubject(Integer id) {
        // 检查是否有子节点
        if (subjectMapper.countByParentId(id) > 0) {
            throw new BusinessException("该学科下有子学科，无法删除");
        }

        // 检查是否有关联的试题
        if (questionMapper.countBySubjectId(id) > 0) {
            throw new BusinessException("该学科下有关联试题，无法删除");
        }

        subjectMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> getSubjectStats() {
        List<Subject> allSubjects = subjectMapper.getAllSubjects();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", allSubjects.size());

        long level1 = allSubjects.stream().filter(s -> s.getLevel() == 1).count();
        long level2 = allSubjects.stream().filter(s -> s.getLevel() == 2).count();
        long level3 = allSubjects.stream().filter(s -> s.getLevel() == 3).count();

        stats.put("level1", level1);
        stats.put("level2", level2);
        stats.put("level3", level3);

        long enabled = allSubjects.stream().filter(s -> s.getStatus() == 1).count();
        long disabled = allSubjects.stream().filter(s -> s.getStatus() == 0).count();

        stats.put("enabled", enabled);
        stats.put("disabled", disabled);

        return stats;
    }
}