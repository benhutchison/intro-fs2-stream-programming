---
layout: page
title:  "Functional Foundations"
section: "Functional Foundations"
position: 3
---
<script defer src="https://embed.scalafiddle.io/integration.js"></script>

# Functional Programming Foundations

Functional Programming (FP) is a programming style emphasising the use of *functions.

## Functions

In FP, functions are defined similarly to mathematics:

- They may have a name, or they may be anonymous, being defined inline at the point where they're used. Anonymous functions
are called "lambda expressions".
```scala:mdoc
object SquareExample {
  //define a named function
  def square(n: Int): Int = n * n

  //using a named function
  assert(Seq(1, 2, 3).map(square) == Seq(1, 4, 9))

  //defining and using an anonymous lambda
  assert(Seq(1, 2, 3).map(x => x * x) == Seq(1, 4, 9))
}
```
- They have zero to N parameters. The number of parameters is termed a functions *arity*.
```scala:mdoc
object ArityExample {

  //arity two
  def remainder(n: Int, dividend: Int) = abs(n % dividend)

  //arity zero
  def piApprox() = 22 / 7.0
}
```

- They compute one result- or return- value. Multiple values can be returned by placing them into a pair or tuple.
 There's also a special singleton ("unique") value called `Unit` that's conceptually
returned by functions that don't compute any result (analogous to void in Java/C##/C ). Slightly confusingly, in Scala
the unit value is written as a bracket pair.
```scala:mdoc
object ReturnExample {

  def pair(n: Int) = (n, n)

  def unit(n: Int) = ()
}
```

**❓Quiz: Unit:** Why might the word `Unit` be chosen for this?

<details><summary>Unit Answer</summary><p>
All functions return a value, so there must exist one single empty value to represent the empty cases. `Unit`, as in "one",
refers to the singlular nature of the empty value.
</p></details>

- Other than their return value, they have no other dependencies or effects on the state of the world. This property is
often called "purity".

**❗Note ** In maths, all functions are "pure". In programming its commonplace to be less strict, and talk
about "impure functions" as function-like operations that interact with the world. So there's a slight context-dependence
in interpreting what people mean by the word "function".


**❓Quiz: Purity:** Which of these are pure?

```scala:mdoc
object EffectsExample {

  def constantInt() = 7

  def doubleInt(n: Int) = n * 2

  def timeInt() = System.currentTimeMillis.toInt

  def readAndInt() = scala.io.StdIn.readInt()

  def writeAndInt() = { println("I'm writing"); 7 }
}
```

<details><summary>Unit Answer</summary><p>
`timeInt`, `readAndInt` and `writeAndInt` are impure, because they depend upon and/or affect the state of the outside world when
computed.
</p></details>


- For given input parameters, they always compute the same result. This is related to the idea of purity, in that returning
a *different* result for the same input implies some interaction with the outside world.

**❓Quiz: Randomness:** Would a function that took no parameters and returned one of two numbers randomly be pure?

<details><summary>Randomness Answer</summary><p>
It's impossible to write such a function! What would the function use as the seed or source of randomness, if it were not
able to depend on any world state, time, computer environment etc?
</p></details>

**❓Quiz: Zero-param Functions:** If a function always computes the same result for the same input, then a zero parameter function
must always yield the same value. What use is such a function?

<details><summary>Zero-param Functions Answer</summary><p>
Computing the answer may be computationally expensive. A zero-arg function can be invoked if and when we want to pay the
cost of computing the result.
</p></details>

### Purity & Referential Transparency

Pure functions imply that a function invocation may be freely substituted for its result value,
without making an observable difference to the world. This property is called *referential transparency*.

The term *pure* is equivalent in meaning to *referentially transparent*.

```scala
object TransparentExample {

  def square(n: Int): Int = n * n

  //these two are equivalent
  val twoSquared_1 = square(2)
  val twoSquared_2 = 4
}
object NonTransparentExample {

  def printAndSquare(n: Int): Int = {
    println(s"Calculating the square of $n")
    n * n
  }

  //these two are not equivalent, as one prints to console
  val twoSquared_1 = printAndSquare(2)
  val twoSquared_2 = 4
}
```

**❓Quiz: Can you think of any conceptual approach to transform or represent impure functions like `printAndSquare` as
pure, referentialy transparent values?

<details><summary>Referential Transparent Transform</summary><p>
Yes! there's a transform that turns *any* impure code into pure code. The idea is to represent the operation as a description
of a program, to be run later, without actually running it. This description is a first-class value that can be manipulated,
 composed and finally, *run* to cause the effects.

This question turns out to be a key one for making functional programming work in "the real world" of software, where effects
and dependencies upon the world are commonplace and a desired aspect of the software. We'll talk lots more about this
transform later when we introduce the `IO` effect
</p></details>

### Non-termination and Total Functions

A more subtle problem is functions that don't terminate but rather run forever.

```scala
object NonTerminating {

  //we can ascribe any return type, even though it's nonsense, since the function will call itself infinitely
  def loop: String = loop
}
```

When the above qualities are true, including non-termination, a function is called *total*. In practice Scala has no way
of determining if a function is total or even pure, but it's a good conceptual goal to aim for when designing functions.

### Exceptions and Purity

Many software runtimes, including the JVM that Scala is based upon, support *exceptions*. This is a special execution mode
used when errors occur that suspends or stops the standard program evaluation behavior.

Exceptions are valuable for handling genuinely unexpected or anomalous program conditions. However, the functional programming
ethos prefer to avoid the use of exceptions for handling or signalling errors which might reasonably have been anticipated,
such as invalid inout data to a program. There are alternate techniques for gracefully handling error flows that don't require
a special execution mode in the runtime, which we'll study in later sections.

So we won't consider a function that throws an exception to be pure, since by not returning anything it breaks the rule
that a function must return a value.

**❓Quiz: Which function is impure and why?

```scala
object MorePurity {

  def head[T](l: List[T]): T = l.head

  def headOption[T](l: List[T]): Option[T] = l.headOption


}
```

<details><summary>MorePurity</summary><p>
`head` is impure. When invoked on an empty list `l`, it will throw an exception rather than returning a value of type `T`.
</p></details>

### An Impure Secret: The Purity Convention

We've looked at the idea that when a function is pure, it has no other effects on the world than its return value, and
it always deterministically computes the same result.

It's commonly accepted that a function that allocates memory on the heap while running is still pure,
but a function that writes bytes to a disk is impure. But we might reasonably ask why writing bytes to DRAM chips is *pure*,
and yet bytes sent to magnetic storage are *impure*?

Similarly, it is always possible that during a heap allocation, the JVM could run out of memory and throw an exception. If
this occurred during an invocation of a pure function, it would have behaved differently to previous invocations.

The point of these examples is to show that the typical definition of *purity is a programming convention*, and not
something to be religious about. It is a sensible convention for *practical* reasons:

- Computer memory is highly reliable, the latency in storage and retrieval are hard to observe, and it is cleaned up when
  a program terminates. Therefore, we treat memory as an ideal store, and choose to ignore the small ways it deviates from
  ideal behavior.
- Disks have a higher latency for access, a higher failure rate, and their contents are long lasting. We consider it
  unwise to treat them as an ideal store, and instead model interactions with disks as external effects.

Typically, when when the underlying machine observably deviates from the assumed abstract machine, we treat that as a
fatal error or anomaly in functional programming, closing the program or restarting the affected operation.

### 🕳️🐇 Currying

We saw earlier that functions can have zero to N parameters. Currying is a transformation that allows us to convert any
function of *multiple* parameters into a function with a *single* parameter. The way a curried function works is that it
accepts the first parameter, then returns a function accepting the remaining parameters.

```scala:mdoc
object Currying {

  val getItem: (Int, List[String]) => Option[String] = (n, l) => l.lift(n)

  val curried: Int => (List[String] => Option[String]) = (n: Int) => getItem(n, _)
}
```

If unfamiliar, the underscore `_` in the example above converts the `function2Params(n, _)` into an anonymous function
of one parameter to "fill the hole", ie with a parameter of type `List[String]`

Currying in simply a mechanical transformation, that lets us feed a function its parameters in stages. We can also used
currying to change the order we provide parameters in.

Exercise: Implement `itemsIn` in terms of `getItem` to re-order parameters.
{% scalafiddle %}
```scala:mdoc
object OutOfOrder {

  val getItem: (Int, List[String]) => Option[String] = (n, l) => l.lift(n)

  val itemsIn: List[String] => (Int => Option[String]) = ???
}
```
{% endscalafiddle %}

<details><summary>Solution</summary><p>
```scala:mdoc
object OutOfOrder {

  val getItem: (Int, List[String]) => Option[String] = (n, l) => l.lift(n)

  val itemsIn: List[String] => (Int => Option[String]) = (l: List[String]) => getItem(_, l)
}
</p></details>

The number and ordering of function parameters is a matter of syntactic convenience, and can
always be changed when desired by mechanical transformations, without changing the meaning of a program.