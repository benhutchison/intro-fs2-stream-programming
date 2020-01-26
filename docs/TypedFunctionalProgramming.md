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
