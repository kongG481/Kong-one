package com.example.config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 导出文件配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "export.file")
public class ExportConfig {

    /**
     * 导出文件存储路径
     */
    private String path = "D:/专业设计/mycode/vue-question/src/exports";

    /**
     * 文件保留天数
     */
    private Retention retention = new Retention();

    @Data
    public static class Retention {
        /**
         * 文件保留天数
         */
        private Integer days = 7;
    }
}
