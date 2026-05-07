package io.github.jerryraf.examples.mongodb.controller;

import com.raf.framework.core.common.result.RafResult;
import io.github.jerryraf.examples.mongodb.document.ArticleDocument;
import io.github.jerryraf.examples.mongodb.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Article REST controller.
 *
 * @author Jerry
 */
@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping
    public RafResult<ArticleDocument> create(@RequestParam String title,
                                              @RequestParam String content,
                                              @RequestParam String author,
                                              @RequestParam(required = false) List<String> tags) {
        return RafResult.success(articleService.create(title, content, author, tags));
    }

    @GetMapping("/{id}")
    public RafResult<ArticleDocument> getById(@PathVariable String id) {
        return RafResult.success(articleService.getById(id));
    }

    @GetMapping("/author/{author}")
    public RafResult<List<ArticleDocument>> getByAuthor(@PathVariable String author) {
        return RafResult.success(articleService.getByAuthor(author));
    }

    @GetMapping("/tag/{tag}")
    public RafResult<List<ArticleDocument>> getByTag(@PathVariable String tag) {
        return RafResult.success(articleService.getByTag(tag));
    }

    @DeleteMapping("/{id}")
    public RafResult<Void> delete(@PathVariable String id) {
        articleService.delete(id);
        return RafResult.success();
    }
}
