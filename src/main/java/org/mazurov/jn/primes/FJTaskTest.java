package org.mazurov.jn.primes;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

public class FJTaskTest extends AbstractTest {

    ForkJoinPool pool;

    public FJTaskTest() {
        pool = new ForkJoinPool();
    }

    public String getName() {
        return "ForkJoinTask Test";
    }

    abstract class Task extends ForkJoinTask<Long> {
        Task in;
        Task out;
        long data;
        final AtomicInteger dependencies;

        Task(int n) {
            this.dependencies = new AtomicInteger(n);
        }

        public void reset(int n) {
            reinitialize();
            dependencies.set(n);
        }

        public void send() {
            if (dependencies.decrementAndGet() == 0) {
                if (Thread.currentThread() instanceof ForkJoinWorkerThread) {
                    fork();
                } else {
                    pool.submit(this);
                }
            }
        }

        public void setOutput(Task out) {
            this.out = out;
            send();
        }

        public void setData(long data) {
            this.data = data;
            send();
        }

        @Override
        public void setRawResult(Long value) {
            data = value;
        }

        @Override
        public Long getRawResult() {
            return data;
        }
    }

    class GenerateTask extends Task {

        public GenerateTask(int n) {
            super(1);
            data = n;
        }

        public boolean exec() {
            long d = data++;
            reset(1);
            out.setData(d);
            return false;
        }
    }

    class FilterTask extends Task {
        final long prime;

        FilterTask(Task input, long p) {
            super(2);
            in = input;
            prime = p;
        }

        public boolean exec() {
            // Copy state before reset
            var d = data;
            var input = in;
            var output = out;
            reset(2);

            // Notify the supplier we are ready
            input.setOutput(this);

            if (d % prime != 0) {
                // Pass the number down the pipeline
                output.setData(d);
            } else {
                // We dropped the number, reuse output
                this.setOutput(output);
            }
            return false;
        }
    }

    class ReportTask extends Task {

        int nth;
        int idx;

        public ReportTask(int nth, Task input, Task output) {
            super(1);
            this.nth = nth;
            this.idx = 0;
            this.in = input;
            this.out = output;
        }

        public boolean exec() {
            var d = data;
            var index = ++idx;
            reset(1);

            if (index < nth) {
                FilterTask filter = new FilterTask(in, d);
                in = filter;
                filter.in.setOutput(filter);
                filter.setOutput(this);
            } else {
                out.setData(d);
            }
            return false;
        }
    }

    @Override
    public void setParallelism(int parallelism) {
        pool.setParallelism(parallelism);
    }

    @Override
    public long findPrime(int nth) {
        Task generator = new GenerateTask(2);
        Task result = new Task(1) {
            public boolean exec() {
                for (Task t = generator; t != null && t != this; t = t.out) {
                    t.cancel(false);
                }
                return true;
            }
        };
        Task reporter = new ReportTask(nth, generator, result);
        generator.setOutput(reporter);
        result.join();
        return result.getRawResult();
    }
}
