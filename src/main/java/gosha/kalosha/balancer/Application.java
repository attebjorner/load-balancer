package gosha.kalosha.balancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Hooks;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private Context context;

    public static void main(String[] args) throws IOException {
        final var application = new Application();
        Runtime.getRuntime().addShutdownHook(new Thread(application::stop));
        application.start();
    }

    public void start() throws IOException {
        configureReactorErrorCallback();
        this.context = new Context(Config.load());
        final var serviceProvider = new ServiceProvider(context.services);
        final var balancer = new Balancer(context, serviceProvider);
        final var healthCheck = new HealthCheck(HttpClient.create(), context);
        context.executors.submit(balancer::start);
        context.executors.submit(healthCheck::start);
        logger.info("Application started on port {}", context.config.port);
    }

    public void stop() {
        logger.info("Shutting down the application");
        if (context != null) {
            context.executors.shutdown();
        }
    }

    private void configureReactorErrorCallback() {
        Hooks.onErrorDropped(ex -> logger.trace("There was an error, but it is already handled", ex));
    }
}
