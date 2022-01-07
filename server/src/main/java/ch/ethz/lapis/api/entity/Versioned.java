package ch.ethz.lapis.api.entity;


public class Versioned<T> {

    private final long dataVersion;
    private final T content;

    public Versioned(long dataVersion, T content) {
        this.dataVersion = dataVersion;
        this.content = content;
    }

    public long getDataVersion() {
        return dataVersion;
    }

    public T getContent() {
        return content;
    }
}
