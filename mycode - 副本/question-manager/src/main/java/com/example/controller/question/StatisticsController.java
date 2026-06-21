package com.example.controller.question;

import com.example.entity.Result;
import com.example.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/question/statistics")
public class StatisticsController {

    @Autowired
    private QuestionService questionService;

    @GetMapping("/full")
    public Result<Map<String, Object>> getFullStatistics() {
        Map<String, Object> statistics = questionService.getFullStatistics();
        return Result.success(statistics);
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportStatisticsToExcel() throws Exception {
        byte[] excelData = questionService.exportStatisticsToExcel();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "question_statistics.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }
}
