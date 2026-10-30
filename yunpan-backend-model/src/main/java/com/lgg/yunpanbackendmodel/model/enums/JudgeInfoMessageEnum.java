package com.lgg.yunpanbackendmodel.model.enums;

import org.apache.commons.lang3.ObjectUtils;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 判题错误信息枚举（沙盒返回的详细错误类型，填入 judgeInfo.message）
 *
 */
public enum JudgeInfoMessageEnum {

    ACCEPTED("成功", "Accepted"),
    WRONG_ANSWER("答案错误", "Wrong Answer"),
    COMPILE_ERROR("编译错误", "Compile Error"),
    MEMORY_LIMIT_EXCEEDED("内存超限", "Memory Limit Exceeded"),
    TIME_LIMIT_EXCEEDED("时间超限", "Time Limit Exceeded"),
    PRESENTATION_ERROR("格式错误", "Presentation Error"),
    OUTPUT_LIMIT_EXCEEDED("输出超限", "Output Limit Exceeded"),
    RUNTIME_ERROR("运行时错误", "Runtime Error"),
    SYSTEM_ERROR("系统异常", "System Error"),
    DANGEROUS_OPERATION("危险操作", "Dangerous Operation");

    private final String text;
    private final String value;

    JudgeInfoMessageEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public static String getValueBySandBoxResponseStatus(SandBoxResponseStatus status) {
        if (ObjectUtils.isEmpty(status)) {
            return null;
        }
        switch (status){
            case RUN_FAILED :
                return RUNTIME_ERROR.value;
            case COMPILE_TIMEOUT:
            case COMPILE_FAILED:
                return COMPILE_ERROR.value;
            case RUN_TIME_OUT:
                return TIME_LIMIT_EXCEEDED.value;
            case OUTPUT_LIMIT_EXCEEDED:
                return OUTPUT_LIMIT_EXCEEDED.value;
            default:
                return null;
        }
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static JudgeInfoMessageEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (JudgeInfoMessageEnum anEnum : JudgeInfoMessageEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
