package com.keenvil.platori.domain;

import static feign.Request.HttpMethod.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import feign.Request;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.easymock.TestSubject;
import org.junit.Test;

import com.keenvil.cork.error.KeenvilApiError;
import com.keenvil.cork.error.KeenvilApiException.Authorization;
import com.keenvil.cork.error.KeenvilApiException.Forbidden;
import com.keenvil.cork.error.KeenvilApiException.InvalidResourceState;
import com.keenvil.cork.error.KeenvilApiException.ResourceNotFound;

import feign.Response;
import feign.Response.Body;

public class PlatoriErrorDecoderTest {

  @TestSubject
  private PlatoriErrorDecoder decoder = new PlatoriErrorDecoder();

  private class MockBody implements Body {

    @Override
    public void close() throws IOException {
    }

    @Override
    public Integer length() {
      return null;
    }

    @Override
    public boolean isRepeatable() {
      return false;
    }

    @Override
    public InputStream asInputStream() throws IOException {
      return null;
    }

    @Override
    public Reader asReader() throws IOException {
      return null;
    }

    @Override
    public Reader asReader(Charset charset) throws IOException {
      return null;
    }

    @Override
    public String toString() {
      return "data";
    }
  }

  private Body mockBody = new MockBody();

  @Test
  @SuppressWarnings("unchecked")
  public void decodeUnauthorized() throws Exception {
    Map<String, Collection<String>> map = Collections.EMPTY_MAP;
    Request request = Request.create(GET, "uri", map, Request.Body.empty());
    Response response = Response.builder().body(mockBody).headers(map).status(401).reason("").request(request).build();
    
    try {
      throw decoder.decode("key", response);
    } catch (Authorization exception) {
      assertThat(exception.getMessage(),
          is("Authorization error. Calling method key with status code 401 and"
              + " response data."));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void decodForbidden() throws Exception {
    Map<String, Collection<String>> map = Collections.EMPTY_MAP;
    Request request = Request.create(GET, "uri", map, Request.Body.empty());
    Response response = Response.builder().body(mockBody).headers(map).status(403).reason("").request(request).build();
    
    try {
      throw decoder.decode("key", response);
    } catch (Forbidden exception) {
      assertThat(exception.getMessage(),
          is("Can not grant access to the requested resource."
              + " Calling method key with status code 403 and response data."));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void decodForbidden_extractCode() throws Exception {
    Map<String, Collection<String>> map = Collections.EMPTY_MAP;
    Request request = Request.create(GET, "uri", map, Request.Body.empty());
    Response response = Response.builder().body("\"Can not grant access to the requested resource. Calling method SecurityApiClient#canAccessWithId(Object,String) with status code 403 and response [{\\\\\\\"httpStatus\\\\\\\":403,\\\\\\\"code\\\\\\\":\\\\\\\"access.not.valid\\\\\\\",\\\\\\\"title\\\\\\\":\\\\\\\"Forbidden\\\\\\\",\\\\\\\"detail\\\\\\\":\\\\\\\"Invalid PIN [1b23c263565dc9d5f7187d09fe7a1eae] for account [352] in community [santacatalina]\\\\\\\",\\\\\\\"source\\\\\\\":\\\\\\\"com.keenvil.security.controller.ResidentCommunityAccessController:authorize:339(ResidentCommunityAccessController.java) com.keenvil.security.controller.ResidentCommunityAccessController:validateByPersonalId:257(ResidentCommunityAccessController.java) com.keenvil.security.controller.ResidentCommunityAccessController$$FastClassBySpringCGLIB$$d2c48ed0:invoke:-1(<generated>) org.springframework.cglib.proxy.MethodProxy:invoke:218(MethodProxy.java) org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation:invokeJoinpoint:749(CglibAopProxy.java) org.springframework.aop.framework.ReflectiveMethodInvocation:proceed:163(ReflectiveMethodInvocation.java) org.springframework.transaction.interceptor.TransactionAspectSupport:invokeWithinTransaction:294(TransactionAspectSupport.java) org.springframework.transaction.interceptor.TransactionInterceptor:invoke:98(TransactionInterceptor.java) org.springframework.aop.framework.ReflectiveMethodInvocation:proceed:186(ReflectiveMethodInvocation.java) org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor:intercept:688(CglibAopProxy.java) \\\\\\\",\\\\\\\"module\\\\\\\":\\\\\\\"keenvil/security\\\\\\\",\\\\\\\"uri\\\\\\\":\\\\\\\"/security/c/santacatalina/access/residents\\\\\\\",\\\\\\\"httpMethod\\\\\\\":\\\\\\\"POST\\\\\\\",\\\\\\\"hostName\\\\\\\":\\\\\\\"localhost\\\\\\\",\\\\\\\"localHostName\\\\\\\":\\\\\\\"127.0.0.1\\\\\\\"}].\"", Charset.defaultCharset()).headers(map).status(403).reason("").request(request).build();

    try {
      throw decoder.decode("key", response);
    } catch (Forbidden exception) {
      assertThat(exception.getCode(),
          is("access.not.valid"));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void decode404() throws Exception {
    Map<String, Collection<String>> map = Collections.EMPTY_MAP;
    Request request = Request.create(GET, "uri", map, Request.Body.empty());
    Response response = Response.builder().body(mockBody).headers(map).status(404).reason("").request(request).build();
    try {
      throw decoder.decode("key", response);
    } catch (ResourceNotFound exception) {
      assertThat(exception.getMessage(),
          is("Resource not found."
              + " Calling method key with status code 404 and response data."));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void decodeConflict() throws Exception {
    Map<String, Collection<String>> map = Collections.EMPTY_MAP;
    Request request = Request.create(GET, "uri", map, Request.Body.empty());
    Response response = Response.builder().body(mockBody).headers(map).status(409).reason("").request(request).build();
    try {
      throw decoder.decode("key", response);
    } catch (InvalidResourceState exception) {
      assertThat(exception.getMessage(),
          is("There is a conflict with the current state of the target"
              + " resource. Calling method key with status code 409 and"
              + " response data."));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void decodeUnprocessableEntity() throws Exception {
    mockBody = new MockBody() {
      @Override
      public InputStream asInputStream() throws IOException {
        String json = "["
            + "{"
            +  "\"httpStatus\": \"404\","
            +  "\"code\": \"entityNotFound\","
            +  "\"title\": \"Entity Not Found\","
            +  "\"detail\": \"Interest Running Not Found.\","
            +  "\"source\": \"com.keenvil.ResidentsController\","
            +  "\"module\": \"security-api\","
            +  "\"uri\": \"/security/residents/1/interests\","
            +  "\"httpMethod\": \"PUT\","
            +  "\"hostName\": \"api.qa.myaws.io\","
            +  "\"localHostName\": \"172.17.0.5\""
            + "}"
            + "]";
        return new ByteArrayInputStream(json.getBytes());
      }
    };
    Map<String, Collection<String>> map = Collections.EMPTY_MAP;
    Request request = Request.create(GET, "uri", map, Request.Body.empty());
    Response response = Response.builder().body(mockBody).headers(map).status(422).reason("").request(request).build();
    try {
      throw decoder.decode("key", response);
    } catch (InvalidResourceState exception) {
      assertThat(exception.getErrors().size(), is(1));
      KeenvilApiError error = exception.getErrors().get(0);
      assertThat(error.getHttpStatus(), is(404));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void decodeUnknown() throws Exception {
    Map<String, Collection<String>> map = Collections.EMPTY_MAP;
    Request request = Request.create(GET, "uri", map, Request.Body.empty());
    Response response = Response.builder().body(mockBody).headers(map).status(999).reason("").request(request).build();
    try {
      throw decoder.decode("key", response);
    } catch (RuntimeException exception) {
      assertThat(exception.getMessage(),
          is("Unknown status code."
              + " Calling method key with status code 999 and response data."));
    }
  }
  
  @Test
  @SuppressWarnings("unchecked")
  public void decodeNullBody() throws Exception {
    Map<String, Collection<String>> map = Collections.EMPTY_MAP;
    Request request = Request.create(GET, "uri", map, Request.Body.empty());
    Response response = Response.builder().body((Body) null).headers(map).status(999).reason("").request(request).build();

    try {
      throw decoder.decode("key", response);
    } catch (RuntimeException exception) {
      assertThat(exception.getMessage(),
          is("Unknown status code."
              + " Calling method key with status code 999 and response null."));
    }
  }
}
