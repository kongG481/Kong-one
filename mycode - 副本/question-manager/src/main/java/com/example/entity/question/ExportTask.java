// ExportTask.java

package com.example.entity.question;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExportTask {
    private Integer id;
    private Integer userId;
    private String exportFormat;
    private Integer questionCount;
    private String fileUrl;
    private LocalDateTime createTime;
    private String userName;  // 非数据库字段

    // 添加辅助方法判断文件是否存在
    public boolean isFileExists() {
        if (fileUrl == null) {
            return false;
        }
        java.io.File file = new java.io.File(fileUrl);
        return file.exists();
    }

    // 获取文件大小
    public long getFileSize() {
        if (fileUrl == null) {
            return 0;
        }
        java.io.File file = new java.io.File(fileUrl);
        return file.exists() ? file.length() : 0;
    }

    // 获取文件名
    public String getFileName() {
        if (fileUrl == null) {
            return "";
        }
        return fileUrl.substring(fileUrl.lastIndexOf(java.io.File.separator) + 1);
    }
}