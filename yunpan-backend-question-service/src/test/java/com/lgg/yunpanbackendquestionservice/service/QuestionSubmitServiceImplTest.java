package com.lgg.yunpanbackendquestionservice.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.lgg.yunpanbackendcommon.common.ErrorCode;
import com.lgg.yunpanbackendcommon.exception.BusinessException;
import com.lgg.yunpanbackendmodel.model.dto.questionsubmit.QuestionSubmitUpdateRequest;
import com.lgg.yunpanbackendmodel.model.entity.Question;
import com.lgg.yunpanbackendmodel.model.entity.QuestionSubmit;
import com.lgg.yunpanbackendmodel.model.entity.User;
import com.lgg.yunpanbackendmodel.model.vo.QuestionSubmitVO;
import com.lgg.yunpanbackendquestionservice.mapper.QuestionSubmitMapper;
import com.lgg.yunpanbackendquestionservice.rabbitmq.MyMessageProducer;
import com.lgg.yunpanbackendquestionservice.service.impl.QuestionSubmitServiceImpl;
import com.lgg.yunpanbackendserviceclient.service.UserFeignClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QuestionSubmitServiceImplTest {

    private QuestionSubmitServiceImpl service;
    private QuestionService questionService;
    private UserFeignClient userFeignClient;
    private MyMessageProducer producer;
    private QuestionSubmitMapper mapper;
    private PlatformTransactionManager transactionManager;
    private User owner;
    private QuestionSubmit submission;

    @BeforeEach
    void setUp() {
        service = spy(new QuestionSubmitServiceImpl());
        questionService = mock(QuestionService.class);
        userFeignClient = mock(UserFeignClient.class);
        producer = mock(MyMessageProducer.class);
        mapper = mock(QuestionSubmitMapper.class);
        transactionManager = mock(PlatformTransactionManager.class);
        ReflectionTestUtils.setField(service, "questionService", questionService);
        ReflectionTestUtils.setField(service, "userFeignClient", userFeignClient);
        ReflectionTestUtils.setField(service, "myMessageProducer", producer);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "transactionManager", transactionManager);
        owner = new User();
        owner.setId(10L);
        submission = new QuestionSubmit();
        submission.setId(100L);
        submission.setUserId(10L);
        submission.setQuestionId(20L);
        submission.setStatus(2);
        doReturn(submission).when(service).getOne(any(Wrapper.class));
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private QuestionSubmitUpdateRequest updateRequest() {
        QuestionSubmitUpdateRequest request = new QuestionSubmitUpdateRequest();
        request.setId(100L);
        request.setLanguage("java");
        request.setCode("class Main {}");
        return request;
    }

    @Test
    void ownerCanUpdateAndQueueOnlyAfterCommit() {
        when(questionService.getById(20L)).thenReturn(new Question());
        doReturn(true).when(service).updateById(any(QuestionSubmit.class));
        TransactionSynchronizationManager.initSynchronization();
        assertTrue(service.updateQuestionSubmit(updateRequest(), owner));
        ArgumentCaptor<QuestionSubmit> captor = ArgumentCaptor.forClass(QuestionSubmit.class);
        verify(service).updateById(captor.capture());
        QuestionSubmit updated = captor.getValue();
        assertEquals(100L, updated.getId());
        assertEquals(0, updated.getStatus());
        assertEquals("{}", updated.getJudgeInfo());
        assertEquals("", updated.getJudgeStatus());
        assertEquals("class Main {}", updated.getCode());
        assertNull(updated.getUserId());
        assertNull(updated.getQuestionId());
        verifyNoInteractions(producer);
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(producer).sendMessage("code_exchange", "my_routingKey", "100");
    }

    @Test
    void queueFailureMarksSubmissionFailedInNewTransaction() {
        when(questionService.getById(20L)).thenReturn(new Question());
        doReturn(true).when(service).updateById(any(QuestionSubmit.class));
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        doThrow(new IllegalStateException("queue unavailable")).when(producer).sendMessage(anyString(), anyString(), anyString());
        TransactionSynchronizationManager.initSynchronization();
        service.updateQuestionSubmit(updateRequest(), owner);
        TransactionSynchronization synchronization = TransactionSynchronizationManager.getSynchronizations().get(0);
        assertThrows(BusinessException.class, synchronization::afterCommit);
        ArgumentCaptor<QuestionSubmit> captor = ArgumentCaptor.forClass(QuestionSubmit.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(3, captor.getValue().getStatus());
        verify(transactionManager).commit(any());
    }

    @Test
    void otherUsersCannotUpdateOrDelete() {
        User stranger = new User();
        stranger.setId(99L);
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), assertThrows(BusinessException.class,
                () -> service.updateQuestionSubmit(updateRequest(), stranger)).getCode());
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), assertThrows(BusinessException.class,
                () -> service.deleteQuestionSubmit(100L, stranger)).getCode());
        verifyNoInteractions(producer);
    }

    @Test
    void activeSubmissionsCannotBeUpdatedOrDeleted() {
        for (int status = 0; status <= 1; status++) {
            submission.setStatus(status);
            assertThrows(BusinessException.class, () -> service.updateQuestionSubmit(updateRequest(), owner));
            assertThrows(BusinessException.class, () -> service.deleteQuestionSubmit(100L, owner));
        }
        verifyNoInteractions(producer);
    }

    @Test
    void rejectsInvalidLanguageAndBlankCode() {
        QuestionSubmitUpdateRequest request = updateRequest();
        request.setLanguage("html");
        assertThrows(BusinessException.class, () -> service.updateQuestionSubmit(request, owner));
        request.setLanguage("java");
        request.setCode("  ");
        assertThrows(BusinessException.class, () -> service.updateQuestionSubmit(request, owner));
        assertThrows(BusinessException.class, () -> service.updateQuestionSubmit(null, owner));
    }

    @Test
    void ownerAndAdminCanDeleteCompletedSubmission() {
        doReturn(true).when(service).removeById(100L);
        assertTrue(service.deleteQuestionSubmit(100L, owner));
        User admin = new User();
        admin.setId(99L);
        when(userFeignClient.isAdmin(admin)).thenReturn(true);
        assertTrue(service.deleteQuestionSubmit(100L, admin));
        verify(service, times(2)).removeById(100L);
    }

    @Test
    void missingRecordAndLoggedOutUserAreRejected() {
        doReturn(null).when(service).getOne(any(Wrapper.class));
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), assertThrows(BusinessException.class,
                () -> service.deleteQuestionSubmit(100L, owner)).getCode());
        assertEquals(ErrorCode.NOT_LOGIN_ERROR.getCode(), assertThrows(BusinessException.class,
                () -> service.deleteQuestionSubmit(100L, null)).getCode());
    }

    @Test
    void sandboxMessageIsIncludedInViewObject() {
        submission.setJudgeStatus("运行成功");
        submission.setJudgeInfo("{\"message\":\"Accepted\",\"executeCodeInfoList\":[{\"time\":12,\"memory\":64}]}");
        QuestionSubmitVO view = QuestionSubmitVO.objToVo(submission);
        assertEquals("运行成功", view.getJudgeStatus());
        assertEquals(12L, view.getJudgeInfo().getExecuteCodeInfoList().get(0).getTime());
    }
}
