package ch.ethz.lapis.util.tuples;

public class MutableTriplet<A, B, C> {

    private A value0;
    private B value1;
    private C value2;

    public MutableTriplet(A value0, B value1, C value2) {
        this.value0 = value0;
        this.value1 = value1;
        this.value2 = value2;
    }

    public A getValue0() {
        return value0;
    }

    public MutableTriplet<A, B, C> setValue0(A value0) {
        this.value0 = value0;
        return this;
    }

    public B getValue1() {
        return value1;
    }

    public MutableTriplet<A, B, C> setValue1(B value1) {
        this.value1 = value1;
        return this;
    }

    public C getValue2() {
        return value2;
    }

    public MutableTriplet<A, B, C> setValue2(C value2) {
        this.value2 = value2;
        return this;
    }
}
