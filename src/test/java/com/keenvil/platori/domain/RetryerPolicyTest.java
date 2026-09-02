package com.keenvil.platori.domain;

import static org.junit.Assert.*;

import feign.Request;
import feign.Request.HttpMethod;
import feign.RetryableException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RetryerPolicyTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void create() throws Exception {

    RetryableException e = new RetryableException(500, "", HttpMethod.GET, (Long) null, (Request) null);

    RetryerPolicy retryerPolicy = new RetryerPolicy();

    assertEquals(1, retryerPolicy.attempt);

    retryerPolicy.continueOrPropagate(e);
    assertEquals(2, retryerPolicy.attempt);

    retryerPolicy.continueOrPropagate(e);
    assertEquals(3, retryerPolicy.attempt);

  }

  @Test(expected = RetryableException.class)
  public void create_exception() throws Exception {

    RetryableException e = new RetryableException(500, "", HttpMethod.GET, (Long) null, (Request) null);

    RetryerPolicy retryerPolicy = new RetryerPolicy();

    assertEquals(1, retryerPolicy.attempt);

    retryerPolicy.continueOrPropagate(e);
    assertEquals(2, retryerPolicy.attempt);

    retryerPolicy.continueOrPropagate(e);
    assertEquals(3, retryerPolicy.attempt);

    retryerPolicy.continueOrPropagate(e);
  }


}