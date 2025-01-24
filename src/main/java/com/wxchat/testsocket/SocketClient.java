package com.wxchat.testsocket;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * @description SocketClient
 * @author JIU-W
 * @date 2025-01-23
 * @version 1.0
 */
public class SocketClient {
    public static void main(String[] args) {
        Socket socket = null;
        try {
            //创建Socket对象，并指定服务器的IP地址和端口号
            socket = new Socket("127.0.0.1", 1024);

            //获取输出流，向服务器发送消息
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream);
            System.out.println("请输入内容");

            //创建线程，用于向服务器发送消息，监听键盘输入
            new Thread(() -> {
                while (true) {
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.nextLine();
                    try {
                        printWriter.println(input);
                        printWriter.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }

                }
            }).start();

            InputStream inputStream = socket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            new Thread(() -> {
                while (true) {
                    try {
                        String readData = bufferedReader.readLine();
                        System.out.println("收到服务端消息：" + readData);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
