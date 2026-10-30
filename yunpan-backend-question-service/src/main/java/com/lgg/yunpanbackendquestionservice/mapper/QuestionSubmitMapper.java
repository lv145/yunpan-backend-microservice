package com.lgg.yunpanbackendquestionservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lgg.yunpanbackendmodel.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.lgg.yunpanbackendmodel.model.entity.QuestionSubmit;
import com.lgg.yunpanbackendmodel.model.vo.QuestionSubmitVO;
import org.apache.ibatis.annotations.Param;

/**
* @author lgg
* @description 针对表【question_submit(题目提交)】的数据库操作Mapper
* @createDate 2023-08-07 20:58:53
* @Entity com.lgg.yunpan.model.entity.QuestionSubmit
*/
public interface QuestionSubmitMapper extends BaseMapper<QuestionSubmit> {

    // QuestionSubmitMapper.java
    Page<QuestionSubmitVO> listSubmitWithTitle(Page<QuestionSubmitVO> page, @Param("query") QuestionSubmitQueryRequest query);

}




