---
name: edupay-testing-quality
description: Testing standards, Mockito patterns, Spring Boot test slices (@WebMvcTest, @DataJpaTest), and Angular unit testing guidelines for EduPay SaaS.
---

# 🧪 EduPay SaaS Testing & Quality Assurance Skill

Use this skill whenever you are writing automated tests, validating logic, or setting up test fixtures for Backend or Frontend in EduPay SaaS.

---

## 1. Tiêu Chuẩn Kiểm Thử Backend (JUnit 5 & Mockito)

### 1. Unit Test Cho Service Layer (Mockito)
Kiểm tra nghiệp vụ độc lập, không cần khởi động Spring context:

```java
@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentMapper studentMapper;

    @InjectMocks
    private StudentServiceImpl studentService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Tạo học sinh thành công khi số điện thoại chưa tồn tại")
    void createStudent_Success() {
        CreateStudentRequest request = new CreateStudentRequest("Nguyen Van A", "0901234567", null);
        Student student = new Student("Nguyen Van A", "0901234567", null);
        Student savedStudent = new Student("Nguyen Van A", "0901234567", null);
        savedStudent.setId(UUID.randomUUID());
        savedStudent.setTenantId(tenantId);

        StudentResponse expectedResponse = new StudentResponse(savedStudent.getId(), "Nguyen Van A", "0901234567", null);

        when(studentRepository.existsByTenantIdAndParentPhone(tenantId, request.getParentPhone())).thenReturn(false);
        when(studentMapper.toEntity(request)).thenReturn(student);
        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);
        when(studentMapper.toResponse(savedStudent)).thenReturn(expectedResponse);

        StudentResponse actual = studentService.createStudent(request);

        assertNotNull(actual);
        assertEquals("Nguyen Van A", actual.getFullName());
        verify(studentRepository, times(1)).save(any(Student.class));
    }

    @Test
    @DisplayName("Ném ngoại lệ DuplicateResourceException khi số điện thoại đã tồn tại trong lớp")
    void createStudent_DuplicatePhone_ThrowsException() {
        CreateStudentRequest request = new CreateStudentRequest("Nguyen Van A", "0901234567", null);

        when(studentRepository.existsByTenantIdAndParentPhone(tenantId, request.getParentPhone())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> studentService.createStudent(request));
        verify(studentRepository, never()).save(any(Student.class));
    }
}
```

### 2. Controller Slice Test (@WebMvcTest)
Kiểm tra định tuyến, validation `@Valid`, status HTTP và format JSON response:

```java
@WebMvcTest(StudentController.class)
@AutoConfigureMockMvc(addFilters = false) // Tắt security filter trong slice test đơn giản
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService studentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /students trả về 400 Bad Request khi tên học sinh bị để trống")
    void createStudent_ValidationFails() throws Exception {
        CreateStudentRequest invalidRequest = new CreateStudentRequest("", "0901234567", null);

        mockMvc.perform(post("/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
```

---

## 2. Tiêu Chuẩn Kiểm Thử Frontend (Angular Jasmine/Karma)

Kiểm thử logic của Standalone Component:

```typescript
describe('AttendanceCardComponent', () => {
  let component: AttendanceCardComponent;
  let fixture: ComponentFixture<AttendanceCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AttendanceCardComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(AttendanceCardComponent);
    component = fixture.componentInstance;
    
    // Gán required input
    fixture.componentRef.setInput('student', { id: '1', fullName: 'Nguyen Van A' });
    fixture.detectChanges();
  });

  it('nên tạo component thành công', () => {
    expect(component).toBeTruthy();
  });

  it('nên emit sự kiện statusChange khi bấm nút Có mặt', () => {
    spyOn(component.statusChange, 'emit');
    component.setStatus('PRESENT');
    expect(component.currentStatus()).toBe('PRESENT');
    expect(component.statusChange.emit).toHaveBeenCalledWith('PRESENT');
  });
});
```

---

## 3. Checklist Chất Lượng Code Trước Khi Merge PR
- [ ] Backend: Đã chạy `mvn test` và tất cả test cases đều PASS.
- [ ] Backend: Không có query nào thiếu `tenant_id` trong repository/JPQL.
- [ ] Backend: Các ngoại lệ đều được bao bọc bởi `GlobalExceptionHandler`.
- [ ] Frontend: Đã kiểm tra giao diện trên màn hình mobile (chiều rộng 375px và 414px).
- [ ] Frontend: Nút bấm có chiều cao tối thiểu 44px (touch target chuẩn).
- [ ] Cả 2: Đã commit theo quy chuẩn Conventional Commits.
