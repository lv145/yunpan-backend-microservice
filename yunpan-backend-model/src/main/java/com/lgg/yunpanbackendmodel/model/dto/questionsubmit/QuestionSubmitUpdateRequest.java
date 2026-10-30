package com.lgg.yunpanbackendmodel.model.dto.questionsubmit;

import lombok.Data;

import java.io.Serializable;

@Data
public class QuestionSubmitUpdateRequest implements Serializable {

    private Long id;

    private String language;

    private String code;

    private static final long serialVersionUID = 1L;
}
