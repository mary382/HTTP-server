package com.bsuir.networks.httpserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ThreadHandler extends Thread {

    private static String NOT_FOUND = "NOT FOUND";

    //map for response
    public static final Map<String,String> CONTENT_TYPES = new HashMap<>() {{
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
        this.directoryPath=directoryPath;
    }

    @Override
    public void run() {
        try (
            var input = this.socket.getInputStream();
            var output = this.socket.getOutputStream()){
            var url = getRequestUrl(input);
            var filePath = Path.of(this.directoryPath + url);
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                var extension = getFileExtension(filePath);
                var type = CONTENT_TYPES.get(extension);
                var fileBytes = Files.readAllBytes(filePath);
                this.buildHeader(output, 200, "OK", type, fileBytes.length);
                output.write(fileBytes);
            }else {
                //отправляем 404
                var contentType = CONTENT_TYPES.get("text");
                this.buildHeader(output, 404, "Not Found", contentType, NOT_FOUND.length());
                output.write(NOT_FOUND.getBytes());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileExtension(Path path) {
        var name = path.getFileName().toString();
        //индекс символа, с которого начинается расширение(то есть последняя точка),
        // если такого нет, то возвращаем пустую стоку, иначе - вырезаем из строки расширение
        var extensionStart = name.lastIndexOf(".");
        return extensionStart == -1 ? "" : name.substring(extensionStart + 1);
    }

    private void buildHeader(OutputStream output, int statusCode, String statusText, String type, long lenght) {
        var ps = new PrintStream(output);
        ps.printf("HTTP/1.1 %s %s%n", statusCode, statusText);
        ps.printf("Content-Type: %s%n", type);
        ps.printf("Content-Length: %s%n%n", lenght);
    }

    private String getRequestUrl(InputStream inputStream) {
        var scanner = new Scanner(inputStream).useDelimiter("\r\n");
        var startingLine = scanner.next();
        // разделяю по пробелу и возвращаю второй элемент,
        // т к второй элемент url - GET /path HTTP/1.1
        return startingLine.split(" ")[1];
    }

} 
