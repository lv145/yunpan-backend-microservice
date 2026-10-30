package com.lgg.yunpanbackendmodel.model.enums;

import cn.hutool.core.util.ObjectUtil;
import org.apache.commons.lang3.ObjectUtils;

public enum SandBoxResponseStatus {
    SUCCESS(0, "运行成功"),
    RUN_FAILED(1, "运行失败RE"),
    COMPILE_FAILED(2, "编译失败CE"),
    RUN_TIME_OUT(3, "运行超时TLE"),
    OUTPUT_LIMIT_EXCEEDED(4, "输出超限OLE"),
    COMPILE_TIMEOUT(5, "编译超时"),

    SANDBOX_ERROR(99, "沙箱底层异常");
    private String message;
    private Integer code;

    SandBoxResponseStatus() {
    }

    SandBoxResponseStatus(String message, Integer code) {
        this.message = message;
        this.code = code;
    }


    SandBoxResponseStatus(Integer code, String message){
        this.message = message;
        this.code = code;
    }
    public static SandBoxResponseStatus getEnumByValue(Integer code) {
        if (ObjectUtils.isEmpty(code)) {
            return null;
        }
        for (SandBoxResponseStatus anEnum : SandBoxResponseStatus.values()) {
            if (anEnum.code.equals(code)) {
                return anEnum;
            }
        }
        return null;
    }
    /**
     * 获取
     * @return message
     */
    public String getMessage() {
        return message;
    }
    /**
     * 获取
     * @return code
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取
     * @return message
     */
    public static String getMessage(Integer code) {
        if (ObjectUtil.isEmpty(code)){
            return null;
        }
        for (SandBoxResponseStatus srs : SandBoxResponseStatus.values()) {
            if (srs.code.equals(code)){
                return srs.message;
            }
        }
        return null;
    }
    /**
     * 获取
     * @return code
     */
    public static Integer getCode(String message) {

        if (ObjectUtil.isEmpty(message)){
            return null;
        }
        for (SandBoxResponseStatus srs : SandBoxResponseStatus.values()) {
            if (srs.message.equals(message)){
                return srs.code;
            }
        }
        return null;
    }
}
