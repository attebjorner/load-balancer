package gosha.kalosha.balancer;

import io.netty.handler.timeout.ReadTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

import java.util.List;

import static java.time.Duration.ofMillis;

public class HealthCheck {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheck.class);
    private final HttpClient client;
    private final Config config;
    private final List<Service> services;

    public HealthCheck(final HttpClient client, final Context context) {
        this.client = client;
        this.config = context.config;
        this.services = context.services;
    }

    public void start() {
        logger.info("Started health checking services");
        Flux.create(this::loadServices)
                .delayElements(ofMillis(config.delay))
                .subscribe(this::checkHealth);
    }

    private void loadServices(final FluxSink<List<Service>> sink) {
        while (!Thread.interrupted()) {
            sink.next(services);
        }
    }

    private void checkHealth(final List<Service> services) {
        for (final var service : services) {
            logger.info("Sending health check to {}:{}", service.host, service.port);
            client.responseTimeout(ofMillis(config.timeout))
                    .get()
                    .uri(service.healthUrl)
                    .response()
                    .doOnError(ex -> handleTimeout(service, ex))
                    .subscribe(response -> checkStatus(service, response));
        }
    }

    private void checkStatus(final Service service, final HttpClientResponse response) {
        final var statusCode = response.status().code();
        logger.info("Got code {} from {}", statusCode, service.host);
        if (isNotOk(statusCode)) {
            service.countFail();
        } else if (!service.isAlive) {
            service.tryReset();
        }
    }

    private void handleTimeout(final Service service, final Throwable throwable) {
        if (throwable instanceof ReadTimeoutException) {
            service.countFail();
        } else {
            logger.warn("Error occurred while sending health check to {}: ", service.host, throwable);
        }
    }

    private boolean isNotOk(final int statusCode) {
        return statusCode / 100 == 5 || config.extraCodesToFailOn.contains(statusCode);
    }
}
