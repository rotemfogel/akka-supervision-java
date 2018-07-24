package io.akka.supervision;

import akka.actor.AbstractLoggingActor;
import akka.event.LoggingReceive;

public class Storage extends AbstractLoggingActor {

    final DummyDB db = DummyDB.instance;

    @Override
    public Receive createReceive() {
        return LoggingReceive.create(receiveBuilder().
                match(StorageApi.Store.class, store -> {
                    db.save(store.entry.key, store.entry.value);
                }).
                match(StorageApi.Get.class, get -> {
                    Long value = db.load(get.key);
                    getSender().tell(new StorageApi.Entry(get.key, value == null ?
                            Long.valueOf(0L) : value), getSelf());
                }).build(), getContext());
    }
}