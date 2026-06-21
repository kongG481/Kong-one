package com.example.controller.question;

import com.example.entity.question.Duplicate;
import com.example.entity.Result;
import com.example.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/question/duplicate")
public class DuplicateController {

    @Autowired
    private QuestionService questionService;

    @GetMapping("/check/{questionId}")
    public Result<Map<String, Object>> checkDuplicate(
            @PathVariable Integer questionId,
            @RequestParam(required = false) Integer threshold) {
        Map<String, Object> result = questionService.checkDuplicate(questionId, threshold);
        return Result.success(result);
    }

    @PostMapping("/batch-check")
    public Result<Map<String, Object>> batchCheck() {
        Map<String, Object> result = questionService.batchCheckAllDuplicates();
        return Result.success(result);
    }

    @PutMapping("/handle/{duplicateId}")
    public Result<String> handleDuplicate(
            @PathVariable Integer duplicateId,
            @RequestParam String handleType) {
        boolean success = questionService.handleDuplicate(duplicateId, handleType);
        if (success) {
            return Result.success("处理成功");
        }
        return Result.error("处理失败");
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getDuplicateStatistics() {
        Map<String, Object> stats = questionService.getDuplicateStatistics();
        return Result.success(stats);
    }

    @GetMapping("/pending")
    public Result<List<Duplicate>> getPendingDuplicates() {
        List<Duplicate> list = questionService.getPendingDuplicates();
        return Result.success(list);
    }
}