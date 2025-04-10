package org.mazurov.jn.primes;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Supplier;

public class VirtualThreadTest extends AbstractTest {

    private static final Supplier<BlockingQueue<Long>> SYNC_QUEUE_SUPPLIER =
            SynchronousQueue::new;
    private static final Supplier<BlockingQueue<Long>> ARRAY_QUEUE_SUPPLIER =
            () -> new ArrayBlockingQueue<>(1);

    private final Supplier<BlockingQueue<Long>> queueSupplier;
    private final String name;

    public VirtualThreadTest(boolean sync) {
        if (sync) {
            queueSupplier = SYNC_QUEUE_SUPPLIER;
            name = "VirtualThread Test (SynchronousQueue)";
        } else {
            queueSupplier = ARRAY_QUEUE_SUPPLIER;
            name = "VirtualThread Test (ArrayBlockingQueue)";
        }
    }

    public String getName() {
        return name;
    }

    public void setParallelism(int parallelism) {
        ForkJoinPool.commonPool().setParallelism(parallelism);
    }

    @Override
    public long findPrime(int n) {
        try {
            var threads = new ArrayList<Thread>();
            var queue = queueSupplier.get();
            final var q1 = queue;
            threads.add(Thread.ofVirtual().start(() -> generate(q1)));
            long prime = 1;
            for (int i = 0; i < n; i++) {
                prime = queue.take();
                final var inQueue = queue;
                final var value = prime;
                final var outQueue = queueSupplier.get();
                threads.add(Thread.ofVirtual().start(() -> filter(inQueue, outQueue, value)));
                queue = outQueue;
            }
            threads.forEach(Thread::interrupt);
            return prime;
        } catch (InterruptedException iex) {
            return -1;
        }
    }

    static void generate(BlockingQueue<Long> queue) {
        try {
            for (long i = 2; ; i++) {
                queue.put(i);
            }
        } catch (InterruptedException ignored) {
        }
    }

    static void filter(BlockingQueue<Long> inQueue, BlockingQueue<Long> outQueue, Long prime) {
        try {
            while (true) {
                var i = inQueue.take();
                if (i % prime != 0) {
                    outQueue.put(i);
                }
            }
        } catch (InterruptedException ignored) {
        }
    }
}
