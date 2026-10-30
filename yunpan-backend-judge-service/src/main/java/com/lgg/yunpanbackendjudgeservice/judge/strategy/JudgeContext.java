package com.lgg.yunpanbackendjudgeservice.judge.strategy;

import com.lgg.yunpanbackendmodel.model.codesandbox.ExecuteCodeInfo;
import com.lgg.yunpanbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.lgg.yunpanbackendmodel.model.codesandbox.JudgeInfo;

import com.lgg.yunpanbackendmodel.model.dto.question.JudgeCase;
import com.lgg.yunpanbackendmodel.model.dto.question.JudgeConfig;
import com.lgg.yunpanbackendmodel.model.entity.Question;
import com.lgg.yunpanbackendmodel.model.entity.QuestionSubmit;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 上下文（用于定义在策略中传递的参数）
 */
@Data
@Builder
public class JudgeContext {
    private String language;

    private JudgeConfig judgeConfig;

    private List<String> inputList;

    private List<JudgeCase> judgeCaseList;
    private ExecuteCodeResponse executeCodeResponse;


}
