package br.gov.bnb.openbanking.policy.ipratelimit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    /**
     * Main method to start the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

        //final Main main = new Main();
        //launch(main);
    }
    /*
    static void launch(final Main main) {
        try {
            main.bind("logging-handler", new LoggingHandler());
            main.configure().addRoutesBuilder(new ProxyRoute());
            main.run();
        } catch (final Exception e) {
            throw new ExceptionInInitializerError(e);
        } finally {
            main.stop();
        }
    } */

}