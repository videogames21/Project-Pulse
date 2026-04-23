package edu.tcu.cs.projectpulse.war;

import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserRepository;
import edu.tcu.cs.projectpulse.war.dto.WAREntryRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WAREntryService {

    private final WAREntryRepository warEntryRepository;
    private final UserRepository userRepository;

    public WAREntryService(WAREntryRepository warEntryRepository, UserRepository userRepository) {
        this.warEntryRepository = warEntryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<WAREntryEntity> findByStudentAndWeek(Long studentId, Integer week) {
        return warEntryRepository.findByStudentIdAndWeekOrderByIdAsc(studentId, week);
    }

    public WAREntryEntity create(Long studentId, WAREntryRequest request) {
        UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("User not found with id " + studentId));
        WAREntryEntity entry = new WAREntryEntity();
        entry.setStudent(student);
        applyRequest(entry, request);
        return warEntryRepository.save(entry);
    }

    public WAREntryEntity update(Long entryId, Long studentId, WAREntryRequest request) {
        WAREntryEntity entry = warEntryRepository.findById(entryId)
                .orElseThrow(() -> new WAREntryNotFoundException(entryId));
        if (!entry.getStudent().getId().equals(studentId)) {
            throw new RuntimeException("Not authorized to edit this WAR entry");
        }
        applyRequest(entry, request);
        return warEntryRepository.save(entry);
    }

    public void delete(Long entryId, Long studentId) {
        WAREntryEntity entry = warEntryRepository.findById(entryId)
                .orElseThrow(() -> new WAREntryNotFoundException(entryId));
        if (!entry.getStudent().getId().equals(studentId)) {
            throw new RuntimeException("Not authorized to delete this WAR entry");
        }
        warEntryRepository.delete(entry);
    }

    private void applyRequest(WAREntryEntity entry, WAREntryRequest req) {
        entry.setWeek(req.week());
        entry.setCategory(req.category());
        entry.setDescription(req.description());
        entry.setPlannedHours(req.plannedHours());
        entry.setActualHours(req.actualHours());
        entry.setStatus(req.status());
    }
}
