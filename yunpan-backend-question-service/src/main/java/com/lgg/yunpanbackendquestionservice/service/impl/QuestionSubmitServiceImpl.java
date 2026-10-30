package com.lgg.yunpanbackendquestionservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lgg.yunpanbackendcommon.common.ErrorCode;
import com.lgg.yunpanbackendcommon.constant.CommonConstant;
import com.lgg.yunpanbackendcommon.exception.BusinessException;
import com.lgg.yunpanbackendcommon.utils.SqlUtils;
import com.lgg.yunpanbackendmodel.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.lgg.yunpanbackendmodel.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.lgg.yunpanbackendmodel.model.dto.questionsubmit.QuestionSubmitUpdateRequest;
import com.lgg.yunpanbackendmodel.model.entity.Question;
import com.lgg.yunpanbackendmodel.model.entity.QuestionSubmit;
import com.lgg.yunpanbackendmodel.model.entity.User;
import com.lgg.yunpanbackendmodel.model.enums.QuestionSubmitLanguageEnum;
import com.lgg.yunpanbackendmodel.model.enums.QuestionSubmitStatusEnum;
import com.lgg.yunpanbackendmodel.model.vo.QuestionSubmitVO;
import com.lgg.yunpanbackendmodel.model.vo.QuestionVO;
import com.lgg.yunpanbackendquestionservice.mapper.QuestionSubmitMapper;
import com.lgg.yunpanbackendquestionservice.rabbitmq.MyMessageProducer;
import com.lgg.yunpanbackendquestionservice.service.QuestionService;
import com.lgg.yunpanbackendquestionservice.service.QuestionSubmitService;
import com.lgg.yunpanbackendserviceclient.service.JudgeFeignClient;
import com.lgg.yunpanbackendserviceclient.service.UserFeignClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
* @author lgg
* @description 针对表【question_submit(题目提交)】的数据库操作Service实现
* @createDate 2023-08-07 20:58:53
*/
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
    implements QuestionSubmitService {
    
    @Resource
    private QuestionService questionService;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    @Lazy
    private JudgeFeignClient judgeFeignClient;

    @Resource
    private MyMessageProducer myMessageProducer;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateQuestionSubmit(QuestionSubmitUpdateRequest updateRequest, User loginUser) {
        if (updateRequest == null || updateRequest.getId() == null || updateRequest.getId() <= 0
                || QuestionSubmitLanguageEnum.getEnumByLanguage(updateRequest.getLanguage()) == null
                || StringUtils.isBlank(updateRequest.getCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提交编号、编程语言或代码不合法");
        }
        QuestionSubmit oldSubmit = getEditableQuestionSubmit(updateRequest.getId(), loginUser);
        if (questionService.getById(oldSubmit.getQuestionId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setId(oldSubmit.getId());
        questionSubmit.setCode(updateRequest.getCode());
        questionSubmit.setLanguage(updateRequest.getLanguage());
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        questionSubmit.setJudgeInfo("{}");
        questionSubmit.setJudgeStatus("");
        if (!updateById(questionSubmit)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提交记录更新失败");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    myMessageProducer.sendMessage("code_exchange", "my_routingKey", String.valueOf(oldSubmit.getId()));
                } catch (RuntimeException exception) {
                    QuestionSubmit failedSubmit = new QuestionSubmit();
                    failedSubmit.setId(oldSubmit.getId());
                    failedSubmit.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
                    failedSubmit.setJudgeStatus("重判消息发送失败，请稍后重新更新");
                    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    transactionTemplate.execute(status -> getBaseMapper().updateById(failedSubmit));
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新已保存，但重判消息发送失败，请重试");
                }
            }
        });
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteQuestionSubmit(long id, User loginUser) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        getEditableQuestionSubmit(id, loginUser);
        if (!removeById(id)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提交记录删除失败");
        }
        return true;
    }

    private QuestionSubmit getEditableQuestionSubmit(long id, User loginUser) {
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        QuestionSubmit questionSubmit = getOne(new QueryWrapper<QuestionSubmit>().eq("id", id).last("FOR UPDATE"));
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!loginUser.getId().equals(questionSubmit.getUserId()) && !userFeignClient.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        if (Integer.valueOf(QuestionSubmitStatusEnum.WAITING.getValue()).equals(questionSubmit.getStatus())
                || Integer.valueOf(QuestionSubmitStatusEnum.RUNNING.getValue()).equals(questionSubmit.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "等待判题或判题中的记录暂不能更新、删除");
        }
        return questionSubmit;
    }

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {
        // 校验编程语言是否合法
        String language = questionSubmitAddRequest.getLanguage();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByLanguage(language);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编程语言错误");
        }
        long questionId = questionSubmitAddRequest.getQuestionId();
        // 判断实体是否存在，根据类别获取实体
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 是否已提交题目
        long userId = loginUser.getId();
        // 每个用户串行提交题目
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setUserId(userId);
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setCode(questionSubmitAddRequest.getCode());
        questionSubmit.setLanguage(language);
        // 设置初始状态
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        questionSubmit.setJudgeInfo("{}");
        boolean save = this.save(questionSubmit);
        if (!save){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据插入失败");
        }
        Long questionSubmitId = questionSubmit.getId();
        // 发送消息
        myMessageProducer.sendMessage("code_exchange", "my_routingKey", String.valueOf(questionSubmitId));
        // 执行判题服务
//        CompletableFuture.runAsync(() -> {
//            judgeFeignClient.doJudge(questionSubmitId);
//        });
        return questionSubmitId;
    }


    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象，得到 mybatis 框架支持的查询 QueryWrapper 类）
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return queryWrapper;
        }
        String language = questionSubmitQueryRequest.getLanguage();
        Integer status = questionSubmitQueryRequest.getStatus();
        Long questionId = questionSubmitQueryRequest.getQuestionId();
        Long userId = questionSubmitQueryRequest.getUserId();
        String sortField = questionSubmitQueryRequest.getSortField();
        String sortOrder = questionSubmitQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(StringUtils.isNotBlank(language), "language", language);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(QuestionSubmitStatusEnum.getEnumByValue(status) != null, "status", status);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);

        return queryWrapper;
    }

    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser) {
        QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
        // 脱敏：仅本人和管理员能看见自己（提交 userId 和登录用户 id 不同）提交的代码
        long userId = loginUser.getId();
        // 处理脱敏
        if (userId != questionSubmit.getUserId() && !userFeignClient.isAdmin(loginUser)) {
            questionSubmitVO.setCode(null);
        }
        Question question = questionService.getById(questionSubmitVO.getQuestionId());
        QuestionVO questionVO= QuestionVO.objToVo(question);
        questionSubmitVO.setQuestionVO(questionVO);
        return questionSubmitVO;
    }

    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser) {
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVO> questionSubmitVoPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());
        if (CollectionUtils.isEmpty(questionSubmitList)) {
            return questionSubmitVoPage;
        }
        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream()
                .map(questionSubmit -> getQuestionSubmitVO(questionSubmit, loginUser))
                .collect(Collectors.toList());
        questionSubmitVoPage.setRecords(questionSubmitVOList);
        return questionSubmitVoPage;
    }


}


