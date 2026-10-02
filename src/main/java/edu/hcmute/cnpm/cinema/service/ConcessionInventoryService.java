package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.dto.inventory.ComboStockView;
import edu.hcmute.cnpm.cinema.dto.inventory.InventoryOverview;
import edu.hcmute.cnpm.cinema.dto.inventory.StockItemView;
import edu.hcmute.cnpm.cinema.dto.inventory.StockLevel;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.repository.ConcessionProductRepository;
import edu.hcmute.cnpm.cinema.repository.ConcessionStockMovementRepository;
import edu.hcmute.cnpm.cinema.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Kho bắp nước: xem tồn kho, nhập kho, xuất hủy, kiểm kê và trừ kho khi khách thanh toán.
 *
 * Chỉ món lẻ có tồn kho. Combo không có kho riêng: số combo còn bán được tính theo món
 * thành phần ít nhất, và bán một combo là trừ kho từng món thành phần.
 */
@Service
public class ConcessionInventoryService {

    static final int MAX_STOCK = 100_000;
    static final int MAX_CHANGE_PER_ENTRY = 10_000;
    static final int MAX_LOW_STOCK_THRESHOLD = 10_000;
    private static final int MAX_NOTE_LENGTH = 255;

    private final ConcessionProductRepository productRepository;
    private final ConcessionStockMovementRepository movementRepository;
    private final UserRepository userRepository;

    public ConcessionInventoryService(ConcessionProductRepository productRepository,
                                      ConcessionStockMovementRepository movementRepository,
                                      UserRepository userRepository) {
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public InventoryOverview overview() {
        List<ConcessionProduct> products = productRepository.findAllByOrderByDisplayOrderAscNameAsc();
        Map<Long, List<String>> combosByItem = combosUsingEachItem(products);
        List<StockItemView> items = new ArrayList<>();
        List<ComboStockView> combos = new ArrayList<>();
        for (ConcessionProduct product : products) {
            if (product.isCombo()) {
                combos.add(toComboView(product));
            } else {
                items.add(toItemView(product, combosByItem));
            }
        }
        // Món đã ngừng bán và không nằm trong combo nào thì hết cũng không sao, không đếm vào cảnh báo.
        List<StockItemView> onSale = items.stream()
                .filter(item -> item.active() || !item.usedInCombos().isEmpty())
                .toList();
        long lowCount = onSale.stream().filter(item -> item.level() == StockLevel.LOW).count();
        long outCount = onSale.stream().filter(item -> item.level() == StockLevel.OUT).count();
        long soldToday = movementRepository.sumOutgoingSince(StockMovementType.SALE, LocalDate.now().atStartOfDay());
        return new InventoryOverview(items, combos, lowCount, outCount, soldToday);
    }

    @Transactional(readOnly = true)
    public StockItemView findStockItem(Long productId) {
        List<ConcessionProduct> products = productRepository.findAllByOrderByDisplayOrderAscNameAsc();
        ConcessionProduct item = products.stream()
                .filter(product -> product.getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("món bắp nước", productId));
        requireStockItem(item);
        return toItemView(item, combosUsingEachItem(products));
    }

    @Transactional(readOnly = true)
    public List<ConcessionStockMovement> findRecentMovements() {
        return movementRepository.findTop20ByOrderByCreatedAtDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<ConcessionStockMovement> findMovements(Long productId) {
        return movementRepository.findTop50ByProductIdOrderByCreatedAtDescIdDesc(productId);
    }

    /** Số món cần nhập thêm, hiện thành huy hiệu trên thanh điều hướng của nhân viên. */
    @Transactional(readOnly = true)
    public long countItemsToRestock() {
        return productRepository.countLowStockItems();
    }

    /** Số phần còn bán được của từng sản phẩm, combo tính theo món thành phần ít nhất. */
    @Transactional(readOnly = true)
    public Map<Long, Integer> findAvailableQuantities() {
        Map<Long, Integer> available = new HashMap<>();
        for (ConcessionProduct product : productRepository.findAllByOrderByDisplayOrderAscNameAsc()) {
            available.put(product.getId(), availableOf(product));
        }
        return available;
    }

    /**
     * Nhân viên nhập kho, xuất hủy hoặc kiểm kê một món lẻ.
     *
     * @param quantity số phần nhập/hủy, riêng kiểm kê là số đếm được thực tế trên kệ
     */
    @Transactional
    public ConcessionStockMovement adjustStock(Long productId, Long staffId, StockMovementType type,
                                               Integer quantity, String note) {
        User staff = findEmployee(staffId);
        if (type == null || !type.isManual()) {
            throw new BusinessException("Bạn chọn một thao tác: nhập kho, xuất hủy hoặc kiểm kê.");
        }
        if (quantity == null) {
            throw new BusinessException("Bạn nhập số lượng.");
        }
        String cleanNote = note == null ? "" : note.trim();
        if (cleanNote.length() > MAX_NOTE_LENGTH) {
            throw new BusinessException("Ghi chú tối đa " + MAX_NOTE_LENGTH + " ký tự.");
        }
        ConcessionProduct item = productRepository.findByIdForStockUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("món bắp nước", productId));
        requireStockItem(item);

        int before = item.getStockQuantity();
        int after = switch (type) {
            case IMPORT -> {
                requireRange(quantity, 1, MAX_CHANGE_PER_ENTRY, "Mỗi lần nhập kho từ 1 đến 10.000 phần.");
                yield before + quantity;
            }
            case WRITE_OFF -> {
                requireRange(quantity, 1, MAX_CHANGE_PER_ENTRY, "Mỗi lần xuất hủy từ 1 đến 10.000 phần.");
                if (quantity > before) {
                    throw new BusinessException("Kho chỉ còn " + before + " phần, không xuất hủy được "
                            + quantity + " phần.");
                }
                if (cleanNote.isEmpty()) {
                    throw new BusinessException("Bạn ghi lý do xuất hủy, ví dụ: bắp bị ỉu, nước hết hạn.");
                }
                yield before - quantity;
            }
            case STOCKTAKE -> {
                requireRange(quantity, 0, MAX_STOCK, "Số đếm thực tế từ 0 đến 100.000 phần.");
                yield quantity;
            }
            default -> throw new BusinessException("Bán hàng do hệ thống tự trừ kho, nhân viên không ghi tay.");
        };
        if (after > MAX_STOCK) {
            throw new BusinessException("Tồn kho mỗi món tối đa 100.000 phần.");
        }
        item.setStockQuantity(after);

        ConcessionStockMovement movement = new ConcessionStockMovement();
        movement.setProduct(item);
        movement.setType(type);
        movement.setQuantityChange(after - before);
        movement.setQuantityAfter(after);
        movement.setNote(cleanNote.isEmpty() ? null : cleanNote);
        movement.setActor(staff);
        movement.setActorName(staff.getFullName());
        return movementRepository.save(movement);
    }

    @Transactional
    public ConcessionProduct updateLowStockThreshold(Long productId, Long staffId, Integer threshold) {
        findEmployee(staffId);
        if (threshold == null || threshold < 0 || threshold > MAX_LOW_STOCK_THRESHOLD) {
            throw new BusinessException("Ngưỡng cảnh báo từ 0 đến 10.000 phần.");
        }
        ConcessionProduct item = productRepository.findByIdForStockUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("món bắp nước", productId));
        requireStockItem(item);
        item.setLowStockThreshold(threshold);
        return item;
    }

    /** Kiểm tra trước, chưa trừ: kho còn đủ cho các món khách vừa chọn không. Gọi trong transaction lưu đơn. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireInStock(Map<ConcessionProduct, Integer> selection) {
        for (StockNeed need : stockNeeded(selection).values()) {
            int stock = need.item().getStockQuantity();
            if (stock < need.quantity()) {
                throw shortage(need.item().getName(), stock, need.quantity());
            }
        }
    }

    /**
     * Trừ kho cho đơn vừa thanh toán và ghi lịch sử bán.
     *
     * Chạy chung transaction với thanh toán: thiếu hàng thì ném lỗi, cả lần thanh toán được
     * hủy và không món nào bị trừ dở dang.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deductForPaidOrder(BookingOrder order) {
        if (order.getItems().isEmpty()) {
            return;
        }
        Map<ConcessionProduct, Integer> selection = new LinkedHashMap<>();
        for (BookingOrderItem orderItem : order.getItems()) {
            selection.merge(orderItem.getProduct(), orderItem.getQuantity(), Integer::sum);
        }
        // Trừ theo thứ tự mã sản phẩm để hai đơn chạy song song luôn khóa dòng theo cùng thứ tự.
        for (StockNeed need : stockNeeded(selection).values()) {
            Long itemId = need.item().getId();
            if (productRepository.deductStock(itemId, need.quantity()) == 0) {
                throw shortage(need.item().getName(), productRepository.findStockQuantity(itemId), need.quantity());
            }
            ConcessionStockMovement movement = new ConcessionStockMovement();
            movement.setProduct(need.item());
            movement.setType(StockMovementType.SALE);
            movement.setQuantityChange(-need.quantity());
            movement.setQuantityAfter(productRepository.findStockQuantity(itemId));
            movement.setReference(order.getReceiptCode());
            movement.setNote("Khách mua kèm vé");
            movement.setActor(order.getUser());
            movement.setActorName(order.getCustomerName());
            movementRepository.save(movement);
        }
    }

    /** Quy đơn về số phần cần của từng món lẻ (combo tách ra món thành phần), xếp theo mã món. */
    private SortedMap<Long, StockNeed> stockNeeded(Map<ConcessionProduct, Integer> selection) {
        SortedMap<Long, StockNeed> needs = new TreeMap<>();
        selection.forEach((product, quantity) -> {
            if (product.isCombo()) {
                for (ConcessionComboItem component : product.getComponents()) {
                    addNeed(needs, component.getComponent(), component.getQuantity() * quantity);
                }
            } else {
                addNeed(needs, product, quantity);
            }
        });
        return needs;
    }

    private void addNeed(SortedMap<Long, StockNeed> needs, ConcessionProduct item, int quantity) {
        needs.merge(item.getId(), new StockNeed(item, quantity),
                (current, extra) -> new StockNeed(current.item(), current.quantity() + extra.quantity()));
    }

    private int availableOf(ConcessionProduct product) {
        if (!product.isCombo()) {
            return Math.max(0, product.getStockQuantity());
        }
        int available = Integer.MAX_VALUE;
        for (ConcessionComboItem component : product.getComponents()) {
            int perCombo = Math.max(1, component.getQuantity());
            available = Math.min(available, Math.max(0, component.getComponent().getStockQuantity()) / perCombo);
        }
        return available;
    }

    private Map<Long, List<String>> combosUsingEachItem(List<ConcessionProduct> products) {
        Map<Long, List<String>> combosByItem = new HashMap<>();
        for (ConcessionProduct product : products) {
            for (ConcessionComboItem component : product.getComponents()) {
                combosByItem.computeIfAbsent(component.getComponent().getId(), id -> new ArrayList<>())
                        .add(product.getName());
            }
        }
        return combosByItem;
    }

    private StockItemView toItemView(ConcessionProduct item, Map<Long, List<String>> combosByItem) {
        return new StockItemView(item.getId(), item.getCode(), item.getName(), item.getIcon(), item.getPrice(),
                item.isActive(), item.getStockQuantity(), item.getLowStockThreshold(),
                StockLevel.of(item.getStockQuantity(), item.getLowStockThreshold()),
                List.copyOf(combosByItem.getOrDefault(item.getId(), List.of())));
    }

    private ComboStockView toComboView(ConcessionProduct combo) {
        List<String> recipe = new ArrayList<>();
        boolean componentRunningLow = false;
        for (ConcessionComboItem component : combo.getComponents()) {
            ConcessionProduct item = component.getComponent();
            recipe.add(component.getQuantity() + " × " + item.getName());
            componentRunningLow |= StockLevel.of(item.getStockQuantity(), item.getLowStockThreshold())
                    != StockLevel.IN_STOCK;
        }
        int available = availableOf(combo);
        StockLevel level = available <= 0 ? StockLevel.OUT
                : componentRunningLow ? StockLevel.LOW : StockLevel.IN_STOCK;
        return new ComboStockView(combo.getId(), combo.getName(), combo.getIcon(), combo.getPrice(),
                combo.isActive(), available, level, recipe);
    }

    private User findEmployee(Long staffId) {
        User user = staffId == null ? null : userRepository.findById(staffId).orElse(null);
        if (user == null || (user.getRole() != Role.STAFF && user.getRole() != Role.ADMIN)) {
            throw new BusinessException("Chỉ nhân viên hoặc quản lý được cập nhật kho bắp nước.");
        }
        return user;
    }

    private void requireStockItem(ConcessionProduct product) {
        if (product.isCombo()) {
            throw new BusinessException("Combo không có kho riêng. Bạn cập nhật kho từng món trong combo nhé.");
        }
    }

    private void requireRange(int value, int min, int max, String message) {
        if (value < min || value > max) {
            throw new BusinessException(message);
        }
    }

    private BusinessException shortage(String itemName, int stock, int needed) {
        if (stock <= 0) {
            return new BusinessException("“" + itemName + "” vừa hết hàng. Bạn bỏ món này "
                    + "(hoặc combo có món này) rồi tiếp tục nhé.");
        }
        return new BusinessException("Quầy chỉ còn " + stock + " phần “" + itemName + "” mà đơn của bạn cần "
                + needed + " phần (đã tính cả combo). Bạn giảm số lượng rồi tiếp tục nhé.");
    }

    private record StockNeed(ConcessionProduct item, int quantity) {}
}
