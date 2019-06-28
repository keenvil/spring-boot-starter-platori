package com.keenvil.platori.domain;

import feign.RetryableException;
import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;

public class RetryerPolicy implements Retryer {

  @Value("${keenvil.platori.feign.options.maxAttempts:3}")
  private int maxAttempts;
  @Value("${keenvil.platori.feign.options.backoff:500}")
  private long backoff;

  protected int attempt;

  public RetryerPolicy() {
    this(3, 500L);
    this.attempt = 1;
  }

  public RetryerPolicy(int maxAttempts, long backoff) {
    this.maxAttempts = maxAttempts;
    this.backoff = backoff;
    this.attempt = 1;
  }

  public void continueOrPropagate(RetryableException e) {
    if (attempt++ >= maxAttempts) {
      throw e;
    }

    try {
      Thread.sleep(backoff);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public Retryer clone() {
    return new RetryerPolicy();
  }
}