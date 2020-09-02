package ohcna;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Optional;

public interface PointRepository extends PagingAndSortingRepository<Point, Long>{

    Optional<Point>  findById(Long id);
    List<Point> findByIdOrderByChangeDtm(Long id);

}