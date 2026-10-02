package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.repository.SupportConversationRepository;
import edu.hcmute.cnpm.cinema.repository.SupportMessageRepository;
import edu.hcmute.cnpm.cinema.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Hộp thư hai chiều giữa khách hàng và nhân viên rạp. */
@Service
public class CustomerSupportService {

    private final SupportConversationRepository conversationRepository;
    private final SupportMessageRepository messageRepository;
    private final UserRepository userRepository;

    public CustomerSupportService(SupportConversationRepository conversationRepository,
                                  SupportMessageRepository messageRepository,
                                  UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SupportConversation create(Long customerId, String subject, SupportCategory category, String content) {
        User customer = findUser(customerId);
        requireCustomer(customer);
        validateText(subject, "Tiêu đề", 5, 200);
        validateText(content, "Nội dung", 10, 2000);
        if (category == null) {
            throw new BusinessException("Bạn hãy chọn nhóm vấn đề cần hỗ trợ.");
        }

        LocalDateTime now = LocalDateTime.now();
        SupportConversation conversation = new SupportConversation();
        conversation.setCustomer(customer);
        conversation.setSubject(subject.trim());
        conversation.setCategory(category);
        conversation.setStatus(SupportStatus.WAITING_STAFF);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversation = conversationRepository.save(conversation);
        saveMessage(conversation, customer, content, now);
        return conversation;
    }

    @Transactional(readOnly = true)
    public List<SupportConversation> findForCustomer(Long customerId) {
        requireCustomer(findUser(customerId));
        return conversationRepository.findByCustomerIdOrderByUpdatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public SupportConversation findForCustomer(Long conversationId, Long customerId) {
        requireCustomer(findUser(customerId));
        SupportConversation conversation = findWithMessages(conversationId);
        if (!conversation.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("Bạn không có quyền xem yêu cầu hỗ trợ này.");
        }
        conversation.getMessages().size();
        return conversation;
    }

    @Transactional
    public SupportConversation replyAsCustomer(Long conversationId, Long customerId, String content) {
        SupportConversation conversation = findForCustomer(conversationId, customerId);
        validateText(content, "Nội dung", 1, 2000);
        if (conversation.getStatus() == SupportStatus.RESOLVED) {
            throw new BusinessException("Yêu cầu này đã đóng. Bạn hãy tạo yêu cầu mới nếu vẫn cần hỗ trợ.");
        }
        LocalDateTime now = LocalDateTime.now();
        saveMessage(conversation, conversation.getCustomer(), content, now);
        conversation.setStatus(SupportStatus.WAITING_STAFF);
        conversation.setUpdatedAt(now);
        return conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public List<SupportConversation> findForStaff(SupportStatus status) {
        return status == null ? conversationRepository.findAllByOrderByUpdatedAtDesc()
                : conversationRepository.findByStatusOrderByUpdatedAtDesc(status);
    }

    /** Số cuộc trao đổi có phản hồi mới từ khách và đang chờ rạp xử lý. */
    @Transactional(readOnly = true)
    public long countWaitingForStaff() {
        return conversationRepository.countByStatus(SupportStatus.WAITING_STAFF);
    }

    @Transactional(readOnly = true)
    public SupportConversation findForStaff(Long conversationId) {
        SupportConversation conversation = findWithMessages(conversationId);
        conversation.getMessages().size();
        return conversation;
    }

    @Transactional
    public SupportConversation replyAsStaff(Long conversationId, Long staffId, String content) {
        User staff = findUser(staffId);
        requireEmployee(staff);
        validateText(content, "Nội dung", 1, 2000);
        SupportConversation conversation = findWithMessages(conversationId);
        LocalDateTime now = LocalDateTime.now();
        conversation.setAssignedStaff(staff);
        conversation.setStatus(SupportStatus.WAITING_CUSTOMER);
        conversation.setUpdatedAt(now);
        saveMessage(conversation, staff, content, now);
        return conversationRepository.save(conversation);
    }

    @Transactional
    public SupportConversation resolve(Long conversationId, Long staffId) {
        User staff = findUser(staffId);
        requireEmployee(staff);
        SupportConversation conversation = findWithMessages(conversationId);
        if (conversation.getAssignedStaff() == null) {
            conversation.setAssignedStaff(staff);
        }
        conversation.setStatus(SupportStatus.RESOLVED);
        conversation.setUpdatedAt(LocalDateTime.now());
        return conversationRepository.save(conversation);
    }

    private SupportConversation findWithMessages(Long conversationId) {
        return conversationRepository.findWithMessagesById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("yêu cầu hỗ trợ", conversationId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("tài khoản", userId));
    }

    private void saveMessage(SupportConversation conversation, User sender, String content, LocalDateTime sentAt) {
        SupportMessage message = new SupportMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content.trim());
        message.setSentAt(sentAt);
        messageRepository.save(message);
    }

    private void requireEmployee(User user) {
        if (user.getRole() != Role.STAFF && user.getRole() != Role.ADMIN) {
            throw new BusinessException("Chỉ nhân viên hoặc quản trị viên được trả lời khách hàng.");
        }
    }

    private void requireCustomer(User user) {
        if (user.getRole() != Role.CUSTOMER) {
            throw new BusinessException("Chỉ tài khoản khách hàng được gửi yêu cầu hỗ trợ.");
        }
    }

    private void validateText(String value, String fieldName, int minimumLength, int maximumLength) {
        int length = value == null ? 0 : value.trim().length();
        if (length < minimumLength || length > maximumLength) {
            throw new BusinessException(fieldName + " phải dài từ " + minimumLength
                    + " đến " + maximumLength + " ký tự.");
        }
    }
}
