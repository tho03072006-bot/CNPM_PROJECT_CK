package edu.hcmute.cnpm.cinema.support;

import edu.hcmute.cnpm.cinema.repository.MovieRepository;
import edu.hcmute.cnpm.cinema.repository.RoomRepository;
import edu.hcmute.cnpm.cinema.repository.SeatRepository;
import edu.hcmute.cnpm.cinema.repository.ShowtimeRepository;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import edu.hcmute.cnpm.cinema.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lop cha cho MOI test tich hop (test co dung database that) trong du an.
 *
 * Lop nay lo san 3 viec de cac module khac chi viec ke thua:
 *   1. Bat profile "test" -> tro vao database rieng cinema_booking_test.
 *   2. CHAN test chay nham tren database dang dev (kiem tra ten database truoc moi test).
 *   3. Xoa sach du lieu truoc moi test case -> cac test khong lam anh huong lan nhau.
 *
 * Cach dung:
 *   class ABCIntegrationTest extends IntegrationTestBase { ... }
 *
 * Phu trach: Tho (Module 4 - Kien truc dung chung + Testing).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestDataFactory.class)
public abstract class IntegrationTestBase {

    /** Ten database test bat buoc phai ket thuc bang hau to nay. */
    private static final String TEST_DATABASE_SUFFIX = "_test";

    @Autowired
    protected TestDataFactory testDataFactory;

    @Autowired
    protected TicketRepository ticketRepository;
    @Autowired
    protected ShowtimeRepository showtimeRepository;
    @Autowired
    protected SeatRepository seatRepository;
    @Autowired
    protected RoomRepository roomRepository;
    @Autowired
    protected MovieRepository movieRepository;
    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void verifyTestDatabaseThenClean() {
        verifyConnectedToTestDatabase();
        cleanDatabase();
    }

    /**
     * Chot an toan: neu file cau hinh bi sua nham lam test tro vao database that,
     * test se dung lai ngay o day thay vi xoa mat du lieu cua ca nhom.
     */
    private void verifyConnectedToTestDatabase() {
        String currentDatabaseName = jdbcTemplate.queryForObject("SELECT DB_NAME()", String.class);
        assertThat(currentDatabaseName)
                .as("Test tich hop chi duoc chay tren database test (ten phai ket thuc bang '%s'). "
                        + "Dang ket noi vao '%s' - kiem tra lai src/test/resources/application-test.properties",
                        TEST_DATABASE_SUFFIX, currentDatabaseName)
                .endsWith(TEST_DATABASE_SUFFIX);
    }

    /** Xoa du lieu theo dung thu tu khoa ngoai: ticket -> showtime -> seat -> room -> movie -> user. */
    protected void cleanDatabase() {
        ticketRepository.deleteAllInBatch();
        showtimeRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();
        movieRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }
}
