package com.lgg.yunpanbackendmodel.model.codesandbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 判题信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeInfo {

    /**
     * 判题信息
     */
    private String message;

    /**
     * 消耗内存
     */
    private Long maxMemory;

    /**
     * 消耗时间（KB）
     */
    private Long maxTime;

    private List<ExecuteCodeInfo> executeCodeInfoList;
}
