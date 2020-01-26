---
layout: page
title:  "Streams"
section:  "Streams"
position: 6
---
<script defer src="https://embed.scalafiddle.io/integration.js"></script>

# Stream Oriented Functional Programming

## Are Functions Enough?

In [FunctionalFoundations](./FunctionalFoundations.md) we learned that functional programs that cause effects
are structured as a pure computation that yields an effectful action, which is constructed and then set running at the
end of the program.

This remains true, but for many (perhaps the majority) of "real world" use cases there are some important
additional details. Effectful actions often need to be interspersed between pure computations; the running code needs
to cause effects, then do some computation, then cause more effects, then compute, and so on, back and forth. We want a
a way to declare a stream or recuring sequence of effects and/or computations while remaining purely functional, and then
 the final action of the program starts the "stream machine" running.

### Case Study: Text File Scanning


Lets make this concrete by studying the problem of scanning lines in a text file. Lets build a `grep`like function;
ie find lines where a substring is matched in the text, and print each matching line to stdout.

For starters, hopefully it is apparent that simply reading the whole file into memory, then treating the scan as a pure
operation, wont do. To scale to applied use, we need to ensure that our program needs only a small window of the file in
memory at any time. That means we'll be doing disk IO, interspersed with scanning text, while the program runs.

Our first attempt will simply use `IO` actions:

```scala:mdoc
object FileScan1 {

  def grepIO(filename: String, match: String): IO[Unit] = {
    IO(scala.io.Source(filename)).bracket { src => IO {

      val lines: Iterator[String] = src.getLines
      while (lines.hasNext) {
        val line = lines.next
        if (line.contains(match))
          println(line)
      }

    }} { src =>
      IO(src.close())
    }
  }

  def grep(filename: String, match: String): Unit =
    grepIO(filename: String, match: String).unsafeRunSync
}
```

Whats wrong here? Well, in the middle of our functional programming we have a very imperative looking while loop, thrown into
an `IO` block to make it superficially "FP compliant".

Let's attempt to refactor it to some a little more general. How could we do that? Well we could make the boolean test a
predicate that we pass in from outside, and we could make the action take for each line a parameter also:

```scala:mdoc
object FileScan2 {

  def grepIO(filename: String, match: String, predicate: String => Boolean, action: String => IO[Unit]): IO[Unit] = {
    IO(scala.io.Source(filename)).bracket { src => IO {

      val lines: Iterator[String] = src.getLines
      while (lines.hasNext) {
        val line = lines.next
        if (predicate(match))
          action(line).unsafeRunSync
      }

    }} { src =>
      IO(src.close())
    }
  }
}
```

However, there are a couple of problems with this:

- Notice how we have to call `unsafeRunSync` inside the loop for each matching String. FP Code Smell! Generally if
find yourself needing to run `IO`s inside another `IO`, it's a sign to rethink your design; it should not be necessary.
Instead you should aim to chain up all the IOs so they'll run at the end. The reason we can't in this case is because we've
forced to resort to an imperative approach here, because we are missing the `Stream` abstraction we'll introduce next.

- Despite pulling out a pair of closures to capture the guts of the loop logic, it's not as reuseable as it might first
appear. What if we wanted to substring match over multiple lines, or find the longest run of lines containing
the string? Such traversals require keeping state between lines, which our design isn't capable of.

Lets put the example aside and introduce `Stream`, and at the end of the chapter we'll reattempt the problem using `Stream`.


