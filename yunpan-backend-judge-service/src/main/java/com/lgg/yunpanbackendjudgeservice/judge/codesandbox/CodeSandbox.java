package com.lgg.yunpanbackendjudgeservice.judge.codesandbox;

import com.lgg.yunpanbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.lgg.yunpanbackendmodel.model.codesandbox.ExecuteCodeResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 代码沙箱接口定义
 */
public interface CodeSandbox {

    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
