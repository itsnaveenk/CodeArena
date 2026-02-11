package com.codearena.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import com.codearena.document.ContestSubmission;
import com.codearena.document.Submission;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class MongoIndexConfig {

    @Bean
    public CommandLineRunner createMongoIndexes(MongoTemplate mongoTemplate) {
        return args -> {
            createContestSubmissionIndexes(mongoTemplate);
            createSubmissionIndexes(mongoTemplate);
            log.info("MongoDB indexes created successfully");
        };
    }

    private void createContestSubmissionIndexes(MongoTemplate mongoTemplate) {
        IndexOperations indexOps = mongoTemplate.indexOps(ContestSubmission.class);

        indexOps.ensureIndex(new Index()
                .on("contestId", Sort.Direction.ASC)
                .on("userId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("idx_contest_user_created"));

        indexOps.ensureIndex(new Index()
                .on("contestId", Sort.Direction.ASC)
                .on("problemId", Sort.Direction.ASC)
                .on("userId", Sort.Direction.ASC)
                .on("pointsEarned", Sort.Direction.DESC)
                .named("idx_contest_problem_user_points"));

        indexOps.ensureIndex(new Index()
                .on("taskId", Sort.Direction.ASC)
                .unique()
                .sparse()
                .named("idx_taskId"));

        log.info("ContestSubmission indexes created");
    }

    private void createSubmissionIndexes(MongoTemplate mongoTemplate) {
        IndexOperations indexOps = mongoTemplate.indexOps(Submission.class);

        indexOps.ensureIndex(new Index()
                .on("problemId", Sort.Direction.ASC)
                .on("userId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("idx_problem_user_created"));

        indexOps.ensureIndex(new Index()
                .on("taskId", Sort.Direction.ASC)
                .unique()
                .sparse()
                .named("idx_taskId"));

        log.info("Submission indexes created");
    }
}
