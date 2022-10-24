package ch.ethz.lapis.api.findaquery.eval;

/**
 * Basic metrics including precision, recall, and the Jaccard index
 */
public record PRMetrics(int numberWanted, int numberFound, int numberWantedAndFound) {

    public double precision() {
        return numberWantedAndFound * 1.0 / numberFound;
    }

    public double recall() {
        return numberWantedAndFound * 1.0 / numberWanted;
    }

    public double jaccard() {
        return numberWantedAndFound * 1.0 / (numberWanted + numberFound - numberWantedAndFound);
    }

}
