package org.mazurov.jn.primes;

import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class JNTest extends AbstractTest {
    @Override
    public String getName() {
        return "jN Test";
    }

    @Override
    public void setParallelism(int parallelism) {
        threads = new Worker[parallelism];
        Worker cur = null;
        for (int i = 0; i < threads.length; ++i) {
            cur = new Worker(i, cur);
            threads[i] = cur;
        }
        threads[0].next = cur;
        for (Worker worker : threads) {
            worker.start();
        }
    }

    static abstract class Task extends AbstractTask {
        Task in;
        Task out;
        long data;

        Task(int n) {
            super(n);
        }

        public void setOutput(Task out) {
            this.out = out;
            send();
        }

        public void setData(long data) {
            this.data = data;
            send();
        }

        public void cancel() {
            reset(-1);
        }
    }

    static class Generate extends Task {

        public Generate(int n) {
            super(1);
            data = n;
        }

        public void exec() {
            long d = data++;
            reset(1);
            out.setData(d);
        }
    }

    static class Filter extends Task {
        final long prime;

        Filter(Task input, long p) {
            super(2);
            in = input;
            prime = p;
        }

        public void exec() {
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
        }
    }

    static class Report extends Task {

        final int nth;
        int idx;

        public Report(int nth, Task input, Task output) {
            super(1);
            this.nth = nth;
            this.idx = 0;
            this.in = input;
            this.out = output;
        }

        public void exec() {
            var d = data;
            var index = ++idx;
            reset(1);

            if (index < nth) {
                Filter filter = new Filter(in, d);
                in = filter;
                filter.in.setOutput(filter);
                filter.setOutput(this);
            } else {
                out.setData(d);
            }
        }
    }

    @Override
    public long findPrime(int nth) {
        Task generator = new Generate(2);
        CompletableFuture<Long> future = new CompletableFuture<>();
        Task result = new Task(1) {
            public void exec() {
                future.complete(data);
                for (Task task = generator; task != null; task = task.out) {
                    task.cancel();
                }
            }
        };
        Task reporter = new Report(nth, generator, result);
        generator.setOutput(reporter);
        return future.join();
    }

    private static Worker[] threads;

    static abstract class AbstractTask {

        private final AtomicInteger dependencies;

        protected AbstractTask(int n) {
            this.dependencies = new AtomicInteger(n);
        }

        protected void reset(int n) {
            dependencies.set(n);
        }

        public abstract void exec();

        public void send() {
            if (dependencies.decrementAndGet() == 0) {
                if (Thread.currentThread() instanceof Worker worker) {
                    worker.addTask(this);
                } else {
                    threads[0].invokeTask(this);
                }
            }
        }
    }

    static class Worker extends Thread {
        int idx;
        Worker next;
        AbstractTask lastPushed;

        Deque<AbstractTask> queue;

        Worker(int idx, Worker next) {
            this.idx = idx;
            this.next = next;
            this.queue = new ConcurrentLinkedDeque<>();
            this.lastPushed = null;
            setDaemon(true);
        }

        void invokeTask(AbstractTask task) {
            queue.addLast(task);
        }

        void addTask(AbstractTask task) {
            if (lastPushed != null) {
                queue.addLast(lastPushed);
            }
            lastPushed = task;
        }

        AbstractTask getTask() {
            AbstractTask task = lastPushed;
            lastPushed = null;
            if (task == null) {
                task = queue.pollLast();
                if (task == null) {
                    task = next.queue.pollFirst();
                }
            }
            return task;
        }

        @Override
        public void run() {
            for (;;) {
                AbstractTask task = getTask();
                if (task != null) {
                    try {
                        task.exec();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                } else {
                    onSpinWait();
                }
            }
        }
    }
}
