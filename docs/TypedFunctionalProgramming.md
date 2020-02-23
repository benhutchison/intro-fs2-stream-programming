---
layout: page
section:  "Static Typing"
title:  "Static Typing"
position: 4
---
<script defer src="https://embed.scalafiddle.io/integration.js"></script>

## Statically Typed Functional Programming

This workshop teaches *statically typed* functional programming. Static typing classifies all the *terms* (ie data
and functions) in the program using a *type*. A type defines and constrains the *set* of possible values that the term can
take on.

    Note that a term can *inhabit* (satisfy) many types at once, just as an element can be a member of many sets.

So in typed FP, we additionally must specify the type of each function parameter, and the type of the function result.

### Types Are Sets

A type describes a set of values. By annotating a functions inputs and outputs with a type, we are describing the set of
values that it consumes and emits. In statically typed functional programming, we should strive to use accurate types:

- Our functions are defined (will run without errors) for any element of the input set
- The type of the return value includes and "tightly bounds" the possible outputs of our functions. (The definition of tightly
  bounding is a little ambiguous and judgement is required).

Consider for example a function `parseInt` that tries to parse a `String` into an . We can view that as
a mapping from every element in the  set of Strings to the union of the set of Integers and the value `None`,
representing a failed parse.

In a language with subtyping like Scala, we could also define `parseInt` to return the `Any`
supertype; while that typechecks, it fails the "tight binding" criterion, since very few of `Any`'s total inhabitants lie
within `Option[Integer]`.

<img src="images/parseIntVennDiagram.png"/>

### Untyped Programming

Many popular languages, such as Ruby, Python and Javascript do not use static types. Viewed from a typed perspective, functions
in these languages are completely unconstrained, and map any input value to any possible output value.

<img src="images/parseIntUntypedVennDiagram.png"/>

In practice, functions in these languages operate over a much small set of inputs and outputs, and it is up to the programmer
to keep track of the types of data. So the type system is implemented in the programmer's head rather than in the rules
of the language.

While this allows for an informal notation that can be more quickly learned by new programmers, as program size and
complexity scale up the burden of manually tracking data types grows ever greater.

### Types And Exceptions

One common way that functions fail to honour their return values is to throw exceptions rather than returning. This is a
no-no in FP, except for truly anomalous & fatal errors, especially as its readily handled by acknowledging the error
possibility is the return type.

```scala
object UseEitherNotExceptions {

  def squareRoot_Incorrect(d: Double): Double =
    if (d < 0) throw new Exception(s"$d is negative") else math.sqrt(d)

  def squareRoot_Correct(d: Double): Either[String, Double] =
    if (d < 0) Left(s"$d is negative") else Right(math.sqrt(d))
}
```

The key objection to using exceptions in FP is that it requires jumping into a special execution mode with different semantics,
and it turns out to be just as easily handled within the standard FP model using `Either` and related concepts.

### Polymorphic Types & Type Parameters

In the chapter [WarmUpWithOption](./WarmUpWithOption.md) we looked at `Option` but we didn't focus on 
its type-polymorphic nature or the mechanics of the type parameter `[A]`. 

Review the definition of `Option`: 

```scala:mdoc
sealed trait Option[A]

case class Some[A](a: A) extends Option[A]

case object None extends Option[Nothing]
```

This is a *polymorphic* data-type; "many shaped" (also called "generic" in other languages). It optionally holds a 
value of type `A` but the type `A` can be  differ at each instantiation of the Option. That is we might have an 
`Option[Int]` and an `Option[String]` in the same  program or even expression. They both have the optional behavior 
of option, but the payload type differs. Polymorphic data types are useful because they let us write reuseable code 
that works for many different types of data.

As well as data types, methods can be polymorphic. Here's an example of how methods can do useful work even when the 
specific data they are handling isn't known:

```scala:mdoc
def orElse[T](planA: Option[T], planB: Option[T]): Option[T] = planA match {
  case Some(_) => planA
  case None => planB
}
```

The use of single capital letters for type parameters is merely a convention. We can have any number of type 
parameters. Here's an example of a polymorphic method with type type parameters that ignores the capital letter 
convention. It optionally returns both inputs if and only if they are both defined:

```scala:mdoc
def tuple[A, B](a: Option[A], b: Option[B]): Option[(A, B)] = (a, b) match {
  case (Some(a), Some(b)) => Some((a, b))
  case _ => None
}
```

It turns out to be a useful method we'll meet (and generalize) again later.


### Type Parameters

So methods and constructors take both type and value parameters; the former in square brackets and the latter in 
round brackets. The value parameters and body of the method can refer to type parameters.

Despite the symmetry, be sure to clearly understand how type and value parameters differ.
- Type parameters are passed or inferred at every call-site (ie code location) of a method and every instantiation of a 
data type. This happens at *compile-time* ie while the code is being compiled. 
- Value parameters are passed every time a method is invoked at *runtime*. The same call site can be invoked many times 
(eg in a loop) or that call site might never be invoked during the program's execution.

Type parameters are commonly left to the compiler to infer. For example, in the expression `Some(a)` from the 
example above, the type parameter `A` is inferred from context and not explicitly written. Expanded it looks like 
`Some[A](a)` but that gets boring quickly. 

It's worth noting that Scala also has an important mechanism to *infer value parameters* passed at runtime by matching 
the type of the parameter with the type of values in the surrounding scope. Such inferred parameters are called 
`implicit` and we'll cover them in the next chapter. 

### Higher-Kinded Types

We saw how `Option` be parameterised with a payload type to yield a type like `Option[Int]`. But does it make sense 
to talk about the type `Option` independent of any specific parameter? 

We can describe the general `Option` type as a family of related types that:
- Accept a single type parameter, being their payload 
- May or may not hold a single value of the payload

Types like `Option` (or `List`, or `Future`) are termed *higher-kinded* types, or alternately *type constructors*. It
 turns out to be useful to represent higher-kinded types in programming. Later chapters use them extensively so we'll
 leave the examples until then.

### Partially Applied Types

