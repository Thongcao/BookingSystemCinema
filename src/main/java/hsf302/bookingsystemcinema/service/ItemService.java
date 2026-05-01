package hsf302.bookingsystemcinema.service;

import hsf302.bookingsystemcinema.dto.ItemRequest;
import hsf302.bookingsystemcinema.dto.ItemResponse;
import java.util.List;

public interface ItemService {
    List<ItemResponse> getAllItems();
    ItemResponse getItemById(Long id);
    ItemResponse createItem(ItemRequest request);
    ItemResponse updateItem(Long id, ItemRequest request);
    void deleteItem(Long id);
}
