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
package com.spotify.mobius2.extras;

import static com.spotify.mobius2.extras.TestConnectable.State.CONNECTED;
import static com.spotify.mobius2.extras.TestConnectable.State.DISPOSED;

import com.spotify.mobius2.Connectable;
import com.spotify.mobius2.Connection;
import com.spotify.mobius2.extras.domain.B;
import com.spotify.mobius2.extras.domain.C;
import com.spotify.mobius2.functions.Consumer;
import com.spotify.mobius2.functions.Function;
import javax.annotation.Nonnull;

class TestConnectable implements Connectable<B, C> {

  enum State {
    DISPOSED,
    CONNECTED
  }

  private final Function<String, String> transform;

  State state;
  int connectionsCount;

  public static TestConnectable create(Function<String, String> transform) {
    return new TestConnectable(transform);
  }

  public static TestConnectable createWithReversingTransformation() {
    return create(value -> new StringBuilder(value).reverse().toString());
  }

  TestConnectable(Function<String, String> transform) {
    this.transform = transform;
  }

  @Nonnull
  @Override
  public Connection<B> connect(Consumer<C> output) {
    state = CONNECTED;
    connectionsCount++;
    return new Connection<B>() {
      @Override
      public void accept(B value) {
        output.accept(C.create(transform.apply(value.something())));
      }

      @Override
      public void dispose() {
        state = DISPOSED;
        connectionsCount--;
      }
    };
  }
}
