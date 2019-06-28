package com.keenvil.platori;

import com.keenvil.platori.domain.RetryerPolicy;
import feign.RetryableException;
import feign.Retryer;
import feign.Retryer.Default;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.keenvil.platori.domain.PlatoriErrorDecoder;

import feign.Logger;
import feign.Logger.Level;
import feign.Request;
import feign.codec.ErrorDecoder;

/**
 * Feign Client configuration exposed by Platori.
 *
 * <p>Platori exposes:
 * <ul>
 * <li>Feign log level, default is BASIC.</li>
 * <li>Connection and Read timeouts in milis, default is 5 seconds.</li>
 * <li>Error decoder.</li>
 * </ul>
 * </p>
 */
@Configuration
public class PlatoriFeignConfiguration {

  @Value("${keenvil.platori.feign.options.logger-level:BASIC}")
  private Level level;

  @Value("${keenvil.platori.feign.options.connection-timeout:5000}")
  private int connectionTimeout;

  @Value("${keenvil.platori.feign.options.read-timeout:5000}")
  private int readTimeout;

  @Value("${keenvil.platori.feign.options.maxAttempts:3}")
  private int maxAttempts;
  @Value("${keenvil.platori.feign.options.backoff:500}")
  private long backoff;

  @Bean
  public Logger.Level feignLoggerLevel() {
    return level;
  }

  @Bean
  public Request.Options options() {
    return new Request.Options(connectionTimeout, readTimeout);
  }

  @Bean
  public ErrorDecoder platoriErrorDecoder() {
    return new PlatoriErrorDecoder();
  }

  @Bean
  public Retryer retryer() {
    return new RetryerPolicy(maxAttempts, backoff);
  }
}
