package com.bsuir.networks.httpserver;

import com.bsuir.networks.httpserver.exception.BadRequestException;
import com.bsuir.networks.httpserver.exception.NotFoundException;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ThreadHandler extends Thread {

    private static final List<String> REQUEST_METHODS = List.of("GET");

    //map for response
    private static final Map<String,String> CONTENT_TYPES = new HashMap<>() {{
        put("jpg", "image/jpeg");
        put("html", "text/html");
        put("json", "application/json");
        put("txt", "text/plain");
        // если пустой - то по дефолту текст
        put("", "text/plain");
    }};

    private Socket socket;

    private String directoryPath;

    ThreadHandler(Socket socket, String directoryPath) {
        this.socket = socket;
        this.directoryPath = directoryPath;
    }

    @Override
    public void run() {
//        System.out.println("New connection established");
//        System.out.println(String.format("Remote address: %s", socket.getRemoteSocketAddress()));

        InputStream input = null;
        OutputStream output = null;

        try {
            input = this.socket.getInputStream();
            output = this.socket.getOutputStream();

            var requestLine = this.getRequestLine(input);
            if (!validateRequest(requestLine)) {
                throw new BadRequestException(String.format("Method is not supported! Available methods: %s", String.join(", ", REQUEST_METHODS)));
            }

            var url = this.getRequestUrl(requestLine);
            var filePath = Path.of(this.directoryPath + url);
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                var extension = getFileExtension(filePath);
                var type = CONTENT_TYPES.get(extension);
                var fileBytes = Files.readAllBytes(filePath);
                this.sendResponse(output, 200, "OK", type, fileBytes);
            } else {
                throw new NotFoundException("Unable to access specified resource");
            }

        } catch (NotFoundException e) {
            var contentType = CONTENT_TYPES.get("txt");
            this.sendResponse(output, 404, "Not Found", contentType, e.getMessage().getBytes());
        } catch (Exception e) {
            var contentType = CONTENT_TYPES.get("txt");
            this.sendResponse(output, 400, "Bad Request", contentType, e.getMessage().getBytes());
        } finally {
            this.closeStream(input);
            this.closeStream(output);
        }
    }

    private String getRequestLine(InputStream inputStream) {
        var scanner = new Scanner(inputStream).useDelimiter("\r\n");
        return scanner.next();
    }

    private boolean validateRequest(String requestLine) {
        var httpMethod = requestLine.split(" ")[0];
        return REQUEST_METHODS.contains(httpMethod);
    }

    private String getRequestUrl(String requestLine) {
        return requestLine.split(" ")[1];
    }

    private String getFileExtension(Path path) {
        var name = path.getFileName().toString();
        //индекс символа, с которого начинается расширение(то есть последняя точка),
        // если такого нет, то возвращаем пустую стоку, иначе - вырезаем из строки расширение
        var extensionStart = name.lastIndexOf(".");
        return extensionStart == -1 ? "" : name.substring(extensionStart + 1);
    }

    private void sendHeader(OutputStream output, int statusCode, String statusText, String type, long length) {
        var ps = new PrintStream(output);
        ps.printf("HTTP/1.1 %s %s%n", statusCode, statusText);
        ps.printf("Content-Type: %s%n", type);
        ps.printf("Content-Length: %s%n%n", length);
    }

    private void sendResponse(OutputStream output, int statusCode, String statusText, String contentType, byte[] responseBody) {
        this.sendHeader(output, statusCode, statusText, contentType, responseBody.length);
        try {
            output.write(responseBody);
        } catch (IOException e) {
            System.err.println("An error occurred while sending response!");
            System.err.println(e.getMessage());
        }
    }

    private void closeStream(Closeable stream) {
        try {
            if (stream != null) {
                stream.close();
            }
        } catch (IOException e) {
            System.err.println("An error occurred while closing connection!");
            System.err.println(e.getMessage());
        }
    }

}
