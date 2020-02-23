---
layout: page
title:  "Effects"
section:  "Effects"
position: 5
---
<script defer src="https://embed.scalafiddle.io/integration.js"></script>

# Effects

How do we build functional programs that have effects on the outside world?

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

{% scalafiddle %}
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
{% endscalafiddle %}


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
the remainder of this tutorial. An [`IO[T]`](https://typelevel.org/cats-effect/api/cats/effect/IO.html) ("IO of T") is an
unevaluated task or program fragment that, when run, will return a value of type `T` *and* have some effects upon the world.

We use `IO` by wrapping it around code that causes interacts with the world, such as read- or write- io, accessing the
system clock, or mutating an externally visible variable. `IO` *suspends* the wrapped code, meaning that it get's stored
 up as a runnable
value but doesn't run when created. Whatever value the wrapped expression returns becomes the value of the
IO, ie the `T` in `IO[T]`. So for example, an `IO` action that effectfully reads bytes off the network might return
`IO[Array[Byte]]`, being the data read when it is finally run.

The intent is that we:
- Build a description of all the effectful actions in our program as a graph of `IO` values, which is a pure
computation.
- Run the whole program *once* at the end with an operation like
[`unsafeRunSync`](https://typelevel.org/cats-effect/datatypes/io.html#unsaferunsync),
which runs it, causes the effects, and waits for the final result. Operations which evaluate effects have an `unsafe`
prefix in Cats Effects; it doesn't imply the method shouldn't be used, but rather to alert programmers that when called
they are
leaving the pure world and actually causing effects.


```scala mdoc:reset
import cats._
import cats.data._
import cats.implicits._

(Validated.valid[String, Int](1), Validated.valid[String, Int](1)).tupled
```

```scala mdoc
import cats.effect.IO
import cats.implicits._

object RunIO {
    val program = IO(println("Hello World"))

    program.unsafeRunSync
}
```

## IO spells Async

So we can think of `IO[A]` as a "task" that computes an `A` when run. It *can* be synchronous, doing the computation 
immediately in the current thread, but it can also represent an asynchronous process that runs in another thread, or 
on another machine, and may take a long time. Whenever it completes, the result will be available. So IO is similar 
to promises, futures or async methods in other languages.

In the example below, we'll run a task that registers with the system timer service, and completes when the timer 
goes off. We won't use `unsafeRunSync` here since that will block the thread waiting for the task, but rather its 
cousin `unsafeRunAsync` which accepts a callback to run when complete, then schedules the IO to run and returns 
immediately.  

```scala mdoc:js
import cats.effect.IO
import cats.implicits._
import scala.scalajs.js.timers

object RunIO {
    IO.async(callback => timers.setTimeout(1000.0)(callback(Either.right("Timer has fired!"))))

    program.unsafeRunAsync(result => println(result))
}
```

## Sequencing IO values

Next we'll look at how to sequence one IO action after another. That is, when the first has run successfully the second
will run, with access to any value the first expression has returned.

```scala mdoc:scastie

object SequencingIO {
    val dieRoll: IO[Int] = IO(util.Random.nextInt(6) + 1)

    val monopolyMove = for {
      d1 <- dieRoll
      d2 <- dieRoll
      extraTurnMsg = if (d1 == d2) "You rolled doubles. Take another turn." else ""
      _ <- IO(println(s"Advance ${d1 + d2} spaces. $extraTurnMsg"))
    } yield (())
}
```

In the above for-expression, the terms on the right of `<-` arrows are all in `IO`. We "pull out" two die rolls and
then print some instructions. Note in the middle the `extraTurnMsg` isn't an IO expression, since it's pure; we can use
an assignment statement in a for loop for this purpose. The yield statement yields the unit value `()`.

Note the similarity to the earlier [`Option` example](./WarmUpWithOption.md#for-expressions), where we sequenced
"optional" computations in a for expression. While `Option` and `IO` are unrelated types with distinct meanings, they
both support a `Monad` operator which enables sequential composition of programs into bigger programs. This is the
essence of what monads are about.

### Exercise: IO Sequencing

See it, Do it. The example below and linked ScalaFiddle contain three program parts which you need to compose.
- An effectful action that reads the current time as a timestamp.
- A pure action that nicely formats a timestamp into a string.
- An effectful action that prints a time string

There are two syntactic styles you can use. Attempt the exercise in both styles to develop your intuition as to their
underlying equivalence despite superficial differences.
- `map` and `flatMap` invocations, the desugared representation
- A for-yield expression



{% scalafiddle %}
```scala mdoc:compile-only
import cats.effect._
import cats.implicits._

object IOExercise {
  val MillsInYearAverage = 1000.0 * 60 * 60 * 24 * 365.25

  val timestamp = IO(System.currentTimeMillis)

  val years = (t: Long) => t / MillsInYearAverage

  val printTimeMsg = (years: Double) =>
    IO(println(s"Based on the computer clock, I estimated $years have elapsed since 1970"))

  val program: IO[Unit] = ???
}
IOExercise.program.unsafeRunSync
```
{% endscalafiddle %}

Note the example uses the rather crude years-since-1970 because there's no built-in data formatter in
Scalajs/Scalafiddle.

### Solutions
<details>

```scala mdoc:compile-only
import cats.effect._
import cats.implicits._

object IOExercise {
  val MillsInYearAverage = 1000.0 * 60 * 60 * 24 * 365.25

  val timestamp = IO(System.currentTimeMillis)

  val years = (t: Long) => t / MillsInYearAverage

  val printTimeMsg = (years: Double) =>
    IO(println(s"Based on the computer clock, I estimated $years have elapsed since 1970"))

  val program: IO[Unit] = timestamp.map(years).flatMap(printTimeMsg)
}
IOExercise.program.unsafeRunSync
```

With for syntax:

```scala mdoc:compile-only
import cats.effect._
import cats.implicits._

object IOExercise {
  val MillsInYearAverage = 1000.0 * 60 * 60 * 24 * 365.25

  val timestamp = IO(System.currentTimeMillis)

  val years = (t: Long) => t / MillsInYearAverage

  val printTimeMsg = (years: Double) =>
    IO(println(s"Based on the computer clock, I estimated $years have elapsed since 1970"))

  val program: IO[Unit] = for {
    t <- timestamp
    y = years(t)
    _ <- printTimeMsg(y)
  } yield (())
}
IOExercise.program.unsafeRunSync
```
</details>

## Applicative Composition

When people started composing effectful functional code in the 1990s, they initially focused on sequential, or
*monadic*, composition. That is, building big program out of small one by running one program piece, then the other, as
we saw in the section above.

But with the rise of concurrent programming in the decade-2000-to-2010 ("it whom should not be named"), it became
increasingly obvious there was another way to compose two programs together: by running them in parallel.

The *applicative functor* is an operator that let's use combine several programs together into a larger program that,
when run, will run all the parts and yield a tuple of all the values computed by them. Although not all applicative
operators do so, I recommend thinking of the applicative as running the parts concurrently as the best "default mental
model".

If the composed programs have effects, concurrent execution means that we shouldn't care which order the effects
happen in.

### Example: Validation

Validation is a classic example used for side-by-side composition, because when we validate multiple input fields, we
typically want to see any/all validation problems and not just the first one.

In this example, we validate a `String` against email length & format checks, yielding an `Email` if successful or else
a `String` detailing one or two problems with the input.

```scala mdoc
import cats._
import cats.data._
import cats.implicits._

case class Email private (emailString: String)

object Email {
  val MaxEmail = 254
  val SimpleEmailRegex = """[^@\s]+@[^@\s]+\.[^@\s]+""".r

  def validate(s: String): Validated[String, Email] = {
    val v: Validated[String, (String, String)] = (
      Validated.cond(s.length < MaxEmail, s, s"Email '$s' exceeds $MaxEmail characters. "),
      Validated.cond(SimpleEmailRegex.pattern.matcher(s).matches, s, s"Email '$s' doesn't confirm to regex $SimpleEmailRegex. ")
    ).tupled
    v.map { case (email1, email2) => Email(email1)}
  }
}


```

### 🕳️🐇 Applicative vs Semigroupal

There's a slight misalignment between "common language" used by functional programmers and the typeclass hierarchy used
in Cats that deserves note. Broadly, people associate the Applicative typeclass with side-by-side or parallel
composition, and the Monad typeclass with sequential composition.

However, the Cats library this course teaches is a "3rd generation" functional library, being inspired by earlier
designs in Scalaz and the Haskell standard libraries. Modern libraries have prised apart the concepts embodied by both
Applicatives and Monads into more nuanced, fine-grained hierarchy; in the modern conception, it is actually the
typeclass [`Semigroupal`]() that enables the `tupled` operation shown above. `Semigroupal` is a generalisation of
Applicative that, as Cats' docs aptly describe it, "captures the idea of composing independent effectful values".

## Error Handling

### Exceptions in IO

`IO` has some built in error handling, because effectful code can of course throw runtime exceptions. So an `IO[A]`
should yield an `A` but may alternately yield an exception when run.

```scala mdoc
object ErrorsAndIO {
    val errorProgram = IO(throw new RuntimeException("Boom!"))

    //swallows the exception by translating to the Unit value
    errorProgram.handleError(ex => ()).unsafeRunSync

    //throws the exception because it's unhandled
    errorProgram.unsafeRunSync
}
```

### Functional Error Handling

The appropriate usage of exceptions is for anomalous, unexpected events that violate the assumptions your program is
constructed on (eg running out of memory). However, exceptions are not the best way to model error conditions that might
reasonably be anticipated, such as a user providing invalid input. The problem is that they are excessively powerful,
 in that they switch the runtime into a different execution mode that breaks the semantics of functions, the building 
 blocks of functional programs.
 
Further, they are unnecessary: Error handling is just some additional non-default branches
 or paths through your program logic, and is as amenable to FP techniques as any other aspect of program design.
 
The [`Either[E, A]`](https://www.scala-lang.org/api/current/scala/util/Either.html) type is a central tool in 
functional error handling. It represents a value `A` whose computation may alternately have failed, yielding an error
value `E`. It is very similar to `Option[A]`, but attaches a value on the empty or failed side to describe *why* the
computation failed. Instead of throwing exceptions, we should acknowledge the possibility of failure in our function's
 type signature by returning an `Either` where required.
 
In an [earlier chapter](./WarmUpWithOption.md) we looked at parsing a string into an integer; lets model that using 
`Either` instead of `Option`: 

```scala:mdoc
def parseIntOpt(s: String): Either[String, Int] =
  try { Either.right(Integer.parseInt(s)) } 
    catch {case e: NumberFormatException =>  Either.left(s"Cannot parse as Int: 
  '$s'")}
```

### Composing Either

"How we can compose small programs that can fail into large programs that can fail?". The same tools we used 
to compose `IO` work for Either: we can combine multiple fallible programs into a larger fallible program, 
sequentially using monadic composition, side-by-side using applicative composition, or a combination of both.

#### Exercise: Composing Either

{% scalafiddle %}
```scala mdoc:compile-only
import cats.effect._
import cats.implicits._

case class Token(value: String)

case class UserConfig(preferredEmail: String)

object ComposeEitherExercise {
  //we model errors as Strings in all 3 operations below

  //models obtaining an auth token required to send an email
  //can fail if the auth service refuses to token grant
  def fetchAuthToken: Either[String, Token] = Either.right(Token("kiuygvcdwetyhbnko"))
  
  //models loading the configuration of the current user
  //can fail if data not found
  def loadConfig: Either[String, UserConfig] = Either.right(UserConfig("user@email.com"))
  
  //both the users email and the token are needed to actually send the email
  def sendWelcomeMsg(email: String, token: Token): Either[String, Unit] = Either.right(())
}

//fetch a token and load config in parallel, then send the welcome message
val sendEmail: Either[String] = ???

```
{% endscalafiddle %}

##### Solution
<details>

```scala mdoc:compile-only
import cats.effect._
import cats.implicits._

case class Token(value: String)

case class UserConfig(preferredEmail: String)

object ComposeEitherExercise {
  //we model errors as Strings in all 3 operations below

  //models obtaining an auth token required to send an email
  //can fail if the auth service refuses to token grant
  def fetchAuthToken: Either[String, Token] = Either.right(Token("kiuygvcdwetyhbnko"))
  
  //models loading the configuration of the current user
  //can fail if data not found
  def loadConfig: Either[String, UserConfig] = Either.right(UserConfig("user@email.com"))
  
  //both the users email and the token are needed to actually send the email
  def sendWelcomeMsg(email: String, token: Token): Either[String, Unit] = Either.right(())
}


//fetch a token and load config in parallel, then send the welcome message
val sendEmail: Either[String, Unit] = (loadConfig, fetchAuthToken).parTupled.flatMap {
  case (config, token) => sendWelcomeMsg(config.email, token)
}
```
</details>

## Effect Composition

The third and most subtle mode of composition is combining effects together to create "super-effects" that have the 
effects of all ingredient effects.. 

Let's look at a more realistic variant of the previous example to understand why this is necessary. The operations to
 fetch a token and to load the config would probably be asynchronous, which we can represent by wrapping the payload 
 in an `IO`. So return for example a nested structure, `IO[[Either[String, Token]]`. 
 
 The problem is, the behavior defined by the Semigroupal and Monad typeclass operators is convenient for both `IO` 
 and `Either`. But if we naively wrap `IO` around `Either`, we get the efects of `IO` but the error checking behavior
  of `Either` on the inside has to be implemented by hand, which can result in significant boilerplate code as we see
   in `sendEmail`.
  
```scala mdoc:compile-only
case class UserConfig(preferredEmail: String)

object ComposeEitherExercise {
  //we model errors as Strings in all 3 operations below

  //models obtaining an auth token required to send an email
  //can fail if the auth service refuses to token grant
  def fetchAuthToken: IO[[Either[String, Token]] = Either.right(Token("kiuygvcdwetyhbnko"))
  
  //models loading the configuration of the current user
  //can fail if data not found
  def loadConfig: IO[Either[String, UserConfig]] = Either.right(UserConfig("user@email.com"))
  
  //both the users email and the token are needed to actually send the email
  def sendWelcomeMsg(email: String, token: Token): IO[Either[String, Unit]] = Either.right(())
}

//fetch a token and load config in parallel, then send the welcome message
val sendEmail: IO[Either[String, Unit]] = (loadConfig, fetchAuthToken).parTupled.flatMap {
  case (Right(config), Right(token)) => Right(sendWelcomeMsg(config.email, token))
  case (Left(errMsg), Right(token)) => Left(errMsg)
  case (Right(config), Left(errMsg)) => Left(errMsg)
  case (Left(errMsg1), Left(errMsg2)) => Left(s"$errMsg1, $errMsg2")
}
``` 

Another approach might be to nest a `tupled` operator for `Either` inside `sendEmail`:

```scala mdoc:compile-only
//fetch a token and load config in parallel, then send the welcome message
val sendEmail2: IO[Either[String, Unit]] = (loadConfig, fetchAuthToken).parTupled.flatMap(tuple => {
  tuple.tupled.map {
    case (config, token) => sendWelcomeMsg(config.email, token)
  }
})
``` 

However, that's still boilerplate and also confusing because we have two `tupled` operators, one over `IO` and, nested
 within, another over `Either`.
 
What we'd ideally like is a combined effect "IOErr" that represents a deferred, possibly asynchronous computation 
that may also fail with an error message (`IO`+`Either`). 
 
### Monad Transformers for Effect Composition

Monad Transformers are a technique for stacking multiple effects together and being able to support effectful 
operators like `map`, `tupled` and `flatMap` over the stack.

Lets see the previous example using a stacked effect `IOErr`. Note:

- The "monad transformer" type `EitherT` which transforms another monad type (here `IO`) to yield a stack including
both effects. We use a type alias to refer to the stack by the simple name `IOErr`
- A new syntax `<expr>.pure[IOErr]`. The `pure` operation wraps (or "lifts" in FP jargon) a payload value into the 
specfied effect type.
- The "payoff" for this form, in `sendEmail`: we can now combine our effectful operations `loadConfig` and 
`fetchAuthToken` in just the same way as when they were merely a single `Either` effect in the very first example above.
  
```scala mdoc:compile-only
// a deferred, possibly asynchronous computation that may fail with an error message
type IOErr[T] = EitherT[String, IO, T]

case class UserConfig(preferredEmail: String)

object ComposeEitherExercise {
  //we model errors as Strings in all 3 operations below

  //models obtaining an auth token required to send an email
  //can fail if the auth service refuses to token grant
  def fetchAuthToken: IOErr[Token] = Token("kiuygvcdwetyhbnko").pure[IOErr]
  
  //models loading the configuration of the current user
  //can fail if data not found
  def loadConfig: IO[Either[String, UserConfig]] = UserConfig("user@email.com").pure[IOErr]
  
  //both the users email and the token are needed to actually send the email
  def sendWelcomeMsg(email: String, token: Token): IOErr[Unit] = ().pure[IOErr]
}

//fetch a token and load config in parallel, then send the welcome message
val sendEmail: IOErr[Unit] = (loadConfig, fetchAuthToken).parTupled.flatMap {
  case (config, token) => sendWelcomeMsg(config.email, token)
}
```
 
### 🕳️🐇 Problems with Monad Transformers

This section explains why monad transfoermers can be problematic. You can skip to text section if you simply want to 
learn the recommended alternative, the so-called "MTL style".

Effect composition is a crucial to apply effectful functional programming to real-world-scale 
problems, but the monad transformer technique illustrated above is problematic at scale.

To understand why, consider a variant where each part of the overall computation has *different* effects. 

```scala mdoc:compile-only
// a deferred, possibly asynchronous computation that may fail with an error message
type IOErr[T] = EitherT[String, IO, T]

case class UserConfig(preferredEmail: String)

object ComposeEitherExercise {
  //we model errors as Strings in all 3 operations below

  //models obtaining an auth token required to send an email
  //can fail if the auth service refuses to token grant
  def fetchAuthToken: IO[Token] = Token("kiuygvcdwetyhbnko").pure[IO]
  
  //models loading the configuration of the current user
  //can fail if data not found
  def loadConfig: Either[String, UserConfig] = UserConfig("user@email.com").pure[IOErr]
  
  //both the users email and the token are needed to actually send the email
  def sendWelcomeMsg(email: String, token: Token): IOErr[Unit] = ().pure[IOErr]
}

//fetch a token and load config in parallel, then send the welcome message
val sendEmail: IOErr[Unit] = (loadConfig, fetchAuthToken).parTupled.flatMap {
  case (config, token) => sendWelcomeMsg(config.email, token)
}
```

## Summary: 3 Ways to Compose