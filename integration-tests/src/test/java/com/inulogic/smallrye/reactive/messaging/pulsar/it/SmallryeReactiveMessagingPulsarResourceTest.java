package com.inulogic.smallrye.reactive.messaging.pulsar.it;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.post;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class SmallryeReactiveMessagingPulsarResourceTest {

    protected static final TypeRef<List<String>> TYPE_REF = new TypeRef<List<String>>() {
    };

    @Test
    public void test() {
        post("/pulsar/people");

        await().atMost(5, SECONDS)
                .untilAsserted(() -> Assertions.assertEquals(6, get("/pulsar/people").as(TYPE_REF).size()));
    }
}
