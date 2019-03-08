# Functional Programming Foundations

Functional Programming (FP) is a programming style emphasising the use of *functions*. Stream-oriented FP, which
 is the topic of this workshop, additionally emphasises *streams* to represent computations that repeat multiple times,
 typically either because we need to do a similar task for different data items, or perform a similar task at different
 times.

 Before introducing streams, we'll start with the general foundation concepts of functional programming.

 ## Functions

 In FP, functions are defined similarly to mathematics:

 - They may have a name or they may be anonymous
 - They have zero to N parameters
 - They compute a result- or return- value
 - Other than their return value, they have no other effects on the world
 - For given input parameters, they always compute the same result

## Statically Typed Functional Programming

This workshop teaches *statically typed* functional programming. Static typing classifies all the terms (ie data
 and functions) in the program using a type. A type defines and constrains the *set* of possible values that the term can
 hold.

So in typed FP, we additionally must specify the type of each function parameter, and the type of the function result.

### Referential Transparency

The last two properties, when combined, mean that a function invocation may be freely substituted for its result value,
without making an observable difference to the world. This property is called *referential transparency*.

The term *pure* is often used to describe functions that are *referentially transparent*; the meanings are equivalent.

### Quiz1: Which of the methods on the `Quiz1` object are pure functions?
```scala mdoc
object Q1 {
  def head[T](l: List[T]): T = l.head

  def headOption[T](l: List[T]): Option[T] = l.headOption

}
class Q2[T](l: List[T]) {

    def unCons: (Option[T], Q2[T]) = l match {
      case h :: t => (Some(h), new Q2(t))
      case nil => (None, new Q2(nil))
    }

}
```

<details>
<summary>Quiz 1 Answers</summary>
<p>
#### Q1

`head`: Impure. Can have the effect of throwing an exception rather than returning a value of type `T`.

`headOption`: Pure.

#### Q2

`unCons`: Pure. When object-oriented syntax is desugared, there's an extra `this` parameter passed to every method. So
the use of the class field `l` is actually via a hidden parameter.


</p>
</details>

