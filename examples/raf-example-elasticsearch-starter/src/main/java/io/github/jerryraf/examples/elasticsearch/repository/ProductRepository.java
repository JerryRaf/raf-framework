package io.github.jerryraf.examples.elasticsearch.repository;

import io.github.jerryraf.examples.elasticsearch.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface ProductRepository extends ElasticsearchRepository<ProductDocument, String> {
    List<ProductDocument> findByCategory(String category);
    List<ProductDocument> findByNameContaining(String keyword);
}
