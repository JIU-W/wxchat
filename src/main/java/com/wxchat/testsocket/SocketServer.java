package com.wxchat.testsocket;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * @description SocketServer
 * @author JIU-W
 * @date 2025-01-23
 * @version 1.0
 */
public class SocketServer {
    public static void main(String[] args) {
        ServerSocket server = null;
        Map<String, Socket> CLENT_MAP = new HashMap<>();

        try {
            //创建服务端socket
            server = new ServerSocket(1024);
            System.out.println("服务启动,等待客户端连接");
            while (true) {
                //获取客户端socket
                Socket socket = server.accept();
                //获取客户端ip
                String ip = socket.getInetAddress().getHostAddress();
                System.out.println("有客户端连接ip:" + ip + "端口:" + socket.getPort());
                String clientKey = ip + socket.getPort();
                CLENT_MAP.put(clientKey, socket);

                //创建线程处理客户端消息，监听客户端消息
                new Thread(() -> {
                    while (true) {//循环监听客户端消息
                        try {
                            InputStream inputStream = socket.getInputStream();
                            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf8");
                            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                            String readData = bufferedReader.readLine();
                            System.out.println("收到客户端消息->" + readData);

                            //发送消息给客户端
                            /*OutputStream outputStream = socket.getOutputStream();
                            PrintWriter printWriter = new PrintWriter(outputStream);
                            printWriter.println("我是服务端，我已经收到了你发的消息消息:" + readData);
                            printWriter.flush();*/

                            //给所有客户端发送消息
                            CLENT_MAP.forEach((k, v) -> {
                                try {
                                    OutputStream outputStream = v.getOutputStream();
                                    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, "utf8"));
                                    printWriter.println(socket.getPort() + ":" + readData);
                                    printWriter.flush();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });

                        } catch (IOException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
