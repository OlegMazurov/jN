# jN - a Java framework for parallel programming in shared memory and distributed systems

Project jN ("Jane") is a Java framework for parallel programming. It is based on dynamic creation and asynchronous execution of tasks with dependencies and data flow.
The framework provides a unified concurrency model that utilizes multithreading and inter process communication without exposing them to the programmer.

## Features 
* mostly lock-free implementation of a communicating sequential processes (CSP) model
* asynchronous I/O and computation without async/await/yield 
* scales automatically with the number of CPUs/nodes
* scalability is limited by parallel algorithms implemented by applications, computational speed of CPUs/nodes, communication cost of threads/processes, scheduler overhead

One way to think about the framework is that it implements a simplified CSP model where all processes are single-shot tasks. 
In that sense it might be called Communicating Sequential Tasks. 
Modelling processes with recurring tasks not only is possible but represents one of framework patterns. 
Although the reverse, i.e. using processes as one-shot tasks, is also possible, communication overhead and extra complexity in both implementation and application code make this approach less interesting from a practical perspective.   

Task dependencies are an essential part of orchestrating parallel computation. 
jN allows for building the directed acyclic graph (DAG) of dependent tasks dynamically, unfolding it during computation.
Tasks are pushed to execution once all their dependencies are computed. Tasks never pull their dependency's computation results, so they do not block.

Deterministic parallel computation was the original design goal of the framework. 
When used in a real product development, it was extended to allow for controlled non-determinism in task execution order. 
Whether that extension, introducing a new way of task communication, should be embraced by the framework or remain an outside pattern is not yet clear.

The existing Java fork-join framework is a natural choice for initial implementation. 
There are, however, some performance considerations and extensions that may make reimplementing the basic layer desirable.
For one thing, jN does not need `join()`, though it often comes as a natural bridge between synchronous and asynchronous worlds within a system that does not fully commit to the new paradigm.

As a framework, jN puts a lot of responsibility on application code. 
It also may make code difficult to read, reason about, debug, and interpret in a profiler.
This price is deemed to be justified by better performance and scalability.
Language extension, compiler and tool support might help to significantly decrease complexity. This is not currently on the horizon, however.

## Demo
* [Prime Sieve](src/main/java/org/mazurov/jn/primes/PrimeSieve.java) inspired by [golang demo](https://go.dev/doc/play/sieve.go)
* [SequentialTest](src/main/java/org/mazurov/jn/primes/SequentialTest.java) - sequential implementation of the daisy-chain filter algorithm 
* [VirtualThreadTest](src/main/java/org/mazurov/jn/primes/VirtualThreadTest.java) - a `VirtualThread` implementation using either `SynchronousQueue` or `ArrayBlockingQueue` for channels
* [FJTaskTest](src/main/java/org/mazurov/jn/primes/FJTaskTest.java) - `ForkJoinTask` based implementation  
* [JNTest](src/main/java/org/mazurov/jn/primes/JNTest.java) - implementation using jN tasks and thread pool
