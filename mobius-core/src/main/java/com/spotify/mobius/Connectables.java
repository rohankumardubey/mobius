/*
 * -\-\-
 * Mobius
 * --
 * Copyright (c) 2017-2018 Spotify AB
 * --
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
 * -/-/-
 */
package com.spotify.mobius;

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Function;
import javax.annotation.Nonnull;

/** Contains utility functions for working with {@link Connectable}s. */
public final class Connectables {
  private Connectables() {
    // prevent instantiation
  }

  /**
   * Create a {@link Connectable} that runs the supplied {@link Runnable} for each incoming value.
   * This connectable never creates output items.
   *
   * @param action the action to run for each input value
   * @param <I> the input type
   * @param <O> the output type, which is ignored
   */
  public static <I, O> Connectable<I, O> fromRunnable(final Runnable action) {
    checkNotNull(action);

    return new Connectable<I, O>() {
      @Nonnull
      @Override
      public Connection<I> connect(Consumer<O> output) throws ConnectionLimitExceededException {
        return new Connection<I>() {
          @Override
          public void accept(I value) {
            action.run();
          }

          @Override
          public void dispose() {}
        };
      }
    };
  }

  /**
   * Create a {@link Connectable} that applies the supplied {@link Consumer} to each incoming value.
   * This connectable never creates output items.
   *
   * @param consumer the consumer for each input value
   * @param <I> the input type
   * @param <O> the output type, which is ignored
   */
  public static <I, O> Connectable<I, O> fromConsumer(final Consumer<I> consumer) {
    checkNotNull(consumer);

    return new Connectable<I, O>() {
      @Nonnull
      @Override
      public Connection<I> connect(Consumer<O> output) throws ConnectionLimitExceededException {
        return new Connection<I>() {
          @Override
          public void accept(I value) {
            consumer.accept(value);
          }

          @Override
          public void dispose() {}
        };
      }
    };
  }

  /**
   * Create a {@link Connectable} that applies the supplied {@link Function} to each incoming value
   * and forwards the result as an output value.
   *
   * @param function the function used to create outputs for each input
   * @param <I> the input type
   * @param <O> the output type
   */
  public static <I, O> Connectable<I, O> fromFunction(final Function<I, O> function) {
    checkNotNull(function);

    return new Connectable<I, O>() {
      @Nonnull
      @Override
      public Connection<I> connect(final Consumer<O> output)
          throws ConnectionLimitExceededException {
        return new Connection<I>() {
          @Override
          public void accept(I value) {
            output.accept(function.apply(value));
          }

          @Override
          public void dispose() {}
        };
      }
    };
  }
}
