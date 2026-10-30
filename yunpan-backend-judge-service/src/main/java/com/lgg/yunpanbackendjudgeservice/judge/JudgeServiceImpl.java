package com.lgg.yunpanbackendjudgeservice.judge;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.lgg.yunpanbackendcommon.common.ErrorCode;
import com.lgg.yunpanbackendcommon.exception.BusinessException;

import com.lgg.yunpanbackendcommon.exception.ThrowUtils;
import com.lgg.yunpanbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.lgg.yunpanbackendjudgeservice.judge.codesandbox.CodeSandboxFactory;
import com.lgg.yunpanbackendjudgeservice.judge.codesandbox.CodeSandboxProxy;
import com.lgg.yunpanbackendjudgeservice.judge.strategy.JudgeContext;
import com.lgg.yunpanbackendmodel.model.codesandbox.ExecuteCodeInfo;
import com.lgg.yunpanbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.lgg.yunpanbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.lgg.yunpanbackendmodel.model.codesandbox.JudgeInfo;

import com.lgg.yunpanbackendmodel.model.dto.question.JudgeCase;
import com.lgg.yunpanbackendmodel.model.dto.question.JudgeConfig;
import com.lgg.yunpanbackendmodel.model.entity.Question;
import com.lgg.yunpanbackendmodel.model.entity.QuestionSubmit;
import com.lgg.yunpanbackendmodel.model.enums.JudgeInfoMessageEnum;
import com.lgg.yunpanbackendmodel.model.enums.QuestionSubmitStatusEnum;
import com.lgg.yunpanbackendmodel.model.enums.SandBoxResponseStatus;
import com.lgg.yunpanbackendserviceclient.service.QuestionFeignClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionFeignClient questionFeignClient;

    @Resource
    private JudgeManager judgeManager;

    @Value("${codesandbox.type:example}")
    private String type;


    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        // 1）传入题目的提交 id，获取到对应的题目、提交信息（包含代码、编程语言等）
        QuestionSubmit questionSubmit = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        ThrowUtils.throwIf(questionSubmit==null,ErrorCode.NOT_FOUND_ERROR,"提交信息不存在");

        Long questionId = questionSubmit.getQuestionId();
        Question question = questionFeignClient.getQuestionById(questionId);
        ThrowUtils.throwIf(question==null,ErrorCode.NOT_FOUND_ERROR,"题目不存在");
        ThrowUtils.throwIf(!questionSubmit.getStatus().equals(QuestionSubmitStatusEnum.WAITING.getValue()),
                ErrorCode.OPERATION_ERROR,"题目正在判题中");

        // 3）更改判题（题目提交）的状态为 “判题中”，防止重复执行
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
        ThrowUtils.throwIf(!questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate),ErrorCode.SYSTEM_ERROR, "题目状态更新错误");

        //4)获取题目信息
        String language = questionSubmit.getLanguage();
        String code = questionSubmit.getCode();
        String judgeCaseStr = question.getJudgeCase();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());

        // 5）调用沙箱，获取到执行结果
        CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
        codeSandbox = new CodeSandboxProxy(codeSandbox);
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(code)
                .language(language)
                .inputList(inputList)
                .build();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        ThrowUtils.throwIf(executeCodeResponse==null,ErrorCode.API_REQUEST_ERROR,"沙箱服务错误");

        SandBoxResponseStatus status =SandBoxResponseStatus.getEnumByValue(executeCodeResponse.getStatus());
        ThrowUtils.throwIf(status==null,ErrorCode.SYSTEM_ERROR,"沙箱状态码解析失败");
        String judgeStatus = status.getMessage();
        questionSubmitUpdate.setJudgeStatus(judgeStatus);;

        JudgeConfig judgeConfig = JSONUtil.toBean(question.getJudgeConfig(), JudgeConfig.class);
        // 6）根据沙箱的执行结果，设置题目的判题状态和信息
        JudgeContext judgeContext = JudgeContext.builder().inputList(inputList)
                .executeCodeResponse(executeCodeResponse).judgeCaseList(judgeCaseList)
                .language(questionSubmit.getLanguage()).judgeConfig(judgeConfig).build();

        // 7）修改数据库中的判题结果
        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);
        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        ThrowUtils.throwIf(!questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate),ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        QuestionSubmit questionSubmitResult = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        return questionSubmitResult;
    }


}
