package io.akka.supervision;

import akka.actor.ActorRef;

public interface CounterApi {
    class UseStorage {
        public final ActorRef storage;

        public UseStorage(ActorRef storage) {
            this.storage = storage;
        }

        public String toString() {
            return String.format("%s(%s)", getClass().getSimpleName(), storage);
        }
    }
}