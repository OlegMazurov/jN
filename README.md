# jN - a Java framework for parallel programming in shared memory and distributed systems

Project jN ("Jane") is a Java framework for parallel programming. It is based on dynamic creation and asynchronous execution of tasks with dependencies and data flow.
The framework provides a unified concurrency model that utilizes multithreading and inter process communication without exposing them to the programmer.

## Features 
* mostly lock-free implementation of a communicating sequential processes (CSP) model
* asynchronous I/O and computation without async/await/yield 
* scales automatically with the number of CPUs/nodes
* scalability is limited by parallel algorithms implemented by applications, computational speed and communication cost of CPUs/nodes, scheduler overhead
