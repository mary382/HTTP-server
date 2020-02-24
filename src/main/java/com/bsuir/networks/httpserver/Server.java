package com.bsuir.networks.httpserver;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * The server class which is an entry point of the program.
 */
public class Server {

    private int port;

    private String directoryFrom;

    private Server(int port, String directoryFrom) {
        this.port = port;
        this.directoryFrom = directoryFrom;
    }

    /**
     * Starts the server with the specified {@link Server#port} and in the {@link Server#directoryFrom}.
     * When server is started it waits for a new connection. Once it happens control is transferred to the {@link ThreadHandler}.
     */
    private void startServer() {
        try (var server = new ServerSocket(this.port)) {
            while (true) {
                //Returns the address of the endpoint this socket is bound to.
                var socket = server.accept();

                var thread = new ThreadHandler(socket, this.directoryFrom);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Entry point of the program.
     * Required arguments (and its order):
     * 1 - port on which server will be started. Should be an integer value.
     * 2 - folder name ???
     * @param args - an array of cmd arguments.
     */
    public static void main(String[] args) {
        var port = Integer.parseInt(args[0]);
        var directory = args[1];
        new Server(port, directory).startServer();
    }

}
