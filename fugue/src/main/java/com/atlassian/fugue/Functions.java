/*
   Copyright 2011 Atlassian

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.atlassian.fugue;

import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Utility methods for Functions
 * <P>
 * Note that this class defines Partial Functions to be functions that return an
 * {@link Option} of the result type, and has some methods for creating them.
 * 
 * @since 1.1
 */
public class Functions {

  // /CLOVER:OFF

  private Functions() {}

  // /CLOVER:ON
  /**
   * Returns the composition of two functions. For {@code f: A->B} and
   * {@code g: B->C}, composition is defined as the function h such that
   * {@code h(a) == g(f(a))} for each {@code a}.
   *
   * @param <A> The start type
   * @param <B> The intermediate type
   * @param <C> The finished type
   * @param g the second function to apply
   * @param f the first function to apply
   * @return the composition of {@code f} and {@code g}
   * @see <a href="//en.wikipedia.org/wiki/Function_composition">function
   * composition</a>
   */
  public static <A, B, C> Function<A, C> compose(Function<? super B, ? extends C> g, Function<? super A, ? extends B> f) {
    return new FunctionComposition<>(g, f);
  }

  private static class FunctionComposition<A, B, C> implements Function<A, C>, Serializable {
    private final Function<? super B, ? extends C> g;
    private final Function<? super A, ? extends B> f;

    FunctionComposition(Function<? super B, ? extends C> g, Function<? super A, ? extends B> f) {
      this.g = requireNonNull(g);
      this.f = requireNonNull(f);
    }

    @Override public C apply(A a) {
      return g.apply(f.apply(a));
    }

    @Override public boolean equals(Object obj) {
      if (obj instanceof FunctionComposition) {
        FunctionComposition<?, ?, ?> that = (FunctionComposition<?, ?, ?>) obj;
        return f.equals(that.f) && g.equals(that.g);
      }
      return false;
    }

    @Override public int hashCode() {
      return f.hashCode() ^ g.hashCode();
    }

    @Override public String toString() {
      return g.toString() + "(" + f.toString() + ")";
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Apply f to each element in elements, with each application using the result
   * of the previous application as the other argument to f. zero is used as the
   * first 'result' value. The final result is returned.
   * 
   * @param <F> the element type
   * @param <T> the final result type
   * @param f the function to apply to all the elements
   * @param zero the starting point for the function
   * @param elements the series of which each element will be accumulated into a
   * result
   * 
   * @return the result of accumulating the application of f to all elements
   * 
   * @since 1.1
   */
  public static <F, T> T fold(final BiFunction<? super T, F, T> f, final T zero, final Iterable<? extends F> elements) {
    T currentValue = zero;
    for (final F element : elements) {
      currentValue = f.apply(currentValue, element);
    }
    return currentValue;
  }

  /**
   * Apply f to each element in elements, with each application using the result
   * of the previous application as the other argument to f. zero is used as the
   * first 'result' value. The final result is returned.
   * 
   * @param <F> the element type
   * @param <S> the accumulator function input type
   * @param <T> the final result type
   * @param f the function to apply to all elements
   * @param zero the starting point for the function
   * @param elements the series of which each element will be accumulated into a
   * result
   * 
   * @return the result of accumulating the application of f to all elements
   * 
   * @since 1.1
   */
  public static <F, S, T extends S> T fold(final Function<Pair<S, F>, T> f, final T zero,
    final Iterable<? extends F> elements) {
    return fold(toBiFunction(f), zero, elements);
  }

  /**
   * Function that takes another function and applies it to the argument.
   * 
   * @param <A> the argument and function input type
   * @param <B> the result type
   * @param arg the argument that will be applied to any input functions
   * @return a function that takes a function from A to B , applies the arg and
   * returns the result
   * 
   * @since 1.1
   */
  public static <A, B> Function<Function<A, B>, B> apply(final A arg) {
    return new Function<Function<A, B>, B>() {
      public B apply(final Function<A, B> f) {
        return f.apply(arg);
      }

      @Override public String toString() {
        return "Apply";
      }
    };
  }

  /**
   * Function that takes another function and applies it to the argument
   * supplied by the parameter.
   * 
   * @param lazyA the supplier of the argument that will be applied to any input
   * functions
   * @param <A> the type of the argument supplied, and the function input type
   * @param <B> the result type of the function
   * @return a function that takes a function from A to B, applies the argument
   * from the supplier and returns the result
   * 
   * @since 2.0
   */
  public static <A, B> Function<Function<A, B>, B> apply(final Supplier<A> lazyA) {
    return new Function<Function<A, B>, B>() {
      @Override public B apply(Function<A, B> f) {
        return f.apply(lazyA.get());
      }

      @Override public String toString() {
        return "ApplySupplier";
      }
    };
  }

  /**
   * Partial Function that does a type check and matches if the value is of the
   * right type.
   * 
   * @param <A> the input type
   * @param <B> the type we expect it to be
   * @param cls the type to check against.
   * @return a function that returns a Some with the value if of the right type
   * otherwise a None
   * @since 1.2
   */
  public static <A, B> Function<A, Option<B>> isInstanceOf(Class<B> cls) {
    return new InstanceOf<>(cls);
  }

  static class InstanceOf<A, B> implements Function<A, Option<B>> {
    private final Class<B> cls;

    InstanceOf(Class<B> cls) {
      this.cls = requireNonNull(cls);
    }

    public Option<B> apply(A a) {
      return (cls.isAssignableFrom(a.getClass())) ? Option.some(cls.cast(a)) : Option.<B> none();
    }

    @Override public String toString() {
      return "InstanceOf";
    }

    @Override public int hashCode() {
      return cls.hashCode();
    }

    static final long serialVersionUID = 0;
  }

  /**
   * Create a PartialFunction from a {@link Predicate} and a {@link Function}.
   * 
   * @param <A> the input type
   * @param <B> the output type
   * @param p the predicate to test the value against.
   * @param f the function to apply if the predicate passes.
   * @return a PartialFunction that tests the supplied predicate before applying
   * the function.
   * @since 1.2
   */
  public static <A, B> Function<A, Option<B>> partial(Predicate<? super A> p, Function<? super A, ? extends B> f) {
    return new Partial<>(p, f);
  }

  static class Partial<A, B> implements Function<A, Option<B>> {
    private final Predicate<? super A> p;
    private final Function<? super A, ? extends B> f;

    Partial(Predicate<? super A> p, Function<? super A, ? extends B> f) {
      this.p = requireNonNull(p);
      this.f = requireNonNull(f);
    }

    public Option<B> apply(A a) {
      return (p.test(a)) ? Option.option(f.apply(a)) : Option.<B> none();
    }

    @Override public String toString() {
      return "Partial";
    }

    @Override public int hashCode() {
      return f.hashCode() ^ p.hashCode();
    }
  }

  /**
   * Compose two PartialFunctions into one.
   * <p>
   * Kleisli composition. In Haskell it is defined as <code>&gt;=&gt;</code>,
   * AKA <a href="http://stackoverflow.com/a/7833488/210216">
   * "compose, fishy, compose"</a>
   * 
   * @param <A> the input type
   * @param <B> the middle type
   * @param <C> the output type
   * @param bc partial function from {@code B -> C}
   * @param ab partial function from {@code A -> B}
   * @return a PartialFunction that flatMaps g on to the result of applying f.
   * @since 1.2
   */

  public static <A, B, C> Function<A, Option<C>> composeOption(Function<? super B, ? extends Option<? extends C>> bc,
    Function<? super A, ? extends Option<? extends B>> ab) {
    return new PartialComposer<>(ab, bc);
  }

  static class PartialComposer<A, B, C> implements Function<A, Option<C>> {
    private final Function<? super A, ? extends Option<? extends B>> ab;
    private final Function<? super B, ? extends Option<? extends C>> bc;

    PartialComposer(Function<? super A, ? extends Option<? extends B>> ab,
      Function<? super B, ? extends Option<? extends C>> bc) {
      this.ab = requireNonNull(ab);
      this.bc = requireNonNull(bc);
    }

    public Option<C> apply(A a) {
      return ab.apply(a).flatMap(bc);
    }

    @Override public String toString() {
      return "PartialComposer";
    }

    @Override public int hashCode() {
      return bc.hashCode() ^ ab.hashCode();
    }
  }

  /**
   * Converts a function that takes a pair of arguments to a function that takes
   * two arguments
   * 
   * @param <A> the type of the left of the pair
   * @param <B> the type of the right of the pair
   * @param <C> the result type
   * @param fpair the source function that takes a pair of arguments
   * @return a function that takes two arguments
   * @since 2.0
   */
  public static <A, B, C> BiFunction<A, B, C> toBiFunction(final Function<Pair<A, B>, C> fpair) {
    requireNonNull(fpair);
    return new BiFunction<A, B, C>() {
      @Override public C apply(A a, B b) {
        return fpair.apply(Pair.pair(a, b));
      }

      @Override public String toString() {
        return "ToBiFunction";
      }
    };
  }

  /**
   * Transforms a function that takes 2 arguments into a function that takes the
   * first argument and return a new function that takes the second argument and
   * return the final result.
   * 
   * @param <A> the type of the first argument
   * @param <B> the type of the second argument
   * @param <C> the type of the final result
   * @param f2 the original function that takes 2 arguments
   * @return the curried form of the original function
   * @since 2.0
   */
  public static <A, B, C> Function<A, Function<B, C>> curried(final BiFunction<A, B, C> f2) {
    requireNonNull(f2);
    return new CurriedFunction<>(f2);
  }

  private static class CurriedFunction<A, B, C> implements Function<A, Function<B, C>> {
    private final BiFunction<A, B, C> f2;

    CurriedFunction(BiFunction<A, B, C> f2) {
      this.f2 = f2;
    }

    @Override public Function<B, C> apply(final A a) {
      return b -> f2.apply(a, b);
    }

    @Override public String toString() {
      return "CurriedFunction";
    }

    @Override public int hashCode() {
      return f2.hashCode();
    }
  }

  /**
   * Transforms a function from {@code A -> (B -> C)} into a function from
   * {@code B -> (A -> C)}.
   * 
   * @param <A> the type of the first argument
   * @param <B> the type of the second argument
   * @param <C> the type of the final result
   * @param f2 the original function from {@code A -> (B -> C)}
   * @return the flipped form of the original function
   * @since 2.0
   */
  public static <A, B, C> Function<B, Function<A, C>> flip(final Function<A, Function<B, C>> f2) {
    requireNonNull(f2);
    return new FlippedFunction<>(f2);
  }

  private static class FlippedFunction<A, B, C> implements Function<B, Function<A, C>> {
    private final Function<A, Function<B, C>> f2;

    FlippedFunction(Function<A, Function<B, C>> f2) {
      this.f2 = f2;
    }

    @Override public Function<A, C> apply(final B b) {
      return a -> f2.apply(a).apply(b);
    }

    @Override public String toString() {
      return "FlippedFunction";
    }

    @Override public int hashCode() {
      return f2.hashCode();
    }
  }

  /**
   * Maps a function that returns nulls into a Partial function that returns an
   * Option of the result.
   * 
   * @param <A> the input type
   * @param <B> the output type
   * @param f the function that may return nulls
   * @return a function that converts any nulls into Options
   * @since 2.0
   */
  public static <A, B> Function<A, Option<B>> mapNullToOption(Function<? super A, ? extends B> f) {
    return Functions.compose(Functions.<B> nullToOption(), f);
  }

  /**
   * Function that turns null inputs into a none, and not-null inputs into some.
   * 
   * @param <A> the input type
   * @return a function that never returns nulls.
   * @since 2.2.1
   */
  public static <A> Function<A, Option<A>> nullToOption() {
    return Options.toOption();
  }

  /**
   * Takes a Function and memoizes (caches) the result for each input. This
   * memoization is weak, so it shouldn't leak memory on its own, but equally it
   * may expunge entries if no-one else is holding the reference in the
   * meantime.
   * <p>
   * NOTE: it is very important that the docs on the input type are read
   * carefully. Failure to heed adhere to this will lead to unspecified behavior
   * (bugs!)
   * 
   * @param <A> the input type, like any cache, this type should be a value,
   * that is it should be immutable and have correct hashcode and equals
   * implementations.
   * @param <B> the output type
   * @param f the function who's output will be memoized
   * @return a function that memoizes the results of the function using the
   * input as a weak key
   * 
   * @since 2.2
   */
  public static <A, B> Function<A, B> weakMemoize(Function<A, B> f) {
    return WeakMemoizer.weakMemoizer(f);
  }

  /**
   * Get a function that uses the Supplier as a factory for all inputs.
   * 
   * @param <D> the key type, ignored
   * @param <R> the result type
   * @param supplier called for all inputs
   * @return the function
   * 
   * @since 2.2
   */
  static <D, R> Function<D, R> fromSupplier(final Supplier<R> supplier) {
    return new FromSupplier<>(supplier);
  }

  static class FromSupplier<D, R> implements Function<D, R> {
    private final Supplier<R> supplier;

    FromSupplier(final Supplier<R> supplier) {
      this.supplier = requireNonNull(supplier, "supplier");
    }

    public R apply(final D ignore) {
      return supplier.get();
    }

    @Override public String toString() {
      return "FromSupplier";
    }

    @Override public int hashCode() {
      return supplier.hashCode();
    }
  }

  /**
   * Returns the identity function.
   */
  @SuppressWarnings("unchecked") public static <A> Function<A, A> identity() {
    // cast a singleton Function<Object, Object> to a more useful type
    return (Function<A, A>) IdentityFunction.INSTANCE;
  }

  // enum singleton pattern
  private enum IdentityFunction implements Function<Object, Object> {
    INSTANCE;

    @Override public Object apply(Object o) {
      return o;
    }

    @Override public String toString() {
      return "identity";
    }
  }

  static <A, B> Function<A, B> constant(final B constant) {
    return from -> constant;
  }

  /**
   * Creates a stack of matcher functions and returns the first result that
   * matches.
   *
   * @param <A> the input type
   * @param <B> the output type
   * @param f1 partial function, tried in order.
   * @param f2 partial function, tried in order.
   * @return a PartialFunction that composes all the functions and tries each
   * one in sequence.
   * @since 1.2
   */
  public static <A, B> Function<A, Option<B>> matches(Function<? super A, ? extends Option<? extends B>> f1,
    Function<? super A, ? extends Option<? extends B>> f2) {
    return matcher(f1, f2);
  }

  /**
   * Creates a stack of matcher functions and returns the first result that
   * matches.
   *
   * @param <A> the input type
   * @param <B> the output type
   * @param f1 partial function, tried in order.
   * @param f2 partial function, tried in order.
   * @param f3 partial function, tried in order.
   * @return a PartialFunction that composes all the functions and tries each
   * one in sequence.
   * @since 1.2
   */
  public static <A, B> Function<A, Option<B>> matches(Function<? super A, ? extends Option<? extends B>> f1,
    Function<? super A, ? extends Option<? extends B>> f2, Function<? super A, ? extends Option<? extends B>> f3) {
    return matcher(f1, f2, f3);
  }

  /**
   * Creates a stack of matcher functions and returns the first result that
   * matches.
   *
   * @param <A> the input type
   * @param <B> the output type
   * @param f1 partial function, tried in order.
   * @param f2 partial function, tried in order.
   * @param f3 partial function, tried in order.
   * @param f4 partial function, tried in order.
   * @return a PartialFunction that composes all the functions and tries each
   * one in sequence.
   * @since 1.2
   */
  public static <A, B> Function<A, Option<B>> matches(Function<? super A, ? extends Option<? extends B>> f1,
    Function<? super A, ? extends Option<? extends B>> f2, Function<? super A, ? extends Option<? extends B>> f3,
    Function<? super A, ? extends Option<? extends B>> f4) {
    return new Matcher<>(Arrays.<Function<? super A, ? extends Option<? extends B>>> asList(f1, f2, f3, f4));
  }

  /**
   * Creates a stack of matcher functions and returns the first result that
   * matches.
   *
   * @param <A> the input type
   * @param <B> the output type
   * @param f1 partial function, tried in order.
   * @param f2 partial function, tried in order.
   * @param f3 partial function, tried in order.
   * @param f4 partial function, tried in order.
   * @param f5 partial function, tried in order.
   * @param fs partial functions, tried in order.
   * @return a PartialFunction that composes all the functions and tries each
   * one in sequence.
   * @since 1.2
   */
  @SafeVarargs public static <A, B> Function<A, Option<B>> matches(
    Function<? super A, ? extends Option<? extends B>> f1, Function<? super A, ? extends Option<? extends B>> f2,
    Function<? super A, ? extends Option<? extends B>> f3, Function<? super A, ? extends Option<? extends B>> f4,
    Function<? super A, ? extends Option<? extends B>> f5, Function<? super A, ? extends Option<? extends B>>... fs) {

    @SuppressWarnings("unchecked")
    Function<? super A, ? extends Option<? extends B>>[] matchingFunctions = new Function[5 + fs.length];
    matchingFunctions[0] = f1;
    matchingFunctions[1] = f2;
    matchingFunctions[2] = f3;
    matchingFunctions[3] = f4;
    matchingFunctions[4] = f5;
    System.arraycopy(fs, 0, matchingFunctions, 5, fs.length);

    return new Matcher<>(Arrays.asList(matchingFunctions));
  }

  /* utility copy function */
  @SafeVarargs private static <A, B> Matcher<A, B> matcher(Function<? super A, ? extends Option<? extends B>>... fs) {
    @SuppressWarnings("unchecked")
    Function<? super A, ? extends Option<? extends B>>[] dest = new Function[fs.length];

    System.arraycopy(fs, 0, dest, 0, fs.length);
    for (Function<? super A, ? extends Option<? extends B>> f : fs) {
      if (f == null) {
        throw new NullPointerException("function value was null");
      }
    }

    return new Matcher<>(Arrays.asList(dest));
  }

  static class Matcher<A, B> implements Function<A, Option<B>> {
    private final Iterable<Function<? super A, ? extends Option<? extends B>>> fs;

    Matcher(Iterable<Function<? super A, ? extends Option<? extends B>>> fs) {
      this.fs = requireNonNull(fs);

      if (!fs.iterator().hasNext()) {
        throw new IllegalArgumentException("Condition must be true but returned false instead");
      }
    }

    public Option<B> apply(A a) {
      for (Function<? super A, ? extends Option<? extends B>> f : fs) {
        @SuppressWarnings("unchecked")
        Option<B> b = (Option<B>) f.apply(a);
        if (b.isDefined())
          return b;
      }
      return Option.none();
    }

    @Override public String toString() {
      return "Matcher";
    }

    @Override public int hashCode() {
      return fs.hashCode();
    }
  }

  /**
   * Class supports the implementation of {@link Functions#weakMemoize(Function)}
   * and is not intended for general use.
   *
   * {@link WeakMemoizer} caches the result of another function. The result is
   * {@link WeakReference weakly referenced} internally. This is useful if the
   * result is expensive to compute or the identity of the result is particularly
   * important.
   * <p>
   * If the results from this function are further cached then they will tend to
   * stay in this cache for longer.
   *
   * @param <A> comparable descriptor, the usual rules for any {@link HashMap} key
   * apply.
   * @param <B> the value
   */
  static final class WeakMemoizer<A, B> implements Function<A, B> {
    static <A, B> WeakMemoizer<A, B> weakMemoizer(final Function<A, B> delegate) {
      return new WeakMemoizer<>(delegate);
    }

    private final ConcurrentMap<A, MappedReference<A, B>> map;
    private final ReferenceQueue<B> queue = new ReferenceQueue<B>();
    private final Function<A, B> delegate;

    /**
     * Construct a new {@link WeakMemoizer} instance.
     *
     * @param delegate for creating the initial values.
     * @throws IllegalArgumentException if the initial capacity of elements is
     * negative.
     */
    WeakMemoizer(Function<A, B> delegate) {
      this.map = new ConcurrentHashMap<>();
      this.delegate = requireNonNull(delegate, "delegate");
    }

    /**
     * Get a result for the supplied Descriptor.
     *q
     * @param descriptor must not be null
     * @return descriptor lock
     */
    public B apply(final A descriptor) {
      expungeStaleEntries();
      requireNonNull(descriptor, "descriptor");
      while (true) {
        final MappedReference<A, B> reference = map.get(descriptor);
        if (reference != null) {
          final B value = reference.get();
          if (value != null) {
            return value;
          }
          map.remove(descriptor, reference);
        }
        map.putIfAbsent(descriptor, new MappedReference<>(descriptor, delegate.apply(descriptor), queue));
      }
    }

    // expunge entries whose value reference has been collected
    @SuppressWarnings("unchecked") private void expungeStaleEntries() {
      MappedReference<A, B> ref;
      // /CLOVER:OFF
      while ((ref = (MappedReference<A, B>) queue.poll()) != null) {
        final A key = ref.getDescriptor();
        if (key == null) {
          // DO NOT REMOVE! In theory this should not be necessary as it
          // should not be able to be null - but we have seen it happen!
          continue;
        }
        map.remove(key, ref);
      }
      // /CLOVER:ON
    }

    /**
     * A weak reference that maintains a reference to the key so that it can be
     * removed from the map when the value is garbage collected.
     */
    static final class MappedReference<K, V> extends WeakReference<V> {
      private final K key;

      public MappedReference(final K key, final V value, final ReferenceQueue<? super V> q) {
        super(requireNonNull(value, "value"), q);
        this.key = requireNonNull(key, "key");
      }

      final K getDescriptor() {
        return key;
      }
    }
  }
}
