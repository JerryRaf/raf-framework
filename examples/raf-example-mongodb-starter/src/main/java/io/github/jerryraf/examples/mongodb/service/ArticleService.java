package io.github.jerryraf.examples.mongodb.service;

import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.result.RafResponseEnum;
import io.github.jerryraf.examples.mongodb.document.ArticleDocument;
import io.github.jerryraf.examples.mongodb.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Article service demonstrating MongoDB CRUD operations.
 *
 * @author Jerry
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleDocument create(String title, String content, String author, List<String> tags) {
        ArticleDocument doc = ArticleDocument.builder()
                .title(title)
                .content(content)
                .author(author)
                .tags(tags)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        ArticleDocument saved = articleRepository.save(doc);
        log.info("Article created: id={}, title={}", saved.getId(), saved.getTitle());
        return saved;
    }

    public ArticleDocument getById(String id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(RafResponseEnum.PARAM_ERROR, "Article not found: " + id));
    }

    public List<ArticleDocument> getByAuthor(String author) {
        return articleRepository.findByAuthor(author);
    }

    public List<ArticleDocument> getByTag(String tag) {
        return articleRepository.findByTagsContaining(tag);
    }

    public void delete(String id) {
        if (!articleRepository.existsById(id)) {
            throw new BusinessException(RafResponseEnum.PARAM_ERROR, "Article not found: " + id);
        }
        articleRepository.deleteById(id);
        log.info("Article deleted: id={}", id);
    }
}
