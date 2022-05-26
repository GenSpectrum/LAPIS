package ch.ethz.lapis.api.entity;

public class AccessKey {

    public enum LEVEL {
        AGGREGATED,
        FULL
    }

    private String key;
    private LEVEL level;

    public AccessKey(String key, LEVEL level) {
        this.key = key;
        this.level = level;
    }

    public String getKey() {
        return key;
    }

    public AccessKey setKey(String key) {
        this.key = key;
        return this;
    }

    public LEVEL getLevel() {
        return level;
    }

    public AccessKey setLevel(LEVEL level) {
        this.level = level;
        return this;
    }
}
