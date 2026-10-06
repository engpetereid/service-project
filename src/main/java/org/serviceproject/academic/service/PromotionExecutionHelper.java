package org.serviceproject.academic.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.academic.repository.PromotionRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionExecutionHelper {

    private final PromotionRunRepository promotionRunRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long runId) {
        if (runId == null) return;
        promotionRunRepository.findById(runId).ifPresent(run -> {
            run.markFailed();
            promotionRunRepository.save(run);
            log.info("Persisted promotion run {} failure state in REQUIRES_NEW transaction", runId);
        });
    }
}
