package com.lgg.yunpanbackendjudgeservice.judge;

import com.lgg.yunpanbackendjudgeservice.judge.strategy.DefaultJudgeStrategy;
import com.lgg.yunpanbackendjudgeservice.judge.strategy.JudgeContext;
import com.lgg.yunpanbackendjudgeservice.judge.strategy.JudgeStrategy;
import com.lgg.yunpanbackendmodel.model.codesandbox.JudgeInfo;
import com.lgg.yunpanbackendmodel.model.entity.QuestionSubmit;
import org.springframework.stereotype.Service;

/**
 * 判题管理（简化调用）
 */
@Service
public class JudgeManager {

    /**
     * 执行判题
     *
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext) {
        String language = judgeContext.getLanguage();
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        return judgeStrategy.doJudge(judgeContext);
    }

}
