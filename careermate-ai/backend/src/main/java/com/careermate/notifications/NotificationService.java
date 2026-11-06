package com.careermate.notifications;

import com.careermate.common.exception.ResourceNotFoundException;
import com.careermate.common.response.PageResponse;
import com.careermate.user.User;
import com.careermate.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repository;
    private final UserRepository userRepository;

    /** Creates a notification unless one with the same dedupe key already exists. Returns true if created. */
    @Transactional
    public boolean create(User user, NotificationType type, String title, String message, String link, String dedupeKey) {
        if (dedupeKey != null && repository.existsByUserIdAndDedupeKey(user.getId(), dedupeKey)) {
            return false;
        }
        Notification n = new Notification();
        n.setUser(userRepository.getReferenceById(user.getId()));
        n.setType(type);
        n.setTitle(truncate(title, 200));
        n.setMessage(truncate(message, 1000));
        n.setLink(link);
        n.setDedupeKey(dedupeKey);
        repository.save(n);
        return true;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> list(User user, boolean unreadOnly, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<Notification> p = unreadOnly ? repository.findByUserIdAndReadFalse(user.getId(), pr) : repository.findByUserId(user.getId(), pr);
        return PageResponse.from(p, NotificationService::toDto);
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return repository.countByUserIdAndReadFalse(user.getId());
    }

    @Transactional
    public NotificationDto markRead(User user, Long id) {
        Notification n = repository.findByIdAndUserId(id, user.getId()).orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        n.setRead(true);
        return toDto(repository.save(n));
    }

    @Transactional
    public int markAllRead(User user) {
        return repository.markAllRead(user.getId());
    }

    public static NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getLink(), n.isRead(), n.getCreatedAt());
    }

    private static String truncate(String s, int max) {
        return s == null ? "" : s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
