package edu.tcu.cs.projectpulse.peerevaluation;

import edu.tcu.cs.projectpulse.peerevaluation.dto.CriterionScoreRequest;
import edu.tcu.cs.projectpulse.peerevaluation.dto.EvaluationEntryRequest;
import edu.tcu.cs.projectpulse.peerevaluation.dto.PeerEvalSubmissionResponse;
import edu.tcu.cs.projectpulse.peerevaluation.dto.SubmitPeerEvalRequest;
import edu.tcu.cs.projectpulse.rubric.CriterionEntity;
import edu.tcu.cs.projectpulse.rubric.RubricRepository;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class PeerEvaluationService {

    private final PeerEvaluationRepository peerEvaluationRepository;
    private final UserRepository userRepository;
    private final RubricRepository rubricRepository;

    public PeerEvaluationService(PeerEvaluationRepository peerEvaluationRepository,
                                 UserRepository userRepository,
                                 RubricRepository rubricRepository) {
        this.peerEvaluationRepository = peerEvaluationRepository;
        this.userRepository = userRepository;
        this.rubricRepository = rubricRepository;
    }

    @Transactional(readOnly = true)
    public Optional<PeerEvalSubmissionResponse> findMySubmission(Long evaluatorId, Integer week) {
        return peerEvaluationRepository.findFirstByEvaluatorIdAndWeek(evaluatorId, week)
                .map(e -> new PeerEvalSubmissionResponse(e.getId(), e.getWeek(), e.getEvaluator().getId()));
    }

    // BR-3: once submitted for a week, cannot resubmit
    public void submit(Long evaluatorId, SubmitPeerEvalRequest request) {
        if (peerEvaluationRepository.existsByEvaluatorIdAndWeek(evaluatorId, request.week())) {
            throw new AlreadySubmittedException("Peer evaluation for week " + request.week() + " has already been submitted.");
        }

        UserEntity evaluator = userRepository.findById(evaluatorId)
                .orElseThrow(() -> new RuntimeException("User not found with id " + evaluatorId));

        for (EvaluationEntryRequest entry : request.evaluations()) {
            if (entry.evaluateeId().equals(evaluatorId)) {
                throw new RuntimeException("Students cannot submit a peer evaluation for themselves.");
            }

            UserEntity evaluatee = userRepository.findById(entry.evaluateeId())
                    .orElseThrow(() -> new RuntimeException("User not found with id " + entry.evaluateeId()));

            PeerEvaluationEntity eval = new PeerEvaluationEntity();
            eval.setEvaluator(evaluator);
            eval.setEvaluatee(evaluatee);
            eval.setWeek(request.week());
            eval.setPublicComment(entry.publicComment());
            eval.setPrivateComment(entry.privateComment());

            for (CriterionScoreRequest cs : entry.criterionScores()) {
                CriterionEntity criterion = rubricRepository.findAll().stream()
                        .flatMap(r -> r.getCriteria().stream())
                        .filter(c -> c.getId().equals(cs.criterionId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Criterion not found with id " + cs.criterionId()));

                CriterionScoreEntity scoreEntity = new CriterionScoreEntity();
                scoreEntity.setCriterion(criterion);
                scoreEntity.setScore(cs.score());
                eval.addCriterionScore(scoreEntity);
            }

            peerEvaluationRepository.save(eval);
        }
    }
}
