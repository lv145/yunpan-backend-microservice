package com.lgg.yunpanbackendjudgeservice.judge.codesandbox.impl;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.lgg.yunpanbackendcommon.common.BaseResponse;
import com.lgg.yunpanbackendcommon.common.ErrorCode;

import com.lgg.yunpanbackendcommon.exception.ThrowUtils;
import com.lgg.yunpanbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.lgg.yunpanbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.lgg.yunpanbackendmodel.model.codesandbox.ExecuteCodeResponse;

import java.util.List;


/**
 * 远程代码沙箱（实际调用接口的沙箱）
 */
public class RemoteCodeSandbox implements CodeSandbox {

    // 定义鉴权请求头和密钥
    private static final String AUTH_REQUEST_HEADER = "X-Auth-Secret";

    private static final String AUTH_REQUEST_SECRET = "lgg";

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest){
        String url = "http://192.168.159.110:8090/executeCode";
        String json = JSONUtil.toJsonStr(executeCodeRequest);
        HttpRequest request = HttpUtil.createPost(url)
                .header(AUTH_REQUEST_HEADER, AUTH_REQUEST_SECRET)
                .body(json)
                .timeout(20000);
        HttpResponse response = request.execute();
        if (response.isOk()) {
            String responseStr= response.body();
            BaseResponse<ExecuteCodeResponse>  baseResponse = JSONUtil.toBean(responseStr, new TypeReference<BaseResponse<ExecuteCodeResponse>>() {},false);
            ThrowUtils.throwIf(baseResponse.getCode()!=ErrorCode.SUCCESS.getCode(),ErrorCode.API_REQUEST_ERROR,baseResponse.toString());
            return baseResponse.getData();
        }
        return null;
    }
}
