package com.keenvil.platori.domain;

import feign.Request;
import feign.RetryableException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Politica de reintentos de los clientes Feign de Keenvil (16/11/2026).
 *
 * <ul>
 *   <li><b>Cuantos:</b> {@code keenvil.platori.feign.options.maxAttempts} intentos en total (default 3).</li>
 *   <li><b>Espera:</b> backoff exponencial con jitter ("equal jitter"): el intento n espera entre la mitad y el
 *       total de {@code min(maxBackoff, backoff * 2^(n-1))}. Con los defaults (500 ms, tope 2 s): 250-500 ms y
 *       despues 500-1000 ms. El jitter evita que todas las replicas reintenten en el mismo instante contra un
 *       servicio que se esta recuperando.</li>
 *   <li><b>Circuito abierto:</b> no se reintenta. Un circuit breaker abierto (o un bulkhead lleno) falla al instante
 *       a proposito; reintentarlo solo sumaba esperas (antes, ~1 s por llamada). Se reconoce por el marcador
 *       {@link NoRetry} o por el nombre de clase ({@code CircuitOpenException}, {@code CallNotPermittedException},
 *       {@code BulkheadFullException}) en la cadena de causas.</li>
 *   <li><b>Escrituras:</b> GET, HEAD, OPTIONS y TRACE se reintentan ante cualquier error de I/O. POST, PUT, PATCH y
 *       DELETE solo si el pedido seguro no llego al otro servicio (conexion rechazada, host desconocido, sin ruta,
 *       timeout de conexion) o si el servicio pidio reintentar ({@code Retry-After}). Un timeout de lectura en una
 *       escritura no se reintenta: el otro servicio pudo haberla procesado y el reintento la duplicaria.</li>
 * </ul>
 *
 * <p>Feign llama a {@link #clone()} en cada request: cada llamada arranca con su contador y conserva la
 * configuracion (antes {@code clone()} volvia a los defaults y la configuracion no se aplicaba).</p>
 *
 * <p>Los 5xx no pasan por aca: {@link PlatoriErrorDecoder} los convierte en excepciones no reintentables.</p>
 */
public class RetryerPolicy implements feign.Retryer {

  static final long DEFAULT_MAX_BACKOFF_MS = 2000L;

  private static final Set<String> FAST_FAIL_CLASSES = Set.of(
      "CircuitOpenException", "CallNotPermittedException", "BulkheadFullException");

  private static final Set<Request.HttpMethod> IDEMPOTENT = Set.of(
      Request.HttpMethod.GET, Request.HttpMethod.HEAD, Request.HttpMethod.OPTIONS, Request.HttpMethod.TRACE);

  /** Para tests: dormir sin esperar de verdad. */
  interface Sleeper {
    void sleep(long millis) throws InterruptedException;
  }

  private final int maxAttempts;
  private final long backoff;
  private final long maxBackoff;
  private final Sleeper sleeper;
  private final DoubleSupplier random;

  protected int attempt;

  public RetryerPolicy() {
    this(3, 500L);
  }

  public RetryerPolicy(int maxAttempts, long backoff) {
    this(maxAttempts, backoff, Math.max(backoff, DEFAULT_MAX_BACKOFF_MS));
  }

  public RetryerPolicy(int maxAttempts, long backoff, long maxBackoff) {
    this(maxAttempts, backoff, maxBackoff, Thread::sleep, () -> ThreadLocalRandom.current().nextDouble());
  }

  RetryerPolicy(int maxAttempts, long backoff, long maxBackoff, Sleeper sleeper, DoubleSupplier random) {
    this.maxAttempts = Math.max(1, maxAttempts);
    this.backoff = Math.max(0, backoff);
    this.maxBackoff = Math.max(this.backoff, maxBackoff);
    this.sleeper = sleeper;
    this.random = random;
    this.attempt = 1;
  }

  @Override
  public void continueOrPropagate(RetryableException e) {
    if (attempt >= maxAttempts || isFastFail(e) || !isSafeToRetry(e)) {
      throw e;
    }
    long delay = delay(attempt);
    attempt++;
    try {
      sleeper.sleep(delay);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
      throw e;
    }
  }

  /** Espera antes del intento {@code n + 1}: equal jitter sobre el backoff exponencial con tope. */
  long delay(int n) {
    double exp = backoff * Math.pow(2, Math.max(0, n - 1));
    long capped = (long) Math.min(maxBackoff, exp);
    long half = capped / 2;
    return half + (long) (random.getAsDouble() * (capped - half));
  }

  /** Circuito abierto o bulkhead lleno en la cadena de causas: fallar ya, sin reintentar. */
  static boolean isFastFail(Throwable e) {
    for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
      if (t instanceof NoRetry || FAST_FAIL_CLASSES.contains(t.getClass().getSimpleName())) {
        return true;
      }
    }
    return false;
  }

  /** Se puede reintentar sin riesgo de duplicar una escritura. */
  static boolean isSafeToRetry(RetryableException e) {
    Request.HttpMethod method = e.method();
    if (method == null && e.request() != null) {
      method = e.request().httpMethod();
    }
    if (method == null || IDEMPOTENT.contains(method)) {
      return true;
    }
    return e.retryAfter() != null || requestNotSent(e);
  }

  /** El error es de antes de mandar el pedido: el otro servicio seguro no lo recibio. */
  static boolean requestNotSent(Throwable e) {
    for (Throwable t = e.getCause(); t != null; t = t.getCause() == t ? null : t.getCause()) {
      if (t instanceof ConnectException || t instanceof UnknownHostException
          || t instanceof NoRouteToHostException || t instanceof java.net.http.HttpConnectTimeoutException) {
        return true;
      }
      if (t instanceof SocketTimeoutException && t.getMessage() != null
          && t.getMessage().toLowerCase(Locale.ROOT).contains("connect")) {
        return true;
      }
    }
    return false;
  }

  @Override
  public feign.Retryer clone() {
    return new RetryerPolicy(maxAttempts, backoff, maxBackoff, sleeper, random);
  }

  /**
   * Marcador para excepciones que significan "fallar ya" (circuito abierto, bulkhead lleno): el retryer no las
   * reintenta. Las {@code CircuitOpenException} de los servicios lo pueden implementar; mientras tanto se las
   * reconoce por nombre.
   */
  public interface NoRetry {
  }
}
