<script defer src="https://embed.scalafiddle.io/integration.js"></script>

# Warm Up: Why Option is better than null

This short chapter consists of a warmup exercise to introduce Scala syntax, but we are also going to address an important
question: "just why is Scala's `Option` better than using null?" as is common in Java, C# and Ruby.

Along the road, we'll encounter monads and get a sense of their place in functional programming.

## Introducing Option

`Option[A]` is abstraction defined in the Scala standard library that represents a value `A` that may or may not be present.
It models some uncertainty (or non-determinism) the program has about whether this `A` value is defined.

Option is a polymorphic ('many formed') or generic type, in that it accepts another type A, which can be anything, defining
the value it holds. In many other languages it would be written `Option<A>', but Scala uses square brackets `[ ]`  to denote
type parameters (Martin Odersky, the language designer, wanted to reserve angle brackets for XML literals, a decision he now
regrets!).

An Option can either be `Some(value)` or it can be `None`, the empty case. Its defined as (simplified slightly):

```scala:mdoc
sealed trait Option[A]

case class Some[A](a: A) extends Option[A]

case object None extends Option[Nothing]
```

Some notes:
- The `sealed` modifier tells scala that only subclasses defined in the same file are allowed, so they form a closed set.
- When we use the `object` keyword in Scala, we define a global singleton, so there is only one `None` object in the system.
- `Nothing` is specially defined subtype of everything, so somewhat confusingly `None` is a subtype of Option-of-*anything*.

## Using Option

When we *create* an Option, we can handle any empty case by emitting `None`:

```scala:mdoc
def parseIntOpt(s: String): Option[Int] =
  try {Some(Integer.parseInt(s)) } catch {case e: NumberFormatException => None}
```



When we *transform* an existing `Option`, we might initially turn to pattern matching, eg if we need parse an integer argument
that may not exist:

```scala:mdoc
def parseArg(s: Option[String]): Option[Int] = {
  case Some(arg) => parseIntOpt(arg)
  case None => None
}
```

**❓Quiz: Is Option actually better than `null`?**  The above code is structurally
similar to the `if (arg != null) ... else ...` patterns used to handle `null`s in C# or Java, in that there's branch to
handle the non-empty case and another to handle the empty one. Are the benefits of Option over null just superficial?


<details><summary>Options Benefits</summary><p>
The first tier benefit is explictness of intent: by using Options consistently in Scala, the type system forces users of
a value to consider how they will handle the empty case.

There's also a more subtle second tier benefit when Options are composed together; we'll study it in the next section
</p></details>

## Chaining Optional Computations

It turns out that we can "factor-out" the code paths that handles the empty case into the Option library, and mostly just
talk about  the non-empty case in our application code.

Lets have a look at how that works:

```scala:mdoc
//cant represent square root of a negative number as a Double value
def squareRoot(n: Double): Option[Double] = if (n > 0) Some(math.sqrt(n)) else None

object TwiceSquareRoot {

  def main(args: Array[String]) = {
    //note: unintuitively named method `lift` safely accesses an arrays contents by index, returning an Option.
    val arg: Option[String] = args.lift(0)
    val optN: Option[Double] = parseArg(arg)
    val optRoot: Option[Double] = optN.flatMap(n => squareRoot(n))
    val optTwiceRoot: Option[Double] = optRoot.map(_ * 2)

    //another way to handle an option is to provide a default value if it's empty, with `getOrElse`
    println(optTwiceRoot.getOrElse(s"Invalid input $arg"))
  }
}
```

Where is the code to handle an empty number in `optN`? It lies within the `flatMap` operation, which transforms an optional
value using a function that itself returns an optional value (`squareRoot` here), and "flattens" the two options in the
result down to one. Whether it was because no argument was provided, or because the provide argument wasn't a number,
we'll end up with an `Option[Double]` in the end.

Similarly, the `map` method on Option also runs only when it's defined, letting us double the optional number without
worrying about the `None` case.

### Exercise

Use Option chaining to implement `estimateDelivery`:

<div data-scalafiddle>
```scala mdoc:reset
object OptionExercise {
  case class UserProfile(postcode: Option[String])

  def loggedInUser: Option[UserProfile] = Some(UserProfile(Some("3000")))

  def deliveryEstimate(postcode: String): Option[BigDecimal] =
    Map("3000" -> BigDecimal("15.00"), "2000" ->  BigDecimal("20.00")).get(postcode)

  val unknownPostcodeEstimate = 25.0

  //Estimate delivery for the current logged in user's postcode, `orElse` use the `unknownPostcodeEstimate`
  //use flatMap and map
  def estimateDelivery: BigDecimal = ???
}
```
</div>

## For Expressions

This pattern of chaining a pipeline of transformations over optional values together is common in applied scala code. In
fact, there is syntactic sugar available in Scala by means of "for-yield expressions" that let us rewrite the above as:

```scala:mdoc
object ForTwiceSquareRoot {

  def main(args: Array[String]) = {
    val optTwiceRoot = for {
      arg <- args.lift(0)
      n <- parseArg(arg)
      root <- squareRoot(n)
    } yield (root * 2)

    //another way to handle an option is to provide a default value if it's empty, with `getOrElse`
    println(optTwiceRoot.getOrElse(s"Invalid input $arg"))
  }
}
```

The intuition behind the for expression is that the expressions on the righthand side (eg ` args.lift(0)`) are all `Option`s
of some form. The arrow `<-` "pulls" the payload out of the option, if its non-empty, and assigns it to the name on the left
(eg `arg`), where it can be referred by any subsequent line in the expression.

As soon as any expression on the right is empty (`None`), the whole for-expression "short-circuits" and yields `None` as
the overall value. But otherwise, whatever expression is in the `yield` block is returned, wrapped inside an `Option`.

### Exercise

Same business logic as the previous exercise, but write it using a for-expression

<div data-scalafiddle>
```scala mdoc:reset
object OptionExercise2 {
  case class UserProfile(postcode: Option[String])

  def loggedInUser: Option[UserProfile] = Some(UserProfile(Some("3000")))

  def deliveryEstimate(postcode: String): Option[BigDecimal] =
   Map("3000" -> BigDecimal("15.00"), "2000" ->  BigDecimal("20.00")).get(postcode)

  val unknownPostcodeEstimate = 25.0

  def estimateDelivery: BigDecimal = ???
}
```
</div>

## Monads

In the Swift language, option chaining is supported by a [dedicated language feature](https://docs.swift.org/swift-book/LanguageGuide/OptionalChaining.html).
We've seen that in Scala, option chaining is possible either by simply using the `flatMap` operation, or via the
for-expression syntax, which is often more elegant.

However, for-expressions are *not* specific to `Option`; rather they work with any data type that supports `map` and
`flatMap` operations with appropriate behavior, and `Option` is just one example. Other datatypes that can be used with
for expressions include `List`, `Either`, `Try`, `Future`, `IO` from the Cats library, and even plain functions and tuples
(which support the so-called Reader and Writer monads respectively).

What all these diverse types share is support for a powerful, extremely general and IMHO beautiful programming abstraction
known as monads.

I'm going to offer the following informal description: a monad is an operator defined for a higher-kinded ("container-like") datatypes
that allows sequential chains of operations over the data to be constructed. Typically the datatype features some kind of
effect (eg for `Option` the effect is that an empty option stops the computation) and the monad knows how to flatten, or
combine, multiple nestings of the effect.

That's a pretty abstract concept and I realize it's not readily absorbed. It was the same for me. In large part because
of their abstract generality, monads have become infamous for being hard to understand.

In my experience, the best strategy
is just to learn how *particular* example monads are used, like `Option`, `List`, `Either` or `IO`. Our brains are excellent at
factoring out common patterns from examples, almost effortlessly, and in getting familiar with some example monads, you'll
come to perceive the common aspects that connect them.

What *won't* work in trying to master FP while avoiding monads. Although they weren't recognized until the 1990s, we now
know they form part of the mathematical substructure underlying computation, and not some fad that will pass next decade.




