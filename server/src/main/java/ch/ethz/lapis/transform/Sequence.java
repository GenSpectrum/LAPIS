package ch.ethz.lapis.transform;

public class Sequence {

    private String name;
    private String seq;

    public String getName() {
        return name;
    }

    public Sequence setName(String name) {
        this.name = name;
        return this;
    }

    public String getSeq() {
        return seq;
    }

    public Sequence setSeq(String seq) {
        this.seq = seq;
        return this;
    }
}
