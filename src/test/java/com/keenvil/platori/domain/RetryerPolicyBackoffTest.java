package com.keenvil.platori.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import feign.Request;
import feign.Request.HttpMethod;
import feign.RetryableException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class RetryerPolicyBackoffTest {

  private final List<Long> sleeps = new ArrayList<>();

  private RetryerPolicy policy(double random) {
    return new RetryerPolicy(3, 500, 2000, sleeps::add, () -> random);
  }

  static Request request(HttpMethod method) {
    return Request.create(method, "http://community-api/x", java.util.Map.of(), null,
        java.nio.charset.StandardCharsets.UTF_8, null);
  }

  private static RetryableException error(HttpMethod method, Throwable cause) {
    return new RetryableException(-1, "io", method, cause, (Long) null, request(method));
  }

  /** Simula la CircuitOpenException de los servicios (no depende de su clase). */
  static class CircuitOpenException extends IOException {
    CircuitOpenException() {
      super("circuit open");
    }
  }

  @Test
  public void exponentialBackoffWithEqualJitter() {
    RetryerPolicy min = policy(0.0);
    assertEquals(250, min.delay(1));
    assertEquals(500, min.delay(2));
    assertEquals(1000, min.delay(3));
    assertEquals(1000, min.delay(10)); // tope 2000 -> 1000-2000

    RetryerPolicy max = policy(0.999999);
    assertEquals(499, max.delay(1));
    assertEquals(999, max.delay(2));
    assertEquals(1999, max.delay(10));
  }

  @Test
  public void getIsRetriedWithGrowingWaits() {
    RetryerPolicy p = policy(0.0);
    RetryableException e = error(HttpMethod.GET, new SocketTimeoutException("Read timed out"));
    p.continueOrPropagate(e);
    p.continueOrPropagate(e);
    assertEquals(List.of(250L, 500L), sleeps);
    try {
      p.continueOrPropagate(e);
      fail("tercer intento tenia que propagar");
    } catch (RetryableException expected) {
      assertEquals(2, sleeps.size());
    }
  }

  @Test
  public void openCircuitIsNotRetried() {
    RetryerPolicy p = policy(0.0);
    try {
      p.continueOrPropagate(error(HttpMethod.GET, new CircuitOpenException()));
      fail("circuito abierto no se reintenta");
    } catch (RetryableException expected) {
      assertTrue(sleeps.isEmpty());
    }
  }

  @Test
  public void noRetryMarkerIsNotRetried() {
    class Marked extends IOException implements RetryerPolicy.NoRetry {
    }
    assertTrue(RetryerPolicy.isFastFail(error(HttpMethod.GET, new Marked())));
    assertFalse(RetryerPolicy.isFastFail(error(HttpMethod.GET, new IOException("x"))));
  }

  @Test
  public void writeAfterReadTimeoutIsNotRetried() {
    for (HttpMethod m : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)) {
      assertFalse(m.name(), RetryerPolicy.isSafeToRetry(error(m, new SocketTimeoutException("Read timed out"))));
    }
    RetryerPolicy p = policy(0.0);
    try {
      p.continueOrPropagate(error(HttpMethod.POST, new SocketTimeoutException("Read timed out")));
      fail("POST con timeout de lectura no se reintenta");
    } catch (RetryableException expected) {
      assertTrue(sleeps.isEmpty());
    }
  }

  @Test
  public void writeThatNeverLeftIsRetried() {
    assertTrue(RetryerPolicy.isSafeToRetry(error(HttpMethod.POST, new ConnectException("Connection refused"))));
    assertTrue(RetryerPolicy.isSafeToRetry(error(HttpMethod.PUT, new UnknownHostException("community-api"))));
    assertTrue(RetryerPolicy.isSafeToRetry(error(HttpMethod.POST, new SocketTimeoutException("Connect timed out"))));
    assertTrue(RetryerPolicy.isSafeToRetry(
        new RetryableException(503, "busy", HttpMethod.POST, (Throwable) null, 1000L, request(HttpMethod.POST))));
  }

  @Test
  public void cloneKeepsConfiguration() {
    RetryerPolicy p = new RetryerPolicy(2, 100, 100, sleeps::add, () -> 0.0);
    RetryerPolicy copy = (RetryerPolicy) p.clone();
    RetryableException e = error(HttpMethod.GET, new IOException("x"));
    copy.continueOrPropagate(e);
    try {
      copy.continueOrPropagate(e);
      fail("maxAttempts=2 tenia que propagar en el segundo");
    } catch (RetryableException expected) {
      assertEquals(List.of(50L), sleeps);
    }
  }
}
