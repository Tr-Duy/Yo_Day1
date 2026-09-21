package com.yo.day1.domain.entity;

import com.yo.day1.domain.AuditableEntity;
import com.yo.day1.domain.enums.ClassStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "course_classes")
public class CourseClass extends AuditableEntity {

    @Column(name = "class_code", nullable = false, unique = true, length = 20)
    private String classCode;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_slot_id", nullable = false)
    private ScheduleSlot scheduleSlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_teacher_id", nullable = false)
    private Teacher mainTeacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assistant_teacher_id")
    private Teacher assistantTeacher;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "max_students", nullable = false)
    private Integer maxStudents = 25;

    @Column(name = "tuition_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal tuitionFee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassStatus status = ClassStatus.OPEN;

    @Column(name = "semester_id")
    private Long semesterId = 1L;
}
