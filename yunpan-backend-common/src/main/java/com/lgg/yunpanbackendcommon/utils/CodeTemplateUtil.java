package com.lgg.yunpanbackendcommon.utils;

import com.lgg.yunpanbackendcommon.common.ErrorCode;
import com.lgg.yunpanbackendcommon.exception.BusinessException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

public class CodeTemplateUtil {

    /**
     * 读取resource下模板，替换占位符
     * @param templatePath 模板路径 code-template/java-template.txt
     * @param className 类名，一般是 properties.getGlobalFileName() → Main
     * @return 替换完成的代码字符串
     */
    public static String renderTemplate(String templatePath, String className) {
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);
            String templateContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            // 替换占位符
            return templateContent.replace("${CLASS_NAME}", className);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取代码模板失败");
        }
    }
}
