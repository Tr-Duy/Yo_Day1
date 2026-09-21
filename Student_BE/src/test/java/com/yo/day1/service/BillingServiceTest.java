package com.yo.day1.service;

import com.yo.day1.common.exception.BadRequestException;
import com.yo.day1.domain.entity.CourseClass;
import com.yo.day1.domain.entity.Student;
import com.yo.day1.domain.entity.TuitionInvoice;
import com.yo.day1.domain.enums.InvoiceStatus;
import com.yo.day1.dto.Billing.InvoiceCreateRequest;
import com.yo.day1.dto.Billing.InvoiceResponse;
import com.yo.day1.repository.NotificationRepository;
import com.yo.day1.repository.PaymentRepository;
import com.yo.day1.repository.PromotionRepository;
import com.yo.day1.repository.TuitionInvoiceRepository;
import com.yo.day1.service.impl.BillingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BillingServiceTest {

    @Mock
    private TuitionInvoiceRepository tuitionInvoiceRepository;
    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private StudentService studentService;
    @Mock
    private CourseClassService courseClassService;
    @Mock
    private AuthService authService;
    @Mock
    private ModelMapper mapper;

    @InjectMocks
    private BillingServiceImpl service;

    // ==================== createInvoice ====================

    @Test
    void createInvoiceSuccess() {
        InvoiceCreateRequest request = buildRequest();
        
        Student student = new Student();
        student.setId(1L);
        student.setStudentCode("STU01");
        
        CourseClass courseClass = new CourseClass();
        courseClass.setId(1L);
        courseClass.setClassCode("CLS01");
        courseClass.setTuitionFee(BigDecimal.valueOf(1000));
        
        when(studentService.getStudent(1L)).thenReturn(student);
        when(courseClassService.getCourseClass(1L)).thenReturn(courseClass);
        when(tuitionInvoiceRepository.existsByStudentIdAndCourseClassIdAndBillingMonth(1L, 1L, LocalDate.now().withDayOfMonth(1))).thenReturn(false);
        when(tuitionInvoiceRepository.existsByInvoiceCode(any())).thenReturn(false);
        
        TuitionInvoice invoice = new TuitionInvoice();
        invoice.setId(1L);
        invoice.setStudent(student);
        invoice.setCourseClass(courseClass);
        invoice.setStatus(InvoiceStatus.UNPAID);
        
        when(tuitionInvoiceRepository.save(any(TuitionInvoice.class))).thenReturn(invoice);
        when(mapper.map(any(TuitionInvoice.class), eq(InvoiceResponse.class))).thenReturn(new InvoiceResponse());
        
        InvoiceResponse result = service.createInvoice(request);
        
        assertThat(result).isNotNull();
    }

    @Test
    void createInvoiceThrowsWhenDuplicate() {
        InvoiceCreateRequest request = buildRequest();
        
        Student student = new Student();
        student.setId(1L);
        
        CourseClass courseClass = new CourseClass();
        courseClass.setId(1L);
        
        when(studentService.getStudent(1L)).thenReturn(student);
        when(courseClassService.getCourseClass(1L)).thenReturn(courseClass);
        when(tuitionInvoiceRepository.existsByStudentIdAndCourseClassIdAndBillingMonth(1L, 1L, LocalDate.now().withDayOfMonth(1))).thenReturn(true);
        
        assertThatThrownBy(() -> service.createInvoice(request))
                .isInstanceOf(com.yo.day1.common.exception.ConflictException.class)
                .hasMessageContaining("Đã có hóa đơn tháng");
    }

    // ==================== helpers ====================

    private InvoiceCreateRequest buildRequest() {
        InvoiceCreateRequest req = new InvoiceCreateRequest();
        req.setStudentId(1L);
        req.setCourseClassId(1L);
        req.setBillingMonth(LocalDate.now());
        return req;
    }
}
