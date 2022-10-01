package gosha.kalosha.balancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Balancer {

    private static final Logger logger = LoggerFactory.getLogger(Balancer.class);
    private final ServerSocket serverSocket;
    private final ServiceProvider serviceProvider;
    private final Context context;

    public Balancer(final Context context, final ServiceProvider serviceProvider) throws IOException {
        this.context = context;
        this.serverSocket = new ServerSocket(context.config.port);
        this.serviceProvider = serviceProvider;
    }

    public void start() {
        logger.info("Starting listening for connections");
        while (!Thread.interrupted()) {
            try {
                final var client = serverSocket.accept();
                context.executors.submit(() -> balanceRequest(client));
            } catch (IOException ex) {
                logger.warn("Error while listening: ", ex);
            }
        }
    }

    private void balanceRequest(final Socket client) {
        try {
            logger.info("Received connection from {}", client.getInetAddress());
            final var destinationService = serviceProvider.pickNext();
            logger.debug("Connecting to {}", destinationService.host);
            final var destination = new Socket(destinationService.host, destinationService.port);
            logger.info("Sending request from {} to {}", client.getInetAddress(), destination.getInetAddress());
            context.executors.submit(() -> writeToSocket(client, destination));
            context.executors.submit(() -> writeToSocket(destination, client));
        } catch (IOException ex) {
            logger.warn("Error while connection from {}", client.getInetAddress(), ex);
        }
    }

    private void writeToSocket(final Socket source, final Socket destination) {
        try {
            final var sourceInStream = new BufferedInputStream(source.getInputStream());
            final var destinationOutStream = new BufferedOutputStream(destination.getOutputStream());
            while (!Thread.interrupted()) {
                final var nextByte = sourceInStream.read();
                if (nextByte < 0) {
                    source.close();
                    destination.close();
                    return;
                }
                destinationOutStream.write(nextByte);
                destinationOutStream.flush();
            }
        } catch (IOException ex) {
            logger.warn(
                    "Connection was closed, source: {}, destination: {}",
                    source.getInetAddress(),
                    destination.getInetAddress());
        }
    }
}
