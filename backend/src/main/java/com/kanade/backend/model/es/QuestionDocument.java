package com.kanade.backend.model.es;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "question_index")
@Setting(shards = 3, replicas = 1, refreshInterval = "5s")
public class QuestionDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    @Field(type = FieldType.Integer)
    private Integer type;

    @Field(type = FieldType.Integer)
    private Integer difficulty;

    @Field(type = FieldType.Keyword)
    private String subject;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String chapter;

    @Field(type = FieldType.Keyword)
    private List<String> knowledgePoints;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Long)
    private Long creatorId;

    @Field(type = FieldType.Integer)
    private Integer status;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createTime;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime updateTime;

    // Non-indexed display fields
    @Field(type = FieldType.Keyword, index = false)
    private String questionMd5;

    @Field(type = FieldType.Text, index = false)
    private String options;

    @Field(type = FieldType.Text, index = false)
    private String answer;

    @Field(type = FieldType.Text, index = false)
    private String analysis;

    @Field(type = FieldType.Long, index = false)
    private Long correctCount;

    @Field(type = FieldType.Long, index = false)
    private Long totalCount;
}
