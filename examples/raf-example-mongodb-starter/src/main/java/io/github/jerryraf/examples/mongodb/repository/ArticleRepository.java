package io.github.jerryraf.examples.mongodb.repository;

import io.github.jerryraf.examples.mongodb.document.ArticleDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends MongoRepository<ArticleDocument, String> {
    Optional<ArticleDocument> findByTitle(String title);
    List<ArticleDocument> findByAuthor(String author);
    List<ArticleDocument> findByTagsContaining(String tag);
}
