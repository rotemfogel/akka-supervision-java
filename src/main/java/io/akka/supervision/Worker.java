package io.akka.supervision;

import akka.actor.*;
import akka.dispatch.Mapper;
import akka.event.LoggingReceive;
import akka.japi.pf.DeciderBuilder;
import akka.pattern.Patterns;
import akka.util.Timeout;

import java.time.Duration;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.stop;
import static akka.japi.Util.classTag;
import static akka.pattern.Patterns.pipe;
import static io.akka.supervision.WorkerApi.*;

/**
 * Worker performs some work when it receives the Start message. It will
 * continuously notify the sender of the Start message of current Progress.
 * The Worker supervise the CounterService.
 */
public class Worker extends AbstractLoggingActor {
    final Timeout askTimeout = Timeout.create(Duration.ofSeconds(5));

    // The sender of the initial Start message will continuously be notified
    // about progress
    ActorRef progressListener;
    final ActorRef counterService = getContext().actorOf(
            Props.create(CounterService.class), "counter");
    final int totalCount = 51;

    // Stop the CounterService child if it throws ServiceUnavailable
    private static final SupervisorStrategy strategy =
            new OneForOneStrategy(DeciderBuilder.
                    match(CounterServiceApi.ServiceUnavailable.class, e -> stop()).
                    matchAny(o -> escalate()).build());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    @Override
    public Receive createReceive() {
        return LoggingReceive.create(receiveBuilder().
                matchEquals(Start, x -> progressListener == null, x -> {
                    progressListener = getSender();
                    getContext().getSystem().scheduler().schedule(
                            Duration.ZERO, Duration.ofSeconds(1L), getSelf(), Do,
                            getContext().dispatcher(), null
                    );
                }).
                matchEquals(Do, x -> {
                    counterService.tell(new CounterServiceApi.Increment(1), getSelf());
                    counterService.tell(new CounterServiceApi.Increment(1), getSelf());
                    counterService.tell(new CounterServiceApi.Increment(1), getSelf());
                    // Send current progress to the initial sender
                    pipe(Patterns.ask(counterService, CounterServiceApi.GetCurrentCount, askTimeout)
                            .mapTo(classTag(CounterServiceApi.CurrentCount.class))
                            .map(new Mapper<CounterServiceApi.CurrentCount, Progress>() {
                                public Progress apply(CounterServiceApi.CurrentCount c) {
                                    return new Progress(100.0 * c.count / totalCount);
                                }
                            }, getContext().dispatcher()), getContext().dispatcher())
                            .to(progressListener);
                }).build(), getContext());
    }
}