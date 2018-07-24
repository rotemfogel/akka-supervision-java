package io.akka.supervision;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.event.LoggingReceive;

import static io.akka.supervision.CounterApi.UseStorage;

/**
 * The in memory count variable that will send current value to the Storage,
 * if there is any storage available at the moment.
 */
public class Counter extends AbstractLoggingActor {
    final String key;
    long count;
    ActorRef storage;

    public Counter(String key, long initialValue) {
        this.key = key;
        this.count = initialValue;
    }

    @Override
    public Receive createReceive() {
        return LoggingReceive.create(receiveBuilder().
                match(UseStorage.class, useStorage -> {
                    storage = useStorage.storage;
                    storeCount();
                }).
                match(CounterServiceApi.Increment.class, increment -> {
                    count += increment.n;
                    storeCount();
                }).
                matchEquals(CounterServiceApi.GetCurrentCount, gcc -> {
                    getSender().tell(new CounterServiceApi.CurrentCount(key, count), getSelf());
                }).build(), getContext());
    }

    void storeCount() {
        // Delegate dangerous work, to protect our valuable state.
        // We can continue without storage.
        if (storage != null) {
            storage.tell(new StorageApi.Store(new StorageApi.Entry(key, count)), getSelf());
        }
    }
}
