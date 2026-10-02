package edu.hcmute.cnpm.cinema.staff;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.inventory.ComboStockView;
import edu.hcmute.cnpm.cinema.dto.inventory.InventoryOverview;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.BookingOrderService;
import edu.hcmute.cnpm.cinema.service.ConcessionInventoryService;
import edu.hcmute.cnpm.cinema.service.PaymentService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("Kho bắp nước: nhập, hủy, kiểm kê và trừ kho khi bán")
class ConcessionInventoryIntegrationTest extends IntegrationTestBase {

    private static final String INVENTORY = "/nhan-vien/kho-bap-nuoc";

    @Autowired private MockMvc mockMvc;
    @Autowired private ConcessionInventoryService inventoryService;
    @Autowired private BookingOrderService bookingOrderService;
    @Autowired private PaymentService paymentService;

    @Test
    @DisplayName("Nhân viên nhập kho, xuất hủy có lý do, kiểm kê; mỗi lần đều ghi lịch sử")
    void shouldImportWriteOffAndStocktake_andKeepHistory() throws Exception {
        User staff = testDataFactory.createUserWithRole("kho@example.com", Role.STAFF);
        ConcessionProduct popcorn = product("POP-M", "Bắp rang cỡ vừa", 10, 5, true);
        String adjustUrl = INVENTORY + "/" + popcorn.getId() + "/dieu-chinh";

        mockMvc.perform(post(adjustUrl).sessionAttr(Constants.SESSION_USER, staff)
                        .param("type", "IMPORT").param("quantity", "50").param("note", "Phiếu nhập số 12"))
                .andExpect(redirectedUrl(INVENTORY + "/" + popcorn.getId()))
                .andExpect(flash().attribute(Constants.MODEL_SUCCESS_MESSAGE, containsString("Đã nhập thêm 50 phần")));
        assertThat(stockOf(popcorn)).isEqualTo(60);

        mockMvc.perform(post(adjustUrl).sessionAttr(Constants.SESSION_USER, staff)
                        .param("type", "WRITE_OFF").param("quantity", "5"))
                .andExpect(flash().attribute(Constants.MODEL_ERROR_MESSAGE, containsString("lý do xuất hủy")));
        mockMvc.perform(post(adjustUrl).sessionAttr(Constants.SESSION_USER, staff)
                        .param("type", "WRITE_OFF").param("quantity", "61").param("note", "Đổ vỡ"))
                .andExpect(flash().attribute(Constants.MODEL_ERROR_MESSAGE, containsString("Kho chỉ còn 60 phần")));
        assertThat(stockOf(popcorn)).as("Lần hủy bị từ chối không được trừ kho").isEqualTo(60);

        mockMvc.perform(post(adjustUrl).sessionAttr(Constants.SESSION_USER, staff)
                        .param("type", "WRITE_OFF").param("quantity", "5").param("note", "Bắp bị ỉu"))
                .andExpect(flash().attributeExists(Constants.MODEL_SUCCESS_MESSAGE));
        mockMvc.perform(post(adjustUrl).sessionAttr(Constants.SESSION_USER, staff)
                        .param("type", "STOCKTAKE").param("quantity", "53"))
                .andExpect(flash().attribute(Constants.MODEL_SUCCESS_MESSAGE, containsString("lệch -2 phần")));
        assertThat(stockOf(popcorn)).isEqualTo(53);

        List<ConcessionStockMovement> history = inventoryService.findMovements(popcorn.getId());
        assertThat(history).extracting(ConcessionStockMovement::getType)
                .containsExactly(StockMovementType.STOCKTAKE, StockMovementType.WRITE_OFF, StockMovementType.IMPORT);
        assertThat(history).extracting(ConcessionStockMovement::getQuantityChange).containsExactly(-2, -5, 50);
        assertThat(history).extracting(ConcessionStockMovement::getActorName).containsOnly(staff.getFullName());

        mockMvc.perform(get(INVENTORY + "/" + popcorn.getId()).sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lịch sử kho của món này")))
                .andExpect(content().string(containsString("Phiếu nhập số 12")))
                .andExpect(content().string(containsString("Bắp bị ỉu")));
    }

    @Test
    @DisplayName("Chỉ nhân viên và quản lý vào được kho; khách hàng bị từ chối, chưa đăng nhập thì phải đăng nhập")
    void shouldKeepCustomersOutOfInventory() throws Exception {
        User customer = testDataFactory.createCustomer("khach-kho@example.com");
        User admin = testDataFactory.createUserWithRole("quanly@example.com", Role.ADMIN);
        ConcessionProduct popcorn = product("POP-M", "Bắp rang cỡ vừa", 10, 5, true);

        mockMvc.perform(get(INVENTORY))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dang-nhap?next=%2Fnhan-vien%2Fkho-bap-nuoc"));
        mockMvc.perform(get(INVENTORY).sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(INVENTORY + "/" + popcorn.getId() + "/dieu-chinh")
                        .sessionAttr(Constants.SESSION_USER, customer)
                        .param("type", "IMPORT").param("quantity", "999"))
                .andExpect(status().isForbidden());
        assertThat(stockOf(popcorn)).isEqualTo(10);

        mockMvc.perform(get(INVENTORY).sessionAttr(Constants.SESSION_USER, admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kho bắp nước")))
                .andExpect(content().string(containsString("Bắp rang cỡ vừa")));
    }

    @Test
    @DisplayName("Bán combo trừ kho từng món thành phần và ghi mã hóa đơn vào lịch sử kho")
    void shouldDeductComponents_whenComboIsPaid() {
        ConcessionProduct popcorn = product("POP-M", "Bắp rang cỡ vừa", 5, 2, true);
        ConcessionProduct drink = product("DRINK-M", "Nước ngọt cỡ vừa", 10, 2, true);
        ConcessionProduct comboForTwo = product("COMBO-2", "Combo đôi", 0, 0, true);
        recipe(comboForTwo, popcorn, 1);
        recipe(comboForTwo, drink, 2);

        Showtime showtime = holdTicketFor("combo@example.com", "A");
        User customer = userRepository.findByEmail("combo@example.com").orElseThrow();
        bookingOrderService.saveConcessions(customer.getId(), showtime.getId(),
                Map.of(comboForTwo.getId(), 2, popcorn.getId(), 1));

        Ticket paid = paymentService.confirmPayment(customer.getId(), showtime.getId()).get(0);

        assertThat(stockOf(popcorn)).as("2 combo dùng 2 bắp, cộng 1 bắp lẻ").isEqualTo(2);
        assertThat(stockOf(drink)).as("2 combo, mỗi combo 2 nước").isEqualTo(6);
        assertThat(inventoryService.findMovements(drink.getId()))
                .singleElement()
                .satisfies(movement -> {
                    assertThat(movement.getType()).isEqualTo(StockMovementType.SALE);
                    assertThat(movement.getQuantityChange()).isEqualTo(-4);
                    assertThat(movement.getQuantityAfter()).isEqualTo(6);
                    assertThat(movement.getReference()).isEqualTo(paid.getBookingOrder().getReceiptCode());
                });

        InventoryOverview overview = inventoryService.overview();
        ComboStockView combo = overview.combos().get(0);
        assertThat(combo.available()).as("Còn 2 bắp và 6 nước thì ghép được 2 combo đôi").isEqualTo(2);
        assertThat(combo.recipe()).containsExactly("1 × Bắp rang cỡ vừa", "2 × Nước ngọt cỡ vừa");
        assertThat(overview.soldToday()).isEqualTo(3 + 4);
    }

    @Test
    @DisplayName("Khách không chọn được nhiều hơn số còn trong kho, món hết hàng bị khóa trên trang")
    void shouldRejectSelectionAboveStock_andShowSoldOut() throws Exception {
        ConcessionProduct popcorn = product("POP-M", "Bắp rang cỡ vừa", 1, 2, true);
        ConcessionProduct drink = product("DRINK-M", "Nước ngọt cỡ vừa", 0, 2, true);
        Showtime showtime = holdTicketFor("it-hang@example.com", "B");
        User customer = userRepository.findByEmail("it-hang@example.com").orElseThrow();

        mockMvc.perform(get("/bap-nuoc/{id}", showtime.getId()).sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tạm hết hàng")))
                .andExpect(content().string(containsString("Chỉ còn 1 phần")));

        mockMvc.perform(post("/bap-nuoc/{id}", showtime.getId()).sessionAttr(Constants.SESSION_USER, customer)
                        .param("quantities[" + popcorn.getId() + "]", "2"))
                .andExpect(redirectedUrl("/bap-nuoc/" + showtime.getId()))
                .andExpect(flash().attribute(Constants.MODEL_ERROR_MESSAGE, containsString("Quầy chỉ còn 1 phần")));
        mockMvc.perform(post("/bap-nuoc/{id}", showtime.getId()).sessionAttr(Constants.SESSION_USER, customer)
                        .param("quantities[" + drink.getId() + "]", "1"))
                .andExpect(flash().attribute(Constants.MODEL_ERROR_MESSAGE, containsString("vừa hết hàng")));
    }

    @Test
    @DisplayName("Kho hết trong lúc khách đang thanh toán: hủy cả lần thanh toán, vé vẫn đang giữ, kho không bị trừ dở")
    void shouldCancelPayment_whenStockRunsOutBeforePaying() {
        User staff = testDataFactory.createUserWithRole("kho@example.com", Role.STAFF);
        // Tạo nước trước để nước có mã nhỏ hơn: lúc thanh toán nước được trừ trước, rồi mới tới bắp bị thiếu.
        ConcessionProduct drink = product("DRINK-M", "Nước ngọt cỡ vừa", 10, 2, true);
        ConcessionProduct popcorn = product("POP-M", "Bắp rang cỡ vừa", 2, 2, true);
        Showtime showtime = holdTicketFor("cham-chan@example.com", "C");
        User customer = userRepository.findByEmail("cham-chan@example.com").orElseThrow();
        bookingOrderService.saveConcessions(customer.getId(), showtime.getId(),
                Map.of(popcorn.getId(), 2, drink.getId(), 1));

        inventoryService.adjustStock(popcorn.getId(), staff.getId(), StockMovementType.WRITE_OFF, 1, "Rơi vỡ");

        assertThatThrownBy(() -> paymentService.confirmPayment(customer.getId(), showtime.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quầy chỉ còn 1 phần “Bắp rang cỡ vừa”");
        assertThat(stockOf(popcorn)).isEqualTo(1);
        assertThat(stockOf(drink)).as("Món đủ hàng cũng không bị trừ vì cả giao dịch đã hủy").isEqualTo(10);
        assertThat(ticketRepository.findAll()).extracting(Ticket::getStatus).containsOnly(TicketStatus.HELD);
        assertThat(inventoryService.findMovements(drink.getId())).isEmpty();
    }

    @Test
    @DisplayName("Hai khách cùng trả tiền cho phần bắp cuối cùng: chỉ một người mua được")
    void shouldSellLastPortionOnlyOnce_whenTwoCustomersPayTogether() throws Exception {
        ConcessionProduct popcorn = product("POP-M", "Bắp rang cỡ vừa", 1, 2, true);
        Showtime first = holdTicketFor("nhanh-tay-1@example.com", "D");
        Showtime second = holdTicketFor("nhanh-tay-2@example.com", "E");
        User firstCustomer = userRepository.findByEmail("nhanh-tay-1@example.com").orElseThrow();
        User secondCustomer = userRepository.findByEmail("nhanh-tay-2@example.com").orElseThrow();
        bookingOrderService.saveConcessions(firstCustomer.getId(), first.getId(), Map.of(popcorn.getId(), 1));
        bookingOrderService.saveConcessions(secondCustomer.getId(), second.getId(), Map.of(popcorn.getId(), 1));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        try {
            List<Future<?>> payments = List.of(
                    pool.submit(() -> pay(start, firstCustomer, first, succeeded, rejected)),
                    pool.submit(() -> pay(start, secondCustomer, second, succeeded, rejected)));
            start.countDown();
            for (Future<?> payment : payments) {
                payment.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(1);
        assertThat(stockOf(popcorn)).isZero();
    }

    @Test
    @DisplayName("Thanh menu nhân viên báo số món cần nhập; món ngừng bán và không nằm trong combo thì không tính")
    void shouldCountItemsToRestock_forNavigationBadge() throws Exception {
        User staff = testDataFactory.createUserWithRole("kho@example.com", Role.STAFF);
        product("POP-M", "Bắp rang cỡ vừa", 3, 10, true);
        product("DRINK-M", "Nước ngọt cỡ vừa", 100, 10, true);
        product("OLD", "Món cũ đã ngừng", 0, 10, false);
        ConcessionProduct cup = product("CUP", "Ly giấy", 0, 10, false);
        ConcessionProduct combo = product("COMBO-1", "Combo một người", 0, 0, true);
        recipe(combo, cup, 1);

        assertThat(inventoryService.countItemsToRestock()).as("Bắp sắp hết và ly giấy (nằm trong combo) đã hết").isEqualTo(2);
        InventoryOverview overview = inventoryService.overview();
        assertThat(overview.lowCount() + overview.outCount()).isEqualTo(2);

        mockMvc.perform(get("/nhan-vien/soat-ve").sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(content().string(containsString("2 món sắp hết hoặc đã hết")));
        mockMvc.perform(get("/").sessionAttr(Constants.SESSION_USER, testDataFactory.createCustomer("k@example.com")))
                .andExpect(content().string(not(containsString("Kho bắp nước"))));
    }

    private void pay(CountDownLatch start, User customer, Showtime showtime,
                     AtomicInteger succeeded, AtomicInteger rejected) {
        try {
            start.await();
            paymentService.confirmPayment(customer.getId(), showtime.getId());
            succeeded.incrementAndGet();
        } catch (BusinessException exception) {
            rejected.incrementAndGet();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    /** Mỗi khách giữ một ghế ở một suất riêng, để hai lần thanh toán không khóa chung suất chiếu. */
    private Showtime holdTicketFor(String email, String row) {
        User customer = testDataFactory.createCustomer(email);
        Movie movie = testDataFactory.createMovie("Phim " + row);
        Room room = testDataFactory.createRoom("Phòng " + row, 2, 4);
        Seat seat = testDataFactory.createSeat(room, row, 1);
        Showtime showtime = testDataFactory.createShowtime(movie, room, LocalDateTime.now().plusDays(1));
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, seat, customer));
        return showtime;
    }

    private ConcessionProduct product(String code, String name, int stock, int threshold, boolean active) {
        ConcessionProduct product = new ConcessionProduct();
        product.setCode(code);
        product.setName(name);
        product.setPrice(new BigDecimal("50000"));
        product.setIcon("🍿");
        product.setActive(active);
        product.setDisplayOrder((int) concessionProductRepository.count() + 1);
        product.setStockQuantity(stock);
        product.setLowStockThreshold(threshold);
        return concessionProductRepository.save(product);
    }

    private void recipe(ConcessionProduct combo, ConcessionProduct component, int quantity) {
        concessionComboItemRepository.save(new ConcessionComboItem(combo, component, quantity));
    }

    private int stockOf(ConcessionProduct product) {
        return concessionProductRepository.findById(product.getId()).orElseThrow().getStockQuantity();
    }
}
