package com.keenvil.platori.domain;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keenvil.cork.error.KeenvilApiError;
import com.keenvil.cork.error.KeenvilApiException;
import com.keenvil.cork.error.KeenvilApiException.Authorization;
import com.keenvil.cork.error.KeenvilApiException.Forbidden;
import com.keenvil.cork.error.KeenvilApiException.InvalidResourceState;
import com.keenvil.cork.error.KeenvilApiException.ResourceNotFound;

import feign.Response;
import feign.Response.Body;
import feign.codec.ErrorDecoder;

/**
 * Platori Error decoder for Keenvil Feign clients.
 * <p>
 * <p>Decodes {@link HttpStatus} error codes raised by internal Keenvil api
 * calls and returns its Keenvil api Exception counterpart.</p>
 * <p>
 * <p>Current handled {@link HttpStatus} are:
 * <ul>
 * <li>{@link HttpStatus#UNAUTHORIZED},</li>
 * <li>{@link HttpStatus#FORBIDDEN},</li>
 * <li>{@link HttpStatus#NOT_FOUND},</li>
 * <li>{@link HttpStatus#CONFLICT},</li>
 * <li>{@link HttpStatus#PRECONDITION_FAILED},</li>
 * <li>{@link HttpStatus#UNPROCESSABLE_ENTITY}.</li>
 * </ul>
 * Any other status will return a {@link RuntimeException}.
 * </p>
 */
public class PlatoriErrorDecoder implements ErrorDecoder {

  private static Logger log = getLogger(PlatoriErrorDecoder.class);

  @Override
  public Exception decode(String methodKey, Response response) {
    int status = response.status();
    Body body = response.body();
    String code = "";

    if (body != null) {
      Pattern pattern = Pattern.compile("(?:\"code\":\")(.*?)(?:\")");
      String bodyString = convert(response.body());
      Matcher matcher = pattern.matcher(bodyString.replace("\\", ""));
      if (matcher.find()) {
        code = matcher.group(1);
      }
    }

    String message =
        format("Calling method %s with status code %s and response %s.",
            methodKey,
            status,
            body);

    log.error(message);
    
    KeenvilApiException exception = null;
    if (status == HttpStatus.UNAUTHORIZED.value()) {
      if (StringUtils.isEmpty(code)) {
        code = "unauthorized";
      }
      exception = new Authorization("Authorization error. " + message, code);
    } else if (status == HttpStatus.FORBIDDEN.value()) {
      if (StringUtils.isEmpty(code)) {
        code = "forbidden";
      }
      exception = new Forbidden("Can not grant access to the requested"
          + " resource. " + message, code);
    } else if (status == HttpStatus.NOT_FOUND.value()) {
      if (StringUtils.isEmpty(code)) {
        code = "resourceNotFound";
      }
      exception = new ResourceNotFound("Resource not found. " + message, code);
    } else if (status == HttpStatus.CONFLICT.value()) {
      if (StringUtils.isEmpty(code)) {
        code = "invalidResourceState";
      }
      exception = new InvalidResourceState("There is a conflict with the"
          + " current state of the target resource. " + message, code);
    } else if (status == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
      exception = new InvalidResourceState(getErrors(response));
    } else if (status == HttpStatus.PRECONDITION_FAILED.value()) {
      if (StringUtils.isEmpty(code)) {
        code = "invalidResourceState";
      }
      exception = new InvalidResourceState("Invalid Resource State. " + message, code);
    } else {
      return new RuntimeException("Unknown status code. " + message);
    }
    return exception;
  }

  private List<KeenvilApiError> getErrors(Response response) {
    List<KeenvilApiError> errors = new ArrayList<>();
    ObjectMapper mapper = new ObjectMapper();
    InputStream body = null;
    try {
      body = response.body().asInputStream();
      errors = Arrays.asList(mapper.readValue(body, KeenvilApiError[].class));
    } catch (JsonParseException | JsonMappingException exception) {
      log.error("There was a problem reading Platform Errors {}.",
          exception.getMessage());
      throw new RuntimeException(exception);
    } catch (IOException exception) {
      log.error("There was a problem reading response body {}.",
          exception.getMessage());
      throw new RuntimeException(exception);
    } finally {
      try {
        if (body != null) {
          body.close();
        }
      } catch (IOException exception) {
        log.error("There was a problem closing body input stream {}.",
            exception.getMessage());
        throw new RuntimeException(exception);
      }
    }
    return errors;
  }

  private String convert(Response.Body body) {
    try {
      InputStream inputStream = body.asInputStream();
      if (inputStream != null) {
        return IOUtils.toString(inputStream);
      }
    } catch (IOException e) {
      log.error("the error can't not be deserialized");
    }
    return "";
  }
}
