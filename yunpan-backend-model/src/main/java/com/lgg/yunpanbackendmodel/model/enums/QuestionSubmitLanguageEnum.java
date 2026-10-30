package com.lgg.yunpanbackendmodel.model.enums;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目提交编程语言枚举
 *
 * @author lgg
 * @from 云盘在线判题平台
 */
public enum QuestionSubmitLanguageEnum {

    JAVA("java" ),
    C("c"),
    PYTHON3("python3");

    private final String language;

    QuestionSubmitLanguageEnum( String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

    public static QuestionSubmitLanguageEnum getEnumByLanguage(String language) {
        for (QuestionSubmitLanguageEnum value : QuestionSubmitLanguageEnum.values()) {
            if (value.language.equals(language)) {
                return value;
            }
        }
        return null;
    }
}
