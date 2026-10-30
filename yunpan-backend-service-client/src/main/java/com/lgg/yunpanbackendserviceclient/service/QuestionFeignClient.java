package com.lgg.yunpanbackendserviceclient.service;


import com.google.gson.JsonObject;
import com.lgg.yunpanbackendmodel.model.entity.Question;
import com.lgg.yunpanbackendmodel.model.entity.QuestionSubmit;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
* @author lgg
* @description 针对表【question(题目)】的数据库操作Service
* @createDate 2023-08-07 20:58:00
*/
@FeignClient(name = "yunpan-backend-question-service", path = "/api/question/inner")
public interface QuestionFeignClient {
    @PostMapping("/question/update")
    Boolean updateQuestionById(@RequestBody Question question);
    @GetMapping("/get/id")
    Question getQuestionById(@RequestParam("questionId") long questionId);

    @GetMapping("/question_submit/get/id")
    QuestionSubmit getQuestionSubmitById(@RequestParam("questionId") long questionSubmitId);

    @PostMapping("/question_submit/update")
    Boolean updateQuestionSubmitById(@RequestBody QuestionSubmit questionSubmit);

}
