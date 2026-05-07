package io.github.jerryraf.examples.elasticsearch.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(indexName = "products")
public class ProductDocument {
    @Id private String id;
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;
    @Field(type = FieldType.Keyword) private String category;
    @Field(type = FieldType.Double)  private BigDecimal price;
    @Field(type = FieldType.Integer) private Integer stock;
}
