package com.keenvil.platori.domain;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import feign.Request;
import feign.Request.HttpMethod;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class PlatoriErrorDecoderLogLevelTest {

  private final PlatoriErrorDecoder decoder = new PlatoriErrorDecoder();

  private Response response(int status) {
    Request request = Request.create(HttpMethod.GET, "uri", Collections.emptyMap(),
        Request.Body.empty(), null);
    return Response.builder().status(status).reason("").headers(Collections.emptyMap())
        .body("{\"code\":\"x\"}", StandardCharsets.UTF_8).request(request).build();
  }

  @Test
  void logsClientErrorsAsWarnAndServerErrorsAsError() {
    Logger logger = (Logger) LoggerFactory.getLogger(PlatoriErrorDecoder.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      decoder.decode("key", response(404));
      decoder.decode("key", response(409));
      decoder.decode("key", response(503));

      assertThat(appender.list).extracting(ILoggingEvent::getLevel)
          .containsExactly(Level.WARN, Level.WARN, Level.ERROR);
    } finally {
      logger.detachAppender(appender);
    }
  }
}
