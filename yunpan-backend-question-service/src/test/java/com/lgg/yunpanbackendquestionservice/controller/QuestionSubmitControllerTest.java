package com.lgg.yunpanbackendquestionservice.controller;

import com.lgg.yunpanbackendcommon.common.ErrorCode;
import com.lgg.yunpanbackendcommon.exception.BusinessException;
import com.lgg.yunpanbackendmodel.model.entity.QuestionSubmit;
import com.lgg.yunpanbackendmodel.model.entity.User;
import com.lgg.yunpanbackendmodel.model.vo.QuestionSubmitVO;
import com.lgg.yunpanbackendquestionservice.service.QuestionSubmitService;
import com.lgg.yunpanbackendserviceclient.service.UserFeignClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuestionSubmitControllerTest {

    private QuestionController controller;
    private QuestionSubmitService service;
    private UserFeignClient userFeignClient;
    private HttpServletRequest request;
    private User loginUser;
    private QuestionSubmit submission;

    @BeforeEach
    void setUp() {
        controller = new QuestionController();
        service = mock(QuestionSubmitService.class);
        userFeignClient = mock(UserFeignClient.class);
        request = mock(HttpServletRequest.class);
        ReflectionTestUtils.setField(controller, "questionSubmitService", service);
        ReflectionTestUtils.setField(controller, "userFeignClient", userFeignClient);
        loginUser = new User();
        loginUser.setId(10L);
        submission = new QuestionSubmit();
        submission.setId(100L);
        submission.setUserId(10L);
        when(userFeignClient.getLoginUser(request)).thenReturn(loginUser);
        when(service.getById(100L)).thenReturn(submission);
    }

    @Test
    void ownerCanLoadEditableDetails() {
        QuestionSubmitVO view = new QuestionSubmitVO();
        view.setCode("class Main {}");
        when(service.getQuestionSubmitVO(submission, loginUser)).thenReturn(view);
        assertSame(view, controller.getQuestionSubmitVOById(100L, request).getData());
    }

    @Test
    void strangersCannotLoadSourceCode() {
        loginUser.setId(99L);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.getQuestionSubmitVOById(100L, request));
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        verify(service, never()).getQuestionSubmitVO(any(), any());
    }

    @Test
    void adminsCanLoadEditableDetails() {
        loginUser.setId(99L);
        when(userFeignClient.isAdmin(loginUser)).thenReturn(true);
        controller.getQuestionSubmitVOById(100L, request);
        verify(service).getQuestionSubmitVO(submission, loginUser);
    }

    @Test
    void missingRecordReturnsNotFound() {
        when(service.getById(100L)).thenReturn(null);
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), assertThrows(BusinessException.class,
                () -> controller.getQuestionSubmitVOById(100L, request)).getCode());
    }

    @Test
    void invalidRequestsAreRejected() {
        assertThrows(BusinessException.class, () -> controller.getQuestionSubmitVOById(0, request));
        assertThrows(BusinessException.class, () -> controller.deleteQuestionSubmit(null, request));
        assertThrows(BusinessException.class, () -> controller.listQuestionSubmitByPage(null, request));
    }
}
