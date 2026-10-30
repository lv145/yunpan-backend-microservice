package com.lgg.yunpanbackendjudgeservice.judge.strategy;

import cn.hutool.json.JSONUtil;
import com.lgg.yunpanbackendmodel.model.codesandbox.ExecuteCodeInfo;
import com.lgg.yunpanbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.lgg.yunpanbackendmodel.model.codesandbox.JudgeInfo;
import com.lgg.yunpanbackendmodel.model.dto.question.JudgeCase;
import com.lgg.yunpanbackendmodel.model.dto.question.JudgeConfig;
import com.lgg.yunpanbackendmodel.model.entity.Question;
import com.lgg.yunpanbackendmodel.model.enums.JudgeInfoMessageEnum;
import com.lgg.yunpanbackendmodel.model.enums.SandBoxResponseStatus;

import java.util.List;
import java.util.Optional;

/**
 * Java 程序的判题策略
 */
public class DefaultJudgeStrategy implements JudgeStrategy {

    /**
     * 执行判题
     * @param judgeContext
     * @return
     */
    @Override
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        //获取JudgeContext属性
        JudgeConfig judgeConfig = judgeContext.getJudgeConfig();
        List<String> inputList = judgeContext.getInputList();
        List<JudgeCase> judgeCaseList = judgeContext.getJudgeCaseList();
        ExecuteCodeResponse executeCodeResponse = judgeContext.getExecuteCodeResponse();
        List<ExecuteCodeInfo> executeCodeInfoList = executeCodeResponse.getExecuteCodeInfoList();

        JudgeInfo judgeInfoResponse = JudgeInfo.builder().maxMemory(executeCodeResponse.getMaxMemory())
                .maxTime(executeCodeResponse.getMaxTime()).message(JudgeInfoMessageEnum.ACCEPTED.getValue())
                .executeCodeInfoList(executeCodeInfoList).build();

        SandBoxResponseStatus status = SandBoxResponseStatus.getEnumByValue(executeCodeResponse.getStatus());
        // 先判断沙箱执行的结果输出数量是否和预期输出数量相等
        if ( !SandBoxResponseStatus.SUCCESS.equals(status)||executeCodeInfoList.size() != inputList.size()) {
            String message=JudgeInfoMessageEnum.getValueBySandBoxResponseStatus(status);
            judgeInfoResponse.setMessage(message);
            return judgeInfoResponse;
        }
        for (int i = 0; i < executeCodeInfoList.size(); i++) {
            JudgeCase judgeCase = judgeCaseList.get(i);
            String output = judgeCase.getOutput();
            String resultMessage = executeCodeInfoList.get(i).getResultMessage();
            if (!output.equals(resultMessage)) {
                judgeInfoResponse.setMessage(JudgeInfoMessageEnum.WRONG_ANSWER.getValue());
                return judgeInfoResponse;
            }
        }
        Long maxMemory = Optional.ofNullable(executeCodeResponse.getMaxMemory()).orElse(0L);
        Long maxTime = Optional.ofNullable(executeCodeResponse.getMaxTime()).orElse(0L);
        // 判断题目限制
        Long needMemoryLimit = judgeConfig.getMemoryLimit();
        Long needTimeLimit = judgeConfig.getTimeLimit();
        if (maxMemory > needMemoryLimit) {
            judgeInfoResponse.setMessage(JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED.getValue());
            return judgeInfoResponse;
        }
        if (maxTime > needTimeLimit) {
            judgeInfoResponse.setMessage(JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getValue());
            return judgeInfoResponse;
        }

        return judgeInfoResponse;
    }
}
