package io.github.jerryraf.examples.elasticsearch.service;

import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.result.RafResponseEnum;
import io.github.jerryraf.examples.elasticsearch.document.ProductDocument;
import io.github.jerryraf.examples.elasticsearch.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public ProductDocument save(String name, String desc, String category, BigDecimal price, int stock) {
        ProductDocument doc = ProductDocument.builder()
                .name(name).description(desc).category(category)
                .price(price).stock(stock).build();
        ProductDocument saved = productRepository.save(doc);
        log.info("Product indexed: id={}", saved.getId());
        return saved;
    }

    public ProductDocument getById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(RafResponseEnum.PARAM_ERROR, "Product not found: " + id));
    }

    public List<ProductDocument> searchByKeyword(String keyword) {
        CriteriaQuery query = new CriteriaQuery(
                new Criteria("name").contains(keyword)
                        .or(new Criteria("description").contains(keyword))
        );
        return elasticsearchOperations.search(query, ProductDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    public List<ProductDocument> getByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public void delete(String id) {
        productRepository.deleteById(id);
        log.info("Product deleted from index: id={}", id);
    }
}
