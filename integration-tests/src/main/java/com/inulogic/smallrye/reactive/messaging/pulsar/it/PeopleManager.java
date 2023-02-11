package com.inulogic.smallrye.reactive.messaging.pulsar.it;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PeopleManager {

    @Inject
    @Channel("people-out")
    Emitter<String> emitter;

    private final Logger log = Logger.getLogger(PeopleManager.class);

    private final List<String> list = new CopyOnWriteArrayList<>();

    @Incoming("people-in")
    public CompletionStage<Void> consume(Message<String> message) {
        String name = message.getPayload();
        log.info("Receiving person " + name);
        list.add(name);
        return message.ack();
    }

    public List<String> getPeople() {
        log.info("Returning people " + list);
        return list;
    }

    public void seedPeople() {
        log.info("Seeding");
        Stream
                .of("bob",
                        "alice",
                        "tom",
                        "jerry",
                        "anna",
                        "ken")
                .forEach(s -> emitter.send(s));
    }
}