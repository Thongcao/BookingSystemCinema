package hsf302.bookingsystemcinema.service.impl;

import hsf302.bookingsystemcinema.dto.ItemRequest;
import hsf302.bookingsystemcinema.dto.ItemResponse;
import hsf302.bookingsystemcinema.entity.Item;
import hsf302.bookingsystemcinema.entity.enums.ItemType;
import hsf302.bookingsystemcinema.exception.ResourceNotFoundException;
import hsf302.bookingsystemcinema.repository.ItemRepository;
import hsf302.bookingsystemcinema.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ItemResponse> getAllItems() {
        return itemRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponse getItemById(Long id) {
        return toResponse(findItemOrThrow(id));
    }

    @Override
    public ItemResponse createItem(ItemRequest request) {
        Item item = Item.builder()
                .name(request.getName())
                .price(request.getPrice())
                .type(ItemType.valueOf(request.getType().toUpperCase()))
                .build();
        return toResponse(itemRepository.save(item));
    }

    @Override
    public ItemResponse updateItem(Long id, ItemRequest request) {
        Item item = findItemOrThrow(id);
        item.setName(request.getName());
        item.setPrice(request.getPrice());
        item.setType(ItemType.valueOf(request.getType().toUpperCase()));
        return toResponse(itemRepository.save(item));
    }

    @Override
    public void deleteItem(Long id) {
        Item item = findItemOrThrow(id);
        itemRepository.delete(item);
    }

    private Item findItemOrThrow(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", id));
    }

    private ItemResponse toResponse(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .type(item.getType().name())
                .build();
    }
}
