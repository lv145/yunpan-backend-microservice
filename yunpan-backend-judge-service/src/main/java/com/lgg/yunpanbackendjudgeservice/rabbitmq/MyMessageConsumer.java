package com.lgg.yunpanbackendjudgeservice.rabbitmq;

import cn.hutool.json.JSONUtil;
import com.lgg.yunpanbackendmodel.model.codesandbox.JudgeInfo;
import com.lgg.yunpanbackendmodel.model.entity.Question;
import com.lgg.yunpanbackendmodel.model.entity.QuestionSubmit;
import com.lgg.yunpanbackendmodel.model.enums.JudgeInfoMessageEnum;
import com.lgg.yunpanbackendmodel.model.enums.QuestionSubmitStatusEnum;
import com.lgg.yunpanbackendserviceclient.service.QuestionFeignClient;
import com.rabbitmq.client.Channel;
import com.lgg.yunpanbackendjudgeservice.judge.JudgeService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class MyMessageConsumer {

    @Resource
    private JudgeService judgeService;
    @Resource
    private QuestionFeignClient questionFeignClient;
    // 指定程序监听的消息队列和确认机制
    @SneakyThrows
    @RabbitListener(queues = {"code_queue"}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receiveMessage message = {}", message);
        long questionSubmitId = Long.parseLong(message);
        try {
            QuestionSubmit questionSubmit = judgeService.doJudge(questionSubmitId);
            Question question = questionFeignClient.getQuestionById(questionSubmit.getQuestionId());
            JudgeInfo judgeInfo = JSONUtil.toBean(questionSubmit.getJudgeInfo(), JudgeInfo.class);
            if(JudgeInfoMessageEnum.ACCEPTED.getValue().equals(judgeInfo.getMessage())){
                question.setAcceptedNum(question.getAcceptedNum()+1);
            }
            Integer submitNum = question.getSubmitNum();
            question.setSubmitNum(submitNum+1);
            questionFeignClient.updateQuestionById(question);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("判题失败:{}", e.getMessage());
            QuestionSubmit questionSubmit = new QuestionSubmit();
            questionSubmit.setId(questionSubmitId);
            questionSubmit.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
            questionFeignClient.updateQuestionSubmitById(questionSubmit);
            channel.basicNack(deliveryTag, false, false);
        }
    }

}