package io.github.jerryraf.examples.elasticsearch.controller;

import com.raf.framework.core.common.result.RafResult;
import io.github.jerryraf.examples.elasticsearch.document.ProductDocument;
import io.github.jerryraf.examples.elasticsearch.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    @PostMapping
    public RafResult<ProductDocument> save(@RequestParam String name,
                                            @RequestParam String description,
                                            @RequestParam String category,
                                            @RequestParam BigDecimal price,
                                            @RequestParam int stock) {
        return RafResult.success(productSearchService.save(name, description, category, price, stock));
    }

    @GetMapping("/{id}")
    public RafResult<ProductDocument> getById(@PathVariable String id) {
        return RafResult.success(productSearchService.getById(id));
    }

    @GetMapping("/search")
    public RafResult<List<ProductDocument>> search(@RequestParam String keyword) {
        return RafResult.success(productSearchService.searchByKeyword(keyword));
    }

    @GetMapping("/category/{category}")
    public RafResult<List<ProductDocument>> getByCategory(@PathVariable String category) {
        return RafResult.success(productSearchService.getByCategory(category));
    }

    @DeleteMapping("/{id}")
    public RafResult<Void> delete(@PathVariable String id) {
        productSearchService.delete(id);
        return RafResult.success();
    }
}
