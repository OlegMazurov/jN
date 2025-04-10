package org.mazurov.jn.primes;

public class SequentialTest extends AbstractTest {

    public String getName() {
        return "Sequential Test";
    }

    static class Filter {
        int prime;
        Filter next;
    }

    public long findPrime(int n) {
        Filter filters = new Filter();

        loop:
        for (int number = 2, idx = 0; ; ++number) {
            Filter f;
            for (f = filters; f.next != null; f = f.next) {
                if (number % f.prime == 0) {
                    continue loop;
                }
            }
            if (++idx == n) {
                return number;
            }
            f.prime = number;
            f.next = new Filter();
        }
    }
}
