/*
 * Java Genetic Algorithm Library (@__identifier__@).
 * Copyright (c) @__year__@ Franz Wilhelmstötter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author:
 *    Franz Wilhelmstötter (franz.wilhelmstoetter@gmail.com)
 */
package io.jenetics.stat;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * This <i>consumer</i> class is used for calculating the min and max value
 * according to the given {@code Comparator}.
 * <p>
 * This class is designed to work with (though does not require) streams. For
 * example, you can compute minimum and maximum values with:
 * <pre>{@code
 * final Stream<Integer> stream = ...
 * final MinMax<Integer> minMax = stream.collect(
 *         MinMax::of,
 *         MinMax::accept,
 *         MinMax::combine
 *     );
 * }</pre>
 *
 * @implNote
 * This implementation is not thread safe. However, it is safe to use on a
 * parallel stream, because the parallel implementation of
 * {@link java.util.stream.Stream#collect Stream.collect()}provides the
 * necessary partitioning, isolation, and merging of results for safe and
 * efficient parallel execution.
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @since 3.0
 * @version 6.0
 */
public final class MinMax<C> implements Consumer<C> {

	private final Comparator<? super C> _comparator;

	private C _min;
	private C _max;
	private long _count = 0L;

	private MinMax(final Comparator<? super C> comparator) {
		_comparator = requireNonNull(comparator);
	}

	/**
	 * Accept the element for min-max calculation.
	 *
	 * @param object the element to use for min-max calculation
	 */
	@Override
	public void accept(final C object) {
		_min = min(_comparator, _min, object);
		_max = max(_comparator, _max, object);
		++_count;
	}

	/**
	 * Combine two {@code MinMax} objects.
	 *
	 * @param other the other {@code MinMax} object to combine
	 * @return {@code this}
	 * @throws java.lang.NullPointerException if the {@code other} object is
	 *         {@code null}.
	 */
	public MinMax<C> combine(final MinMax<C> other) {
		_min = min(_comparator, _min, other._min);
		_max = max(_comparator, _max, other._max);
		_count += other._count;

		return this;
	}

	/**
	 * Returns the count of values recorded.
	 *
	 * @return the count of recorded values
	 */
	public long count() {
		return _count;
	}

	/**
	 * Return the current minimal object or {@code null} if no element has been
	 * accepted yet.
	 *
	 * @return the current minimal object
	 */
	public C min() {
		return _min;
	}

	/**
	 * Return the current maximal object or {@code null} if no element has been
	 * accepted yet.
	 *
	 * @return the current maximal object
	 */
	public C max() {
		return _max;
	}

	/**
	 * Compares the state of two {@code LongMomentStatistics} objects. This is
	 * a replacement for the {@link #equals(Object)} which is not advisable to
	 * implement for this mutable object. If two object have the same state, it
	 * has still the same state when updated with the same value.
	 * <pre>{@code
	 * final MinMax mm1 = ...;
	 * final MinMax mm2 = ...;
	 *
	 * if (mm1.sameState(mm2)) {
	 *     final long value = random.nextInt(1_000_000);
	 *     mm1.accept(value);
	 *     mm2.accept(value);
	 *
	 *     assert mm1.sameState(mm2);
	 *     assert mm2.sameState(mm1);
	 *     assert mm1.sameState(mm1);
	 * }
	 * }</pre>
	 *
	 * @since 3.7
	 *
	 * @param other the other object for the test
	 * @return {@code true} the {@code this} and the {@code other} objects have
	 *         the same state, {@code false} otherwise
	 */
	public boolean sameState(final MinMax<C> other) {
		return this == other ||
			Objects.equals(_min, other._min) &&
			Objects.equals(_max, other._max);
	}

	@Override
	public String toString() {
		return format("MinMax[count=%d, min=%s, max=%s]", _count, _min, _max);
	}

	/* *************************************************************************
	 *  Some static helper methods.
	 * ************************************************************************/

	/**
	 * Return the minimum of two values, according the given comparator.
	 * {@code null} values are allowed.
	 *
	 * @param comp the comparator used for determining the min value
	 * @param a the first value to compare
	 * @param b the second value to compare
	 * @param <T> the type of the compared objects
	 * @return the minimum value, or {@code null} if both values are {@code null}.
	 *         If only one value is {@code null}, the non {@code null} values is
	 *         returned.
	 */
	public static <T> T
	min(final Comparator<? super T> comp, final T a, final T b) {
		return a != null ? b != null ? comp.compare(a, b) <= 0 ? a : b : a : b;
	}

	/**
	 * Return the maximum of two values, according the given comparator.
	 * {@code null} values are allowed.
	 *
	 * @param comp the comparator used for determining the max value
	 * @param a the first value to compare
	 * @param b the second value to compare
	 * @param <T> the type of the compared objects
	 * @return the maximum value, or {@code null} if both values are {@code null}.
	 *         If only one value is {@code null}, the non {@code null} values is
	 *         returned.
	 */
	public static <T> T
	max(final Comparator<? super T> comp, final T a, final T b) {
		return a != null ? b != null ? comp.compare(a, b) >= 0 ? a : b : a : b;
	}


	/* *************************************************************************
	 *  Some static factory methods.
	 * ************************************************************************/

	/**
	 * Return a {@code Collector} which calculates the minimum and maximum value.
	 * The given {@code comparator} is used for comparing two objects.
	 *
	 * <pre>{@code
	 * final Comparator<SomeObject> comparator = ...
	 * final Stream<SomeObject> stream = ...
	 * final MinMax<SomeObject> moments = stream
	 *     .collect(doubleMoments.toMinMax(comparator));
	 * }</pre>
	 *
	 * @param comparator the {@code Comparator} to use
	 * @param <T> the type of the input elements
	 * @return a {@code Collector} implementing the min-max reduction
	 * @throws java.lang.NullPointerException if the given {@code mapper} is
	 *         {@code null}
	 */
	public static <T> Collector<T, ?, MinMax<T>>
	toMinMax(final Comparator<? super T> comparator) {
		requireNonNull(comparator);
		return Collector.of(
			() -> MinMax.of(comparator),
			MinMax::accept,
			MinMax::combine
		);
	}

	/**
	 * Return a {@code Collector} which calculates the minimum and maximum value.
	 * The <i>reducing</i> objects must be comparable.
	 *
	 * <pre>{@code
	 * final Stream<SomeObject> stream = ...
	 * final MinMax<SomeObject> moments = stream
	 *     .collect(doubleMoments.toMinMax(comparator));
	 * }</pre>
	 *
	 * @param <C> the type of the input elements
	 * @return a {@code Collector} implementing the min-max reduction
	 * @throws java.lang.NullPointerException if the given {@code mapper} is
	 *         {@code null}
	 */
	public static <C extends Comparable<? super C>>
	Collector<C, ?, MinMax<C>> toMinMax() {
		return toMinMax(Comparator.naturalOrder());
	}

	/**
	 * Create a new {@code MinMax} <i>consumer</i> with the given
	 * {@link java.util.Comparator}.
	 *
	 * @param comparator the comparator used for comparing two elements
	 * @param <T> the element type
	 * @return a new {@code MinMax} <i>consumer</i>
	 * @throws java.lang.NullPointerException if the {@code comparator} is
	 *         {@code null}.
	 */
	public static <T> MinMax<T> of(final Comparator<? super T> comparator) {
		return new MinMax<>(comparator);
	}

	/**
	 * Create a new {@code MinMax} <i>consumer</i>.
	 *
	 * @param <C> the element type
	 * @return a new {@code MinMax} <i>consumer</i>
	 */
	public static <C extends Comparable<? super C>> MinMax<C> of() {
		return of(Comparator.naturalOrder());
	}


	/* *************************************************************************
	 *  Some "flat" mapper functions.
	 * ************************************************************************/

	/**
	 * Return a new flat-mapper function, which guarantees a strictly increasing
	 * stream, from an arbitrarily ordered source stream. Note that this
	 * function doesn't sort the stream. It <em>just</em> skips the <em>out of
	 * order</em> elements.
	 *
	 * <pre>{@code
	 * final ISeq<Integer> values = new Random().ints(0, 100)
	 *     .boxed()
	 *     .limit(100)
	 *     .flatMap(MinMax.toStrictlyIncreasing())
	 *     .collect(ISeq.toISeq());
	 *
	 * System.out.println(values);
	 * // [6,47,65,78,96,96,99]
	 * }</pre>
	 *
	 * @since 5.0
	 *
	 * @param <C> the comparable type
	 * @return a new flat-mapper function
	 */
	public static <C extends Comparable<? super C>>
	Function<C, Stream<C>> toStrictlyIncreasing() {
		return toStrictly(MinMax::max);
	}

	/**
	 * Return a new flat-mapper function, which guarantees a strictly decreasing
	 * stream, from an arbitrarily ordered source stream. Note that this
	 * function doesn't sort the stream. It <em>just</em> skips the <em>out of
	 * order</em> elements.
	 *
	 * <pre>{@code
	 * final ISeq<Integer> values = new Random().ints(0, 100)
	 *     .boxed()
	 *     .limit(100)
	 *     .flatMap(MinMax.toStrictlyDecreasing())
	 *     .collect(ISeq.toISeq());
	 *
	 * System.out.println(values);
	 * // [45,32,15,12,3,1]
	 * }</pre>
	 *
	 * @since 5.0
	 *
	 * @param <C> the comparable type
	 * @return a new flat-mapper function
	 */
	public static <C extends Comparable<? super C>>
	Function<C, Stream<C>> toStrictlyDecreasing() {
		return toStrictly(MinMax::min);
	}

	private static <C>
	Function<C, Stream<C>> toStrictly(final BinaryOperator<C> comp) {
		return new Function<>() {
			private C _best;

			@Override
			public Stream<C> apply(final C result) {
				final C best = comp.apply(_best, result);

				final Stream<C> stream = best == _best
					? Stream.empty()
					: Stream.of(best);

				_best = best;

				return stream;
			}
		};
	}

	private static <T extends Comparable<? super T>> T max(final T a, final T b) {
		if (a == null && b == null) return null;
		if (a == null) return b;
		if (b == null) return a;
		return a.compareTo(b) >= 0 ? a : b;
	}

	private static <T extends Comparable<? super T>> T min(final T a, final T b) {
		if (a == null && b == null) return null;
		if (a == null) return b;
		if (b == null) return a;
		return a.compareTo(b) <= 0 ? a : b;
	}

	/**
	 * Return a new flat-mapper function which returns (emits) the maximal value
	 * of the last <em>n</em> elements.
	 *
	 * @param size the size of the slice
	 * @param <C> the element type
	 * @return a new flat-mapper function
	 * @throws IllegalArgumentException if the given size is smaller than one
	 */
	public static <C extends Comparable<? super C>>
	Function<C, Stream<C>> toSliceMax(final int size) {
		return toSliceBest(MinMax::max, size);
	}

	/**
	 * Return a new flat-mapper function which returns (emits) the minimal value
	 * of the last <em>n</em> elements.
	 *
	 * @param size the size of the slice
	 * @param <C> the element type
	 * @return a new flat-mapper function
	 * @throws IllegalArgumentException if the given size is smaller than one
	 */
	public static <C extends Comparable<? super C>>
	Function<C, Stream<C>> toSliceMin(final int size) {
		return toSliceBest(MinMax::min, size);
	}


	private static <C> Function<C, Stream<C>> toSliceBest(
		final BinaryOperator<C> comp,
		final int rangeSize
	) {
		if (rangeSize < 1) {
			throw new IllegalArgumentException(
				"Range size must be at least one: " + rangeSize
			);
		}

		return new Function<>() {
			private int _count = 0;
			private C _best;

			@Override
			public Stream<C> apply(final C value) {
				++_count;
				_best = comp.apply(_best, value);

				final Stream<C> result;
				if (_count >= rangeSize) {
					result = Stream.of(_best);
					_count = 0;
					_best = null;
				} else {
					result = Stream.empty();
				}

				return result;
			}
		};
	}

}
