package com.tradev.domain.item.service;

import com.tradev.common.dto.CursorPageResponse;
import com.tradev.common.exception.ErrorCode;
import com.tradev.common.exception.TradevException;
import com.tradev.domain.category.entity.Category;
import com.tradev.domain.category.repository.CategoryRepository;
import com.tradev.domain.item.dto.*;
import com.tradev.domain.item.entity.Item;
import com.tradev.domain.item.entity.ItemImage;
import com.tradev.domain.item.entity.ItemStatus;
import com.tradev.domain.item.repository.ItemMapper;
import com.tradev.domain.item.repository.ItemRepository;
import com.tradev.domain.user.entity.User;
import com.tradev.domain.user.repository.UserRepository;
import com.tradev.domain.ai.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    @Value("${aws.s3.base-url}")
    private String s3BaseUrl;

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final FraudDetectionService fraudDetectionService;

    @Transactional
    public ItemDetailResponse createItem(Long sellerId, ItemRequest request) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new TradevException(ErrorCode.CATEGORY_NOT_FOUND));

        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .tradeType(request.getTradeType())
                .itemCondition(request.getItemCondition())
                .location(request.getLocation())
                .build();

        for (int i = 0; i < request.getImageS3Keys().size(); i++) {
            String s3Key = request.getImageS3Keys().get(i);
            item.getImages().add(new ItemImage(item, s3BaseUrl + "/" + s3Key, s3Key, i));
        }

        Item saved = itemRepository.save(item);
        // 사기 패턴 비동기 감지
        fraudDetectionService.detectAsync(saved.getId(), saved.getTitle(), saved.getDescription());
        return new ItemDetailResponse(saved);
    }

    public CursorPageResponse<ItemSummary> searchItems(ItemSearchCondition condition) {
        List<ItemSummary> items = itemMapper.searchItems(condition);
        return CursorPageResponse.of(items, condition.getPageSize(), item ->
                item.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "|" + item.getId()
        );
    }

    public ItemDetailResponse getItem(Long itemId, Long currentUserId) {
        Item item = itemRepository.findByIdWithDetails(itemId)
                .orElseThrow(() -> new TradevException(ErrorCode.ITEM_NOT_FOUND));

        incrementViewCountAsync(itemId);

        ItemDetailResponse response = new ItemDetailResponse(item);
        // TODO: wishlist 체크는 WishlistService에서
        return response;
    }

    @Transactional
    public ItemDetailResponse updateItem(Long itemId, Long userId, ItemRequest request) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new TradevException(ErrorCode.ITEM_NOT_FOUND));

        if (!item.isOwner(userId)) {
            throw new TradevException(ErrorCode.ITEM_NOT_OWNER);
        }
        if (item.getStatus() != ItemStatus.SALE) {
            throw new TradevException(ErrorCode.ITEM_NOT_AVAILABLE);
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new TradevException(ErrorCode.CATEGORY_NOT_FOUND));

        item.update(category, request.getTitle(), request.getDescription(),
                request.getPrice(), request.getTradeType(), request.getItemCondition(), request.getLocation());

        item.getImages().clear();
        for (int i = 0; i < request.getImageS3Keys().size(); i++) {
            String s3Key = request.getImageS3Keys().get(i);
            item.getImages().add(new ItemImage(item, s3BaseUrl + "/" + s3Key, s3Key, i));
        }

        return new ItemDetailResponse(item);
    }

    @Transactional
    public void deleteItem(Long itemId, Long userId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new TradevException(ErrorCode.ITEM_NOT_FOUND));

        if (!item.isOwner(userId)) {
            throw new TradevException(ErrorCode.ITEM_NOT_OWNER);
        }
        if (item.getStatus() == ItemStatus.RESERVED) {
            throw new TradevException(ErrorCode.ITEM_IN_PROGRESS);
        }

        itemRepository.delete(item);
    }

    @Transactional
    public void boostItem(Long itemId, Long userId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new TradevException(ErrorCode.ITEM_NOT_FOUND));

        if (!item.isOwner(userId)) {
            throw new TradevException(ErrorCode.ITEM_NOT_OWNER);
        }

        String key = "item:boost:" + userId + ":" + itemId;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "1", 1, TimeUnit.DAYS);
        if (Boolean.FALSE.equals(isNew)) {
            throw new TradevException(ErrorCode.ITEM_BOOST_LIMIT);
        }

        item.boost();
    }

    @Transactional
    public void toggleVisibility(Long itemId, Long userId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new TradevException(ErrorCode.ITEM_NOT_FOUND));

        if (!item.isOwner(userId)) {
            throw new TradevException(ErrorCode.ITEM_NOT_OWNER);
        }

        item.toggleHidden();
    }

    @Async
    @Transactional
    public void incrementViewCountAsync(Long itemId) {
        itemRepository.findById(itemId).ifPresent(Item::incrementViewCount);
    }
}
