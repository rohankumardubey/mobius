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
package com.spotify.mobius2;

import com.spotify.mobius2.functions.Consumer;
import com.spotify.mobius2.functions.Producer;
import com.spotify.mobius2.internal_util.ImmutableUtil;
import com.spotify.mobius2.internal_util.Preconditions;
import com.spotify.mobius2.runners.WorkRunner;
import com.spotify.mobius2.runners.WorkRunners;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Mobius {
  private Mobius() {
    // prevent instantiation
  }

  private static final Connectable<?, ?> NOOP_EVENT_SOURCE =
      new Connectable<Object, Object>() {

        @Nonnull
        @Override
        public Connection<Object> connect(Consumer<Object> output)
            throws ConnectionLimitExceededException {
          return new Connection<Object>() {
            @Override
            public void accept(Object value) {}

            @Override
            public void dispose() {}
          };
        }
      };

  private static final MobiusLoop.Logger<?, ?, ?> NOOP_LOGGER =
      new MobiusLoop.Logger<Object, Object, Object>() {
        @Override
        public void beforeInit(Object model) {
          /* noop */
        }

        @Override
        public void afterInit(Object model, com.spotify.mobius2.First<Object, Object> result) {
          /* noop */
        }

        @Override
        public void exceptionDuringInit(Object model, Throwable exception) {
          System.err.println("error initialising from model: '" + model + "' - " + exception);
          exception.printStackTrace(System.err);
        }

        @Override
        public void beforeUpdate(Object model, Object event) {
          /* noop */
        }

        @Override
        public void afterUpdate(Object model, Object event, Next<Object, Object> result) {
          /* noop */
        }

        @Override
        public void exceptionDuringUpdate(Object model, Object event, Throwable exception) {
          System.err.println(
              "error updating model: '" + model + "' with event: '" + event + "' - " + exception);
          exception.printStackTrace(System.err);
        }
      };

  /**
   * Create a {@link MobiusLoop.Builder} to help you configure a MobiusLoop before starting it.
   *
   * <p>Once done configuring the loop you can start the loop using {@link
   * MobiusLoop.Factory#startFrom(Object)}.
   *
   * @param update the {@link Update} function of the loop
   * @param effectHandler the {@link Connectable} effect handler of the loop
   * @return a {@link MobiusLoop.Builder} instance that you can further configure before starting
   *     the loop
   */
  public static <M, E, F> MobiusLoop.Builder<M, E, F> loop(
      Update<M, E, F> update, Connectable<F, E> effectHandler) {

    //noinspection unchecked
    return new Builder<>(
        update,
        effectHandler,
        null,
        (Connectable<M, E>) NOOP_EVENT_SOURCE,
        (MobiusLoop.Logger<M, E, F>) NOOP_LOGGER,
        new com.spotify.mobius2.functions.Producer<com.spotify.mobius2.runners.WorkRunner>() {
          @Nonnull
          @Override
          public com.spotify.mobius2.runners.WorkRunner get() {
            return com.spotify.mobius2.runners.WorkRunners.from(
                Executors.newSingleThreadExecutor(Builder.THREAD_FACTORY));
          }
        },
        new com.spotify.mobius2.functions.Producer<com.spotify.mobius2.runners.WorkRunner>() {
          @Nonnull
          @Override
          public com.spotify.mobius2.runners.WorkRunner get() {
            return com.spotify.mobius2.runners.WorkRunners.from(
                Executors.newCachedThreadPool(Builder.THREAD_FACTORY));
          }
        });
  }

  /**
   * Create a {@link MobiusLoop.Controller} that allows you to start, stop, and restart MobiusLoops.
   *
   * @param loopFactory a factory for creating loops
   * @param defaultModel the model the controller should start from
   * @return a new controller
   */
  public static <M, E, F> MobiusLoop.Controller<M, E> controller(
      MobiusLoop.Factory<M, E, F> loopFactory, M defaultModel) {
    return new MobiusLoopController<>(
        loopFactory, defaultModel, null, com.spotify.mobius2.runners.WorkRunners.immediate());
  }

  /**
   * Create a {@link MobiusLoop.Controller} that allows you to start, stop, and restart MobiusLoops.
   *
   * @param loopFactory a factory for creating loops
   * @param defaultModel the model the controller should start from
   * @param init the init function to run when a loop starts
   * @return a new controller
   */
  public static <M, E, F> MobiusLoop.Controller<M, E> controller(
      MobiusLoop.Factory<M, E, F> loopFactory, M defaultModel, Init<M, F> init) {
    return new MobiusLoopController<>(loopFactory, defaultModel, init, WorkRunners.immediate());
  }

  /**
   * Create a {@link MobiusLoop.Controller} that allows you to start, stop, and restart MobiusLoops.
   *
   * @param loopFactory a factory for creating loops
   * @param defaultModel the model the controller should start from
   * @param modelRunner the WorkRunner to use when observing model changes
   * @return a new controller
   */
  public static <M, E, F> MobiusLoop.Controller<M, E> controller(
      MobiusLoop.Factory<M, E, F> loopFactory,
      M defaultModel,
      com.spotify.mobius2.runners.WorkRunner modelRunner) {
    return new MobiusLoopController<>(loopFactory, defaultModel, null, modelRunner);
  }

  /**
   * Create a {@link MobiusLoop.Controller} that allows you to start, stop, and restart MobiusLoops.
   *
   * @param loopFactory a factory for creating loops
   * @param defaultModel the model the controller should start from
   * @param init the init function to run when a loop starts
   * @param modelRunner the WorkRunner to use when observing model changes
   * @return a new controller
   */
  public static <M, E, F> MobiusLoop.Controller<M, E> controller(
      MobiusLoop.Factory<M, E, F> loopFactory,
      M defaultModel,
      Init<M, F> init,
      com.spotify.mobius2.runners.WorkRunner modelRunner) {
    return new MobiusLoopController<>(loopFactory, defaultModel, init, modelRunner);
  }

  private static final class Builder<M, E, F> implements MobiusLoop.Builder<M, E, F> {

    private static final MyThreadFactory THREAD_FACTORY = new MyThreadFactory();

    private final Update<M, E, F> update;
    private final Connectable<F, E> effectHandler;
    @Nullable private final Init<M, F> init;
    private final Connectable<M, E> eventSource;
    private final com.spotify.mobius2.functions.Producer<com.spotify.mobius2.runners.WorkRunner>
        eventRunner;
    private final com.spotify.mobius2.functions.Producer<com.spotify.mobius2.runners.WorkRunner>
        effectRunner;
    private final MobiusLoop.Logger<M, E, F> logger;

    private Builder(
        Update<M, E, F> update,
        Connectable<F, E> effectHandler,
        @Nullable Init<M, F> init,
        Connectable<M, E> eventSource,
        MobiusLoop.Logger<M, E, F> logger,
        com.spotify.mobius2.functions.Producer<com.spotify.mobius2.runners.WorkRunner> eventRunner,
        com.spotify.mobius2.functions.Producer<com.spotify.mobius2.runners.WorkRunner>
            effectRunner) {
      this.update = Preconditions.checkNotNull(update);
      this.effectHandler = Preconditions.checkNotNull(effectHandler);
      this.init = init;
      this.eventSource = Preconditions.checkNotNull(eventSource);
      this.eventRunner = Preconditions.checkNotNull(eventRunner);
      this.effectRunner = Preconditions.checkNotNull(effectRunner);
      this.logger = Preconditions.checkNotNull(logger);
    }

    @Override
    @Nonnull
    public MobiusLoop.Builder<M, E, F> init(Init<M, F> init) {
      return new Builder<>(
          update,
          effectHandler,
          Preconditions.checkNotNull(init),
          eventSource,
          logger,
          eventRunner,
          effectRunner);
    }

    @Override
    @Nonnull
    public MobiusLoop.Builder<M, E, F> eventSource(Connectable<M, E> eventSource) {
      return new Builder<>(
          update, effectHandler, init, eventSource, logger, eventRunner, effectRunner);
    }

    @Override
    @Nonnull
    public MobiusLoop.Builder<M, E, F> eventSource(EventSource<E> eventSource) {
      return new Builder<>(
          update,
          effectHandler,
          init,
          EventSourceConnectable.<M, E>create(eventSource),
          logger,
          eventRunner,
          effectRunner);
    }

    @Nonnull
    @Override
    public MobiusLoop.Builder<M, E, F> eventSources(
        EventSource<E> eventSource, EventSource<E>... eventSources) {
      EventSource<E> mergedSource = MergedEventSource.from(eventSource, eventSources);
      return new Builder<>(
          update,
          effectHandler,
          init,
          EventSourceConnectable.<M, E>create(mergedSource),
          logger,
          eventRunner,
          effectRunner);
    }

    @Override
    @Nonnull
    public MobiusLoop.Builder<M, E, F> logger(MobiusLoop.Logger<M, E, F> logger) {
      return new Builder<>(
          update, effectHandler, init, eventSource, logger, eventRunner, effectRunner);
    }

    @Override
    @Nonnull
    public MobiusLoop.Builder<M, E, F> eventRunner(
        com.spotify.mobius2.functions.Producer<com.spotify.mobius2.runners.WorkRunner>
            eventRunner) {
      return new Builder<>(
          update, effectHandler, init, eventSource, logger, eventRunner, effectRunner);
    }

    @Override
    @Nonnull
    public MobiusLoop.Builder<M, E, F> effectRunner(Producer<WorkRunner> effectRunner) {
      return new Builder<>(
          update, effectHandler, init, eventSource, logger, eventRunner, effectRunner);
    }

    @Override
    @Nonnull
    public MobiusLoop<M, E, F> startFrom(final M startModel) {
      M firstModel = startModel;
      Set<F> firstEffects = ImmutableUtil.emptySet();

      if (init != null) {
        LoggingInit<M, F> loggingInit = new LoggingInit<>(init, logger);
        First<M, F> first = loggingInit.init(Preconditions.checkNotNull(startModel));

        firstModel = first.model();
        firstEffects = first.effects();
      }

      return startFromInternal(firstModel, firstEffects);
    }

    @Override
    @Nonnull
    public MobiusLoop<M, E, F> startFrom(M startModel, Set<F> startEffects) {
      if (init != null) {
        throw new IllegalArgumentException(
            "cannot pass in start effects when a loop has init defined");
      }

      return startFromInternal(startModel, startEffects);
    }

    private MobiusLoop<M, E, F> startFromInternal(M startModel, Set<F> startEffects) {
      LoggingUpdate<M, E, F> loggingUpdate = new LoggingUpdate<>(update, logger);

      return MobiusLoop.create(
          loggingUpdate,
          startModel,
          startEffects,
          effectHandler,
          eventSource,
          Preconditions.checkNotNull(eventRunner.get()),
          Preconditions.checkNotNull(effectRunner.get()));
    }

    private static class MyThreadFactory implements ThreadFactory {

      private static final AtomicLong threadCount = new AtomicLong(0);

      @Override
      public Thread newThread(Runnable runnable) {
        Thread thread =
            Executors.defaultThreadFactory().newThread(Preconditions.checkNotNull(runnable));

        thread.setName(
            String.format(Locale.ENGLISH, "mobius-thread-%d", threadCount.incrementAndGet()));

        return thread;
      }
    }
  }
}
