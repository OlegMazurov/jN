package org.mazurov.jn.primes;

public abstract class AbstractTest {
    public String getName() {
        return "UNKNOWN";
    }
    public void setParallelism(int parallelism) {}
    public abstract long findPrime(int n);
}
