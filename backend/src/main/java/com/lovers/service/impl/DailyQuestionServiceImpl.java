package com.lovers.service.impl;

import com.lovers.common.exception.BusinessException;
import com.lovers.model.DailyAnswer;
import com.lovers.model.DailyQuestion;
import com.lovers.model.Question;
import com.lovers.repository.DailyAnswerRepository;
import com.lovers.repository.DailyQuestionRepository;
import com.lovers.repository.QuestionRepository;
import com.lovers.service.IActivityService;
import com.lovers.service.IDailyQuestionService;
import com.lovers.service.ILoveTreeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class DailyQuestionServiceImpl implements IDailyQuestionService {

    private static final Logger log = LoggerFactory.getLogger(DailyQuestionServiceImpl.class);

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private DailyQuestionRepository dailyQuestionRepository;

    @Autowired
    private DailyAnswerRepository dailyAnswerRepository;

    @Autowired
    private IActivityService activityService;

    @Autowired
    private ILoveTreeService loveTreeService;

    @Override
    public Map<String, Object> getTodayQuestion(Long coupleId) {
        LocalDate today = LocalDate.now();

        // 查找今天是否已有题目
        Optional<DailyQuestion> existing = dailyQuestionRepository.findByCoupleIdAndQuestionDate(coupleId, today);
        DailyQuestion dq = existing.orElseGet(() -> {
            // 没有则随机分配一道
            Question q = questionRepository.findRandomQuestion();
            if (q == null) throw new BusinessException("问题池为空，请联系管理员");

            DailyQuestion newDq = new DailyQuestion();
            newDq.setCoupleId(coupleId);
            newDq.setQuestionId(q.getId());
            newDq.setQuestionDate(today);
            return dailyQuestionRepository.save(newDq);
        });

        // 查询题目详情
        Question question = questionRepository.findById(dq.getQuestionId())
                .orElseThrow(() -> new BusinessException("题目不存在"));

        // 查询已有答案
        List<DailyAnswer> answers = dailyAnswerRepository.findByDailyQuestionId(dq.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("dailyQuestionId", dq.getId());
        result.put("questionId", question.getId());
        result.put("questionText", question.getQuestionText());
        result.put("optionA", question.getOptionA());
        result.put("optionB", question.getOptionB());
        result.put("optionC", question.getOptionC());
        result.put("optionD", question.getOptionD());
        result.put("questionType", question.getQuestionType());
        result.put("questionDate", dq.getQuestionDate().toString());
        result.put("answerCount", answers.size());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> submitAnswer(Long coupleId, Long userId, Long dailyQuestionId, String answer) {
        if (answer == null || answer.isEmpty() || !List.of("A", "B", "C", "D").contains(answer.toUpperCase())) {
            throw new BusinessException("答案格式错误，请输入 A/B/C/D");
        }

        DailyQuestion dq = dailyQuestionRepository.findById(dailyQuestionId)
                .orElseThrow(() -> new BusinessException("答题记录不存在"));

        if (!dq.getCoupleId().equals(coupleId)) {
            throw new BusinessException("无权操作");
        }

        // 检查是否已答过
        Optional<DailyAnswer> existing = dailyAnswerRepository.findByDailyQuestionIdAndUserId(dailyQuestionId, userId);
        if (existing.isPresent()) {
            throw new BusinessException("今日已作答，不能修改");
        }

        DailyAnswer da = new DailyAnswer();
        da.setDailyQuestionId(dailyQuestionId);
        da.setUserId(userId);
        da.setAnswer(answer.toUpperCase());
        dailyAnswerRepository.save(da);

        // 检查双方是否都答完了
        List<DailyAnswer> allAnswers = dailyAnswerRepository.findByDailyQuestionId(dailyQuestionId);
        boolean bothAnswered = allAnswers.size() >= 2;

        Map<String, Object> result = new HashMap<>();
        result.put("dailyQuestionId", dailyQuestionId);
        result.put("answer", answer.toUpperCase());
        result.put("bothAnswered", bothAnswered);

        if (bothAnswered) {
            // 记录成长值（完成 +2）
            try {
                loveTreeService.addGrowth(coupleId, "quiz_complete",
                        ILoveTreeService.GROWTH_QUIZ_COMPLETE, dailyQuestionId,
                        "完成双人问答");
            } catch (Exception e) {
                log.warn("Failed to add love tree growth for quiz", e);
            }

            // 记录岛屿动态
            try {
                activityService.recordActivity(coupleId, "quiz",
                        "💞 完成一次双人问答",
                        "今日默契度即将揭晓",
                        dailyQuestionId, "💞");
            } catch (Exception e) {
                log.warn("Failed to record quiz activity", e);
            }

            // 计算默契度
            String score = calculateScore(allAnswers);
            result.put("score", score);
            result.put("matched", "100%".equals(score));

            if ("100%".equals(score)) {
                try {
                    loveTreeService.addGrowth(coupleId, "quiz_match",
                            ILoveTreeService.GROWTH_QUIZ_MATCH, dailyQuestionId,
                            "双人问答默契一致");
                } catch (Exception e) {
                    log.warn("Failed to add love tree growth for quiz match", e);
                }
            }
        }

        return result;
    }

    @Override
    public Map<String, Object> getResult(Long coupleId) {
        return getResult(coupleId, null);
    }

    public Map<String, Object> getResult(Long coupleId, Long currentUserId) {
        LocalDate today = LocalDate.now();
        Optional<DailyQuestion> dqOpt = dailyQuestionRepository.findByCoupleIdAndQuestionDate(coupleId, today);
        if (dqOpt.isEmpty()) {
            return Map.of("answered", false, "message", "今天还没有答题");
        }

        DailyQuestion dq = dqOpt.get();
        Question question = questionRepository.findById(dq.getQuestionId())
                .orElseThrow(() -> new BusinessException("题目不存在"));
        List<DailyAnswer> answers = dailyAnswerRepository.findByDailyQuestionId(dq.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("answered", answers.size() >= 2);
        result.put("questionText", question.getQuestionText());
        result.put("optionA", question.getOptionA());
        result.put("optionB", question.getOptionB());
        result.put("optionC", question.getOptionC());
        result.put("optionD", question.getOptionD());

        if (answers.size() >= 2) {
            // 标记当前用户的答案
            DailyAnswer a1 = answers.get(0);
            DailyAnswer a2 = answers.get(1);
            if (currentUserId != null && a1.getUserId().equals(currentUserId)) {
                result.put("myAnswer", a1.getAnswer());
                result.put("partnerAnswer", a2.getAnswer());
            } else {
                result.put("myAnswer", a2.getAnswer());
                result.put("partnerAnswer", a1.getAnswer());
            }
            result.put("score", calculateScore(answers));
            // 成长值情况
            result.put("growthTotal", "100%".equals(calculateScore(answers))
                    ? (ILoveTreeService.GROWTH_QUIZ_COMPLETE + ILoveTreeService.GROWTH_QUIZ_MATCH)
                    : ILoveTreeService.GROWTH_QUIZ_COMPLETE);
        } else if (answers.size() == 1) {
            result.put("myAnswer", answers.get(0).getAnswer());
            result.put("waitingPartner", true);
        }

        return result;
    }

    /** 计算默契度 */
    private String calculateScore(List<DailyAnswer> answers) {
        if (answers.size() < 2) return "0%";
        String a1 = answers.get(0).getAnswer();
        String a2 = answers.get(1).getAnswer();
        return a1.equalsIgnoreCase(a2) ? "100%" : "0%";
    }
}
