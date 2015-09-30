package com.techlooper.cron;

import com.techlooper.entity.ChallengeEntity;
import com.techlooper.model.ChallengePhaseEnum;
import com.techlooper.service.ChallengeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Service
public class DailyChallengeSummaryEmailSender {

    private final static Logger LOGGER = LoggerFactory.getLogger(DailyChallengeSummaryEmailSender.class);

    @Resource
    private ChallengeService challengeService;

    @Scheduled(cron = "${scheduled.cron.dailyChallengeSummary}")
    public void notifyRegistrantAboutChallengeTimeline() throws Exception {
        List<ChallengePhaseEnum> challengePhases = Arrays.asList(ChallengePhaseEnum.REGISTRATION, ChallengePhaseEnum.IN_PROGRESS);

        for (ChallengePhaseEnum challengePhase : challengePhases) {
            List<ChallengeEntity> challengeEntities = challengeService.listChallengesByPhase(challengePhase);

            for (ChallengeEntity challengeEntity : challengeEntities) {
                try {
                    if (challengeEntity.getChallengeId() == 1442478937708L)
                        challengeService.sendDailySummaryEmailToChallengeOwner(challengeEntity);
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        }
    }
}
