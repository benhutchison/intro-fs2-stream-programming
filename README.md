# Introduction to stream-oriented functional programming in Scala with FS2

## Setup

There is no setup for Part 1! Setup for Part 2 requires a Scala Build Tool (SBT) installed and many libraries download,
which is lengthy but automatic background process. So we suggest you start Part 2 setup now so it's running while you do
Part 1

### Part 1 - Browser-based Learning

It trials a zero setup model where Scala, SBT & libraries do not need be installed to complete the course
content. Instead, we will use embedded code examples and exercises, powered by the [ScalaFiddle](https://scalafiddle.io/)
 compilation service and [Scala.js](https://www.scala-js.org/) for execution in your browser. (Yes, that means you're
 actually learning to program transpiled Javascript!).

Credit and thanks to @ochrons, @olafurpg, the Scalajs team (esp @sjrd, @gzm0) and many contributors to the Scala
ecosystem for making all this possible!

### Part 2 - SBT and IDE-based exercises

#### Install Java JDK 8 or higher and SBT

As per https://www.scala-sbt.org/1.x/docs/Setup.html

#### Launch SBT

`shell> sbt` in the root directory of the git repository.

`sbt> test` to download SBT itself, Scala, and all required libraries, compile the code and run tests. This may take up
to 60 mins if you don't have any existing Scala artifacts & libraries cached locally.

#### Recommended: Install Intellij & Scala Plugin

Free Community Edition at: https://www.jetbrains.com/idea/download/

The Intellij Scala plugin can be installed from within Preferences > Plugins.


## Part 1: Stream-Oriented Functional Programming

- [Warming Up with Option](./docs/WarmUpWithOption.md) introduces `Option`, show why it's better than using `null`,
 and how to use for-expression syntax.
- [Functional Foundations](./docs/FunctionalFoundations.md) covers the foundation concepts of Functional Programming
(FP)
- [Typed Functional Programming](./docs/TypedFunctionalProgramming.md) covers static typing in functional programming
- [Effectful Functional Programming](./docs/EffectfulFP.md) introduces ("side"-)effects, deferred execution, and the
Cats Effect library
- [Stream Oriented Functional Programming](./docs/StreamOrientedFP.md) explains why we need a Stream abstraction
- [Pure Streams](./docs/StreamOrientedFP.md) covers the simpler case of using FS2 `Stream` to represent a collection or
stream of pure data values.
- [Effectful Streams](./docs/EffectfulStreams.md)
- [Concurrent Streams](./docs/ConcurrentStreams.md)

## Part 2: Applied Streams