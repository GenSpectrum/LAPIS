package ch.ethz.lapis.api.findaquery.eval;

import ch.ethz.lapis.api.findaquery.FindAQuery;
import ch.ethz.lapis.api.query.Database;
import ch.ethz.lapis.api.query.QueryEngine;
import java.util.ArrayList;
import java.util.List;


public class Evaluator {

    private final Database database;
    private final FindAQuery findAQuery;
    private final QueryEngine queryEngine = new QueryEngine();


    public Evaluator(Database database) {
        this.database = database;
        this.findAQuery = new FindAQuery(database.getNucMutationStore());
    }


    public void runCollectionChallenges() {
        var challenges = generateCollectionChallenges();
        List<ChallengeAndResult<CollectionChallengeMetadata>> results = new ArrayList<>();
        for (var challenge : challenges) {
            String query = findAQuery.proposeQuery(database, challenge);
            PRMetrics metrics = evaluateProposedQuery(challenge, query);
            results.add(new ChallengeAndResult<>(challenge, query, metrics));
        }
        reportCollectionChallengesResults(results);
    }


    public void runPangoLineageChallenges() {
        var challenges = generatePangoLineageChallenges();
        List<ChallengeAndResult<PangoLineageChallengeMetadata>> results = new ArrayList<>();
        for (var challenge : challenges) {
            String query = findAQuery.proposeQuery(database, challenge);
            PRMetrics metrics = evaluateProposedQuery(challenge, query);
            results.add(new ChallengeAndResult<>(challenge, query, metrics));
        }
        reportPangoLineageChallengesResults(results);
    }


    private List<Challenge<CollectionChallengeMetadata>> generateCollectionChallenges() {
        throw new RuntimeException("TODO");
    }


    private List<Challenge<PangoLineageChallengeMetadata>> generatePangoLineageChallenges() {
        throw new RuntimeException("TODO");
    }


    private PRMetrics evaluateProposedQuery(Challenge<?> challenge, String proposedQuery) {
        throw new RuntimeException("TODO");
    }


    private void reportCollectionChallengesResults(List<ChallengeAndResult<CollectionChallengeMetadata>> results) {
        throw new RuntimeException("TODO");
    }


    private void reportPangoLineageChallengesResults(List<ChallengeAndResult<PangoLineageChallengeMetadata>> results) {
        throw new RuntimeException("TODO");
    }


    private record ChallengeAndResult<T>(
        Challenge<T> challenge,
        String proposedQuery,
        PRMetrics prMetrics
    ) {

    }
}
