package hsf302.bookingsystemcinema.repository;

import hsf302.bookingsystemcinema.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
}
