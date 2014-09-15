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
 *    Franz Wilhelmstötter (franz.wilhelmstoetter@gmx.at)
 */
package org.jenetics.engine;

import static java.util.Objects.requireNonNull;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jenetics.Gene;

/**
 * Spliterator which is used for building the evolution stream.
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmx.at">Franz Wilhelmstötter</a>
 * @since 3.0
 * @version 3.0 &mdash; <em>$Date: 2014-09-15 $</em>
 */
final class UnlimitedEvolutionSpliterator<
	G extends Gene<?, G>,
	C extends Comparable<? super C>
>
	implements Spliterator<EvolutionResult<G, C>>
{

	private final Function<EvolutionStart<G, C>, EvolutionResult<G, C>> _evolution;
	private final Supplier<EvolutionStart<G, C>> _initial;

	private EvolutionStart<G, C> _start;

	public UnlimitedEvolutionSpliterator(
		final Function<EvolutionStart<G, C>, EvolutionResult<G, C>> evolution,
		final Supplier<EvolutionStart<G, C>> initial
	) {
		_evolution = requireNonNull(evolution);
		_initial = requireNonNull(initial);
	}

	@Override
	public boolean tryAdvance(
		final Consumer<? super EvolutionResult<G, C>> action
	) {
		if (_start == null) {
			_start = _initial.get();
		}

		final EvolutionResult<G, C> result = _evolution.apply(_start);
		action.accept(result);
		_start = result.next();

		return true;
	}

	@Override
	public Spliterator<EvolutionResult<G, C>> trySplit() {
		return null;
	}

	@Override
	public long estimateSize() {
		return Long.MAX_VALUE;
	}

	@Override
	public int characteristics() {
		return Spliterator.NONNULL | Spliterator.IMMUTABLE;
	}
}
