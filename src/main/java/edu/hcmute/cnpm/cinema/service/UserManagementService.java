package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Quản trị viên xem danh sách tài khoản và cấp vai trò (khách hàng, nhân viên, quản trị).
 *
 * Tài khoản nhân viên trước đây chỉ tạo được bằng cách sửa tay trong database.
 */
@Service
public class UserManagementService {

    private final UserRepository userRepository;

    public UserManagementService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Tài khoản mới nhất lên đầu, lọc theo họ tên hoặc email (không phân biệt dấu). */
    @Transactional(readOnly = true)
    public List<User> searchUsers(String keyword) {
        String foldedKeyword = VietnameseText.fold(keyword);
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(user -> foldedKeyword.isEmpty()
                        || VietnameseText.fold(user.getFullName()).contains(foldedKeyword)
                        || VietnameseText.fold(user.getEmail()).contains(foldedKeyword))
                .toList();
    }

    /** Đếm số tài khoản theo từng vai trò, vai trò nào chưa có ai thì vẫn trả về 0. */
    @Transactional(readOnly = true)
    public Map<Role, Long> countUsersByRole() {
        Map<Role, Long> counts = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            counts.put(role, 0L);
        }
        userRepository.findAll().forEach(user -> counts.merge(user.getRole(), 1L, Long::sum));
        return counts;
    }

    /**
     * Đổi vai trò của một tài khoản.
     *
     * Không cho tự đổi vai trò của chính mình: quản trị viên duy nhất lỡ tay hạ mình xuống
     * khách hàng là cả hệ thống không còn ai vào được trang quản trị.
     *
     * Lưu ý: người bị đổi vai trò mà đang đăng nhập sẵn thì vẫn giữ quyền cũ cho tới khi
     * đăng xuất, vì phiên đăng nhập lưu bản sao tài khoản lúc đăng nhập.
     */
    @Transactional
    public User changeRole(Long actingUserId, Long targetUserId, Role newRole) {
        if (newRole == null) {
            throw new BusinessException("Bạn hãy chọn vai trò mới.");
        }
        if (actingUserId != null && actingUserId.equals(targetUserId)) {
            throw new BusinessException(
                    "Bạn không thể tự đổi vai trò của chính mình. Hãy nhờ một quản trị viên khác.");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("tài khoản", targetUserId));
        target.setRole(newRole);
        return userRepository.save(target);
    }
}
