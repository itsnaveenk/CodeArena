package com.codearena.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import com.codearena.document.Submission;
import com.codearena.entity.Verdict;

@Repository
public class LeaderboardRepository {

    private final MongoTemplate mongoTemplate;

    public LeaderboardRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<LeaderboardRow> topSolved(LocalDateTime from, int limit) {
        Criteria criteria = Criteria.where("verdict").is(Verdict.ACCEPTED);
        if (from != null) {
            criteria = criteria.and("createdAt").gte(from);
        }

        MatchOperation match = Aggregation.match(criteria);

        GroupOperation distinctSolved = Aggregation.group("userId", "problemId")
            .min("runtime").as("bestRuntimeForProblem");

        GroupOperation perUser = Aggregation.group("_id.userId")
            .count().as("solved")
            .min("bestRuntimeForProblem").as("bestRuntime");

        ProjectionOperation project = Aggregation.project()
            .and("_id").as("userId")
            .and("solved").as("solved")
            .and("bestRuntime").as("bestRuntime");

        SortOperation sort = Aggregation.sort(org.springframework.data.domain.Sort.by(
            org.springframework.data.domain.Sort.Order.desc("solved"),
            org.springframework.data.domain.Sort.Order.asc("bestRuntime")
        ));

        Aggregation agg = Aggregation.newAggregation(match, distinctSolved, perUser, project, sort, Aggregation.limit(limit));

        AggregationResults<LeaderboardRow> results = mongoTemplate.aggregate(agg, mongoTemplate.getCollectionName(Submission.class), LeaderboardRow.class);
        return results.getMappedResults();
    }

    public record LeaderboardRow(Long userId, long solved, Double bestRuntime) {}
}
