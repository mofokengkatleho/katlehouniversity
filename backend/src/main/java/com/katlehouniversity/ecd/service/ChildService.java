package com.katlehouniversity.ecd.service;

import com.katlehouniversity.ecd.dto.ChildDto;
import com.katlehouniversity.ecd.entity.Child;
import com.katlehouniversity.ecd.exception.ResourceNotFoundException;
import com.katlehouniversity.ecd.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChildService {

    private final ChildRepository childRepository;

    /**
     * Generate a unique student number in format STU-YYYY-NNN
     * Example: STU-2025-001, STU-2025-002
     */
    private String generateStudentNumber(String academicYear) {
        String year = academicYear != null ? academicYear : String.valueOf(LocalDate.now().getYear());
        String prefix = "STU-" + year + "-";

        // Find the highest sequence number for this academic year
        List<Child> studentsInYear = childRepository.findByAcademicYear(year);

        int maxSequence = studentsInYear.stream()
                .map(Child::getStudentNumber)
                .filter(num -> num != null && num.startsWith(prefix))
                .map(num -> num.substring(prefix.length()))
                .filter(seq -> seq.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);

        int nextSequence = maxSequence + 1;
        return String.format("%s%03d", prefix, nextSequence);
    }

    @Transactional(readOnly = true)
    public List<ChildDto> getAllChildren() {
        return childRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChildDto> getActiveChildren() {
        return childRepository.findByActiveTrue().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChildDto getChildById(Long id) {
        Child child = childRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Child not found with id: " + id));
        return toDto(child);
    }

    @Transactional(readOnly = true)
    public ChildDto getChildByPaymentReference(String reference) {
        Child child = childRepository.findByPaymentReferenceIgnoreCase(reference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Child not found with payment reference: " + reference));
        return toDto(child);
    }

    @Transactional(readOnly = true)
    public List<ChildDto> searchChildren(String name) {
        return childRepository.searchByName(name).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChildDto createChild(ChildDto dto) {
        log.info("Creating new student: {} {}", dto.getFirstName(), dto.getLastName());

        // Set default academic year if not provided
        String academicYear = dto.getAcademicYear() != null ?
                dto.getAcademicYear() : String.valueOf(LocalDate.now().getYear());

        // Generate student number
        String studentNumber = generateStudentNumber(academicYear);
        log.info("Generated student number: {}", studentNumber);

        Child child = Child.builder()
                .studentNumber(studentNumber)
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .gender(dto.getGender() != null ? Child.Gender.valueOf(dto.getGender()) : null)
                .studentIdNumber(dto.getStudentIdNumber())
                .physicalAddress(dto.getPhysicalAddress())
                .allergies(dto.getAllergies())
                .paymentReference(studentNumber) // Use student number as payment reference
                .monthlyFee(dto.getMonthlyFee())
                .paymentDay(dto.getPaymentDay())
                .parentPhone(dto.getParentPhone())
                .parentEmail(dto.getParentEmail())
                .guardianEmail(dto.getGuardianEmail())
                .parentName(dto.getParentName())
                .gradeClass(dto.getGradeClass())
                .academicYear(academicYear)
                .dateOfBirth(dto.getDateOfBirth())
                .enrollmentDate(dto.getEnrollmentDate() != null ? dto.getEnrollmentDate() : LocalDate.now())
                .status(Child.StudentStatus.ACTIVE)
                .notes(dto.getNotes())
                .build();

        child = childRepository.save(child);
        log.info("Student created successfully with id: {} and student number: {}", child.getId(), child.getStudentNumber());

        return toDto(child);
    }

    @Transactional
    public ChildDto updateChild(Long id, ChildDto dto) {
        log.info("Updating student with id: {}", id);

        Child child = childRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        child.setFirstName(dto.getFirstName());
        child.setLastName(dto.getLastName());
        if (dto.getGender() != null) {
            child.setGender(Child.Gender.valueOf(dto.getGender()));
        }
        child.setStudentIdNumber(dto.getStudentIdNumber());
        child.setPhysicalAddress(dto.getPhysicalAddress());
        child.setAllergies(dto.getAllergies());
        child.setMonthlyFee(dto.getMonthlyFee());
        child.setPaymentDay(dto.getPaymentDay());
        child.setParentPhone(dto.getParentPhone());
        child.setParentEmail(dto.getParentEmail());
        child.setGuardianEmail(dto.getGuardianEmail());
        child.setParentName(dto.getParentName());
        child.setGradeClass(dto.getGradeClass());
        child.setDateOfBirth(dto.getDateOfBirth());
        child.setEnrollmentDate(dto.getEnrollmentDate());
        if (dto.getStatus() != null) {
            child.setStatus(Child.StudentStatus.valueOf(dto.getStatus()));
        }
        child.setNotes(dto.getNotes());

        child = childRepository.save(child);
        log.info("Student updated successfully: {}", id);

        return toDto(child);
    }

    @Transactional
    public void deleteChild(Long id) {
        log.info("Deleting student with id: {}", id);

        Child child = childRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        child.setStatus(Child.StudentStatus.WITHDRAWN);
        childRepository.save(child);

        log.info("Student withdrawn successfully: {}", id);
    }

    @Transactional(readOnly = true)
    public long getActiveChildrenCount() {
        return childRepository.countActiveChildren();
    }

    private ChildDto toDto(Child child) {
        return ChildDto.builder()
                .id(child.getId())
                .studentNumber(child.getStudentNumber())
                .firstName(child.getFirstName())
                .lastName(child.getLastName())
                .gender(child.getGender() != null ? child.getGender().name() : null)
                .studentIdNumber(child.getStudentIdNumber())
                .physicalAddress(child.getPhysicalAddress())
                .allergies(child.getAllergies())
                .paymentReference(child.getPaymentReference())
                .monthlyFee(child.getMonthlyFee())
                .paymentDay(child.getPaymentDay())
                .parentPhone(child.getParentPhone())
                .parentEmail(child.getParentEmail())
                .guardianEmail(child.getGuardianEmail())
                .parentName(child.getParentName())
                .gradeClass(child.getGradeClass())
                .academicYear(child.getAcademicYear())
                .dateOfBirth(child.getDateOfBirth())
                .enrollmentDate(child.getEnrollmentDate())
                .status(child.getStatus() != null ? child.getStatus().name() : null)
                .active(child.getStatus() == Child.StudentStatus.ACTIVE)
                .notes(child.getNotes())
                .build();
    }
}