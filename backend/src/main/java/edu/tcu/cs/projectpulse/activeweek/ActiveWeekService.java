package edu.tcu.cs.projectpulse.activeweek;

import edu.tcu.cs.projectpulse.activeweek.dto.ActiveWeekRequest;
import edu.tcu.cs.projectpulse.activeweek.dto.ActiveWeekResponse;
import edu.tcu.cs.projectpulse.section.SectionEntity;
import edu.tcu.cs.projectpulse.section.SectionNotFoundException;
import edu.tcu.cs.projectpulse.section.SectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ActiveWeekService {

    private final ActiveWeekRepository activeWeekRepository;
    private final SectionRepository sectionRepository;

    public ActiveWeekService(ActiveWeekRepository activeWeekRepository, SectionRepository sectionRepository) {
        this.activeWeekRepository = activeWeekRepository;
        this.sectionRepository = sectionRepository;
    }

    public ActiveWeekResponse getActiveWeeks(Long sectionId) {
        SectionEntity section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new SectionNotFoundException(sectionId));

        List<ActiveWeekEntity> all = activeWeekRepository.findBySectionIdOrderByWeekStartDateAsc(sectionId);

        List<LocalDate> activeWeeks = all.stream()
                .filter(ActiveWeekEntity::isActive)
                .map(ActiveWeekEntity::getWeekStartDate)
                .toList();

        List<LocalDate> configuredWeeks = all.stream()
                .map(ActiveWeekEntity::getWeekStartDate)
                .toList();

        return new ActiveWeekResponse(section.getId(), activeWeeks, configuredWeeks);
    }

    private List<LocalDate> computeAllMondays(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> mondays = new ArrayList<>();
        LocalDate current = startDate;
        while (current.getDayOfWeek() != DayOfWeek.MONDAY) {
            current = current.plusDays(1);
        }
        while (!current.isAfter(endDate)) {
            mondays.add(current);
            current = current.plusWeeks(1);
        }
        return mondays;
    }

    @Transactional
    public ActiveWeekResponse saveActiveWeeks(Long sectionId, ActiveWeekRequest request) {
        SectionEntity section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new SectionNotFoundException(sectionId));

        if (section.getStartDate() == null || section.getEndDate() == null) {
            throw new IllegalArgumentException(
                    "Cannot set up active weeks for section '" + section.getName() +
                    "' because it does not have a start and end date. Please edit the section first.");
        }

        List<LocalDate> dates = request.activeWeekDates().stream().distinct().toList();

        for (LocalDate date : dates) {
            if (date.getDayOfWeek() != DayOfWeek.MONDAY) {
                throw new IllegalArgumentException(
                        "All active week dates must be Mondays. '" + date + "' is a " + date.getDayOfWeek() + ".");
            }
            if (section.getStartDate() != null && date.isBefore(section.getStartDate())) {
                throw new IllegalArgumentException(
                        "Date '" + date + "' is before the section start date '" + section.getStartDate() + "'.");
            }
            if (section.getEndDate() != null && date.isAfter(section.getEndDate())) {
                throw new IllegalArgumentException(
                        "Date '" + date + "' is after the section end date '" + section.getEndDate() + "'.");
            }
        }

        activeWeekRepository.deleteBySectionId(sectionId);
        activeWeekRepository.flush();

        Set<LocalDate> activeSet = new HashSet<>(dates);
        List<LocalDate> allMondays = computeAllMondays(section.getStartDate(), section.getEndDate());

        List<ActiveWeekEntity> entities = allMondays.stream().map(monday -> {
            ActiveWeekEntity entity = new ActiveWeekEntity();
            entity.setSectionId(sectionId);
            entity.setWeekStartDate(monday);
            entity.setActive(activeSet.contains(monday));
            return entity;
        }).toList();

        activeWeekRepository.saveAll(entities);

        List<LocalDate> savedActive = allMondays.stream()
                .filter(activeSet::contains)
                .toList();

        return new ActiveWeekResponse(sectionId, savedActive, allMondays);
    }
}
