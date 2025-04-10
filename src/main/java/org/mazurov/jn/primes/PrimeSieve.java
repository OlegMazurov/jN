package org.mazurov.jn.primes;

public class PrimeSieve {

    private static final int DEFAULT_PRIMES = 100000;
    private static final int DEFAULT_ITERATIONS = 5;

    public static void main(String[] args) {

        AbstractTest test = null;
        int nth = DEFAULT_PRIMES;
        int iterations = DEFAULT_ITERATIONS;
        int parallelism = Runtime.getRuntime().availableProcessors();
        // Parse arguments
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            switch (arg) {
                case "-i" -> iterations = Integer.parseInt(args[++i]);
                case "-p" -> parallelism = Integer.parseInt(args[++i]);
                case "-fj", "fjtask" -> test = new FJTaskTest();
                case "-jn", "jntask" -> test = new JNTest();
                case "-sq", "sequential" -> test = new SequentialTest();
                case "-vs", "vsync" -> test = new VirtualThreadTest(true);
                case "-va", "varray" -> test = new VirtualThreadTest(false);
                default -> nth = Integer.parseInt(arg);
            }
        }
        // Validate arguments
        if (test == null) {
            test = new SequentialTest();
        }
        if (iterations <= 0) {
            System.out.printf("ERROR: number of iterations must be positive: %d%n", iterations);
            return;
        }
        if (test instanceof SequentialTest) {
            parallelism = 1;
        }
        if (parallelism <= 0) {
            System.out.printf("ERROR: parallelism must be positive: %d%n", parallelism);
            return;
        }
        if (nth <= 0) {
            System.out.printf("ERROR: n-th must be positive: %d%n", parallelism);
            return;
        }

        test.setParallelism(parallelism);

        // Run the benchmark
        System.out.printf("=== Starting %s ===%n", test.getName());
        System.out.printf("N-th:        %d%n", nth);
        System.out.printf("ITERATIONS:  %d%n", iterations);
        System.out.printf("PARALLELISM: %d%n", parallelism);
        System.out.println("---------------------");
        long sum = 0;
        long result = 0;
        for (int iteration = 0; iteration < iterations; iteration++) {
            var start = System.currentTimeMillis();
            long prime = test.findPrime(nth);
            long time = System.currentTimeMillis() - start;
            if (iteration == 0) {
                result = prime;
            } else if (prime != result) {
                System.out.printf("FAILED non-deterministic results: %d != %d%n", prime, result);
                return;
            }
            sum += time;
            System.out.printf("%d: prime = %d, time: %d ms%n", iteration, prime, time);
        }
        System.out.format("AVERAGE TIME: %d ms%n", sum / iterations);
    }
}