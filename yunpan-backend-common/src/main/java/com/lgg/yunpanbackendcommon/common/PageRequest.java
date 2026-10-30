package com.lgg.yunpanbackendcommon.common;

import lombok.Data;
import com.lgg.yunpanbackendcommon.constant.CommonConstant;

/**
 * 分页请求
 *
 * @author lgg
 * @from 云盘在线判题平台
 */
@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private long current = 1;

    /**
     * 页面大小
     */
    private long pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认升序）
     */
    private String sortOrder = CommonConstant.SORT_ORDER_ASC;
}
