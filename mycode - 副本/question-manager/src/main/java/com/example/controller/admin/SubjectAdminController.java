package com.example.controller.admin;

import com.example.controller.BaseController;
import com.example.entity.question.Subject;
import com.example.entity.Result;
import com.example.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/subject")
public class SubjectAdminController extends BaseController {

    @Autowired
    private SubjectService subjectService;

    /**
     * 获取学科树（管理员用）
     */
    @GetMapping("/tree")
    public Result<List<Subject>> getSubjectTree() {
        List<Subject> tree = subjectService.getSubjectTree();
        return Result.success(tree);
    }

    /**
     * 添加学科
     */
    @PostMapping
    public Result addSubject(@RequestBody Subject subject) {
        subjectService.addSubject(subject);
        return Result.success();
    }

    /**
     * 更新学科
     */
    @PutMapping("/{id}")
    public Result updateSubject(@PathVariable Integer id, @RequestBody Subject subject) {
        subject.setId(id);
        subjectService.updateSubject(subject);
        return Result.success();
    }

    /**
     * 删除学科
     */
    @DeleteMapping("/{id}")
    public Result deleteSubject(@PathVariable Integer id) {
        subjectService.deleteSubject(id);
        return Result.success();
    }

    /**
     * 获取学科统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getSubjectStats() {
        Map<String, Object> stats = subjectService.getSubjectStats();
        return Result.success(stats);
    }
}