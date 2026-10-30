package com.lgg.yunpanbackendquestionservice.controller.inner;

import com.lgg.yunpanbackendmodel.model.entity.Question;
import com.lgg.yunpanbackendmodel.model.entity.QuestionSubmit;
import com.lgg.yunpanbackendquestionservice.service.QuestionService;
import com.lgg.yunpanbackendquestionservice.service.QuestionSubmitService;
import com.lgg.yunpanbackendserviceclient.service.QuestionFeignClient;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 该服务仅内部调用，不是给前端的
 */
@RestController
@RequestMapping("/inner")
public class QuestionInnerController implements QuestionFeignClient {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Override
    public Boolean updateQuestionById(Question question) {
        return questionService.updateById(question);
    }

    @GetMapping("/get/id")
    @Override
    public Question getQuestionById(@RequestParam("questionId") long questionId) {
        return questionService.getById(questionId);
    }

    @GetMapping("/question_submit/get/id")
    @Override
    public QuestionSubmit getQuestionSubmitById(@RequestParam("questionId") long questionSubmitId) {
        return questionSubmitService.getById(questionSubmitId);
    }

    @PostMapping("/question_submit/update")
    @Override
    public Boolean updateQuestionSubmitById(@RequestBody QuestionSubmit questionSubmit) {
        return questionSubmitService.updateById(questionSubmit);
    }

}
