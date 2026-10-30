package com.lgg.yunpanbackendmodel.model.codesandbox;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 进程执行信息
 */

@SuperBuilder
@Data
@NoArgsConstructor
public class ExecuteCodeInfo {

    private String resultMessage;

    private String errorMessage;

    private Long time;

    private Long memory;
}
