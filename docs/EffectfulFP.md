## Effects

Lets look now at how we build functional programs that do have effects on the outside world.

The idea is that we have a functional, pure core to our software that describes, or computes, what effects the software should
have, given inputs from the outside world. Our pure computation will yield an *effectful action*, which can be run to
process inputs, do computations, and cause (side-) effects: real observable changes in the world such as network or disk
IO.

Then, right at last point in our functional program, sometimes called the "end of the world", we set this action
running.

So functional programming isn't so much about avoiding side-effects as it is about separating the pure and effectful
aspects of our program into two distinct phases. A functional program has been likened to a fruit, with a pure interior
surrounded by a thin effectful rind or skin.

### Exercise: Refactoring Effects to the Edge

In this exercise, I encourage you to try applying the ideas above by refactoring this program to move all the effects
to the `main` method, leaving the remainder of the program pure and free of mutable state.

[Edit on Scala Fiddle (advise open in new tab)](https://scalafiddle.io/sf/xdwyVXW/0)

```scala mdoc:reset
import collection.mutable.ArrayBuffer

case class Website(ads: ArrayBuffer[Ad] = ArrayBuffer.empty)

object Website {

  def buyAd(text: String)(site: Website, cc: CreditCard) = {
    val ad = new Ad(text)
    site.ads += ad
    cc.charge(ad.price)
  }
}
//CreditCard contains a mutable balance, so the charge operation causes side-effects
case class CreditCard(id: Long, var balanceOwing: Int) {

  def charge(amount: Int) = balanceOwing += amount
}
case class Ad(text: String) {

  def price = 10 + math.min(text.length, 40)
}

object ImperativeMain {

  def main() = {
    val site = Website()
    val cc = CreditCard(1111222233334444L, 0)
    Website.buyAd("Functional Programming is cool!")(site, cc)
    printStatus(site, cc)
  }

  def printStatus(site: Website, cc: CreditCard) =
    println(s"After purchase, I have ${cc.balanceOwing} owing on my card. There are ${site.ads.size} on site.")
}
```

</div>


#### Sample Solution

<details>

Note: the `tupled` operator below mechnically transforms a functions parameter list from accepting N parameters to accepting
one parameter, an N-tuple of the same types as the original.

```scala mdoc:reset
case class Website(ads: List[Ad] = List.empty[Ad])

object Website {

   def buyAd(text: String, site: Website, cc: CreditCard): (Website, CreditCard) = {
    val ad = new Ad(text)
    (site.copy(ads = site.ads :+ ad), cc.charge(ad.price))
  }
}

case class CreditCard(id: Long, val balanceOwing: Int) {

  def charge(amount: Int) = copy(balanceOwing = this.balanceOwing + amount)
}
case class Ad(text: String) {

  def price = 10 + math.min(text.length, 40)
}

object FunctionalMain {

  //we still have side-effects and mutable state, but they are confined to the top-level, ie `main`
  def main() = {
    var appState = (Website(), CreditCard(1111222233334444L, 0))
    appState = Website.buyAd("Functional Programming is cool!", appState._1, appState._2)
    println(status(appState._1, appState._2))
  }

  def status(site: Website, cc: CreditCard) =
      s"After purchase, I have ${cc.balanceOwing} owing on my card. There are ${site.ads.size} on site."
}
```

</details>

## Cats IO: A building block effect

`IO` is effect abstraction provided by the [`Cats Effect`](https://typelevel.org/cats-effect/) that we'll use heavily in
the remainder of this tutorial.

We use `IO` by wrapping it around code that causes side-effects, such as read- or write- io, or accessing the system clock,
or mutating an externally visible variable. `IO` *suspends* the wrapped code, meaning that it get's stored up as a runnable
value but doesn't run when created. Whatever value the wrapped expression returns, call it `A`, becomes the value of the
IO, ie `IO[A]`. So for example, an `IO` action that effectfully reads bytes off the network might return `IO[Array[Byte]]`,
being the data read when it is finally run.

```scala mdoc
import cats.effect.IO
import cats.implicits._

object RunIO {
    val program = IO(println("Hello World"))

    program.unsafeRunSync
}
```

`IO` has a monad defined, so we can chain together `IO`s, feeding the result of one into the input of the next. It also
provides operations for launching two `IO` actions concurrently and waiting for one or all of them to complete. The intent
is that we build up a description of all the effectful actions in our program as a graph of `IO` values, which is a pure
computation, and then set run running at the end with an operation like [`unsafeRunSync`](https://typelevel.org/cats-effect/datatypes/io.html#unsaferunsync),
which starts them running.

//TODO when the sbt mdoc task is run, it blocks on the readLine. Fix this
```scala mdoc
object SequencingIO {
    val prompt = IO(println("Enter your name"))

    val readName: IO[String] = IO(scala.io.StdIn.readLine)

    val greeting = (name: String) => IO(println(s"Hello $name"))

    prompt.>>(readName).>>=(greeting).unsafeRunSync
}
```

The `unsafe` prefix is not intended to imply the method shouldn't be called, but rather to alert programmers that when
called they are leaving the pure world and actually causing effects.

The above example uses the [`>>`](https://github.com/typelevel/cats/blob/e1a7cfcddce0/core/src/main/scala/cats/syntax/flatMap.scala#L33)
operator, which is used to sequence an 'IO' action that doesn't receive any parameters
from the preceding ones (`readName` here). `>>(action)` is an alias for `flatMap(_ => action)`, and requires the import
of Cats library "syntax" (similar concept to "extension methods" in other languages) enabled by
[`import cats.implicits._`](https://typelevel.org/cats/typeclasses/imports.html).
`>>=` is an operator alias of `flatMap` to sequence an action that *does* require a parameter passed from the previous action
(the greeting string in the above example).

### Exercise: IO Sequencing

See it, Do it. The example below and linked ScalaFiddle contain three program parts which you need to compose.
- An action that reads the current time as a timestamp.
- An action that nice formats a timestamp into a string.
- An action that prints a time string

There are two syntactic styles you can use. Attempt the exercise in both styles to develop your intuition as to their
underlying equivalence despite superficial differences.
- `map` and `flatMap` invocations, the desugared representation
- A for-yield expression


#### Sample Solutions

```scala mdoc:reset
import cats.effect._
import cats.implicits._

object IOExercise extends App {
  val timestamp = IO(System.currentTimeMillis)

  val formatTime = (t: Long) => IO(new java.text.SimpleDateFormat("yyyy-MM-dd HH").format(new java.util.Date(t)))

  val printTimeMsg = (s: String) => IO(s"The clock in the executing computer says time is $s")

  val program: IO[Unit] = ???

  program.unsafeRunSync

}
```

<details>

```scala mdoc:reset
import cats.effect._
import cats.implicits._

object IOExercise extends App {
  val timestamp = IO(System.currentTimeMillis)

  val formatTime = (t: Long) => new java.text.SimpleDateFormat("yyyy-MM-dd HH").format(new java.util.Date(t))

  val printTimeMsg = (s: String) => IO(println(s"The clock in the executing computer says time is $s"))

  val program: IO[Unit] = timestamp.map(formatTime).flatMap(printTimeMsg)

  program.unsafeRunSync

}
```

With for syntax:


```scala mdoc:reset
import cats.effect._
import cats.implicits._

object IOExercise extends App {
  val timestamp = IO(System.currentTimeMillis)

  val formatTime = (t: Long) => new java.text.SimpleDateFormat("yyyy-MM-dd HH").format(new java.util.Date(t))

  val printTimeMsg = (s: String) => IO(println(s"The clock in the executing computer says time is $s"))

  val program: IO[Unit] = for {
    t <- timestamp
    s = formatTime(t)
    _ <- printTimeMsg(s)
  } yield (())

  program.unsafeRunSync

}
```



</details>

`IO` has some built in error handling, because effectful code can of course throw runtime exceptions. So an `IO[A]` should
yield an `A` but may alternately yield an exception when run.

```scala mdoc
object ErrorsAndIO {
    val errorProgram = IO(throw new RuntimeException("Boom!"))

    //swallows the exception by translating to the Unit value
    errorProgram.handleError(ex => ()).unsafeRunSync

    //throws the exception because it's unhandled
    errorProgram.unsafeRunSync
}
```
