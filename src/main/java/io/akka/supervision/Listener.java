package io.akka.supervision;

import akka.actor.AbstractLoggingActor;
import akka.actor.ReceiveTimeout;
import akka.event.LoggingReceive;

import java.time.Duration;

/**
 * Listens on progress from the worker and shuts down the system when enough
 * work has been done.
 */
public class Listener extends AbstractLoggingActor {

    @Override
    public void preStart() {
        // If we don't get any progress within 15 seconds then the service
        // is unavailable
        getContext().setReceiveTimeout(Duration.ofSeconds(15));
    }

    @Override
    public Receive createReceive() {
        return LoggingReceive.create(receiveBuilder().
                match(WorkerApi.Progress.class, progress -> {
                    log().info("Current progress: {} %", progress.percent);
                    if (progress.percent >= 100.0) {
                        log().info("That's all, shutting down");
                        getContext().getSystem().terminate();
                    }
                }).
                matchEquals(ReceiveTimeout.getInstance(), x -> {
                    // No progress within 15 seconds, ServiceUnavailable
                    log().error("Shutting down due to unavailable service");
                    getContext().getSystem().terminate();
                }).build(), getContext());
    }
}