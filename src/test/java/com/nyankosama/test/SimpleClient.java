package com.nyankosama.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by hlr@superid.cn on 2014/10/28.
 */
public class SimpleClient {

    public static void main(String args[]) throws IOException {
        new SimpleClient().testClient();
    }

    public void testClient() throws IOException {
        String ip = "127.0.0.1";
        int requestNum = 10000;
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(ip, 9123));
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        long begin = System.currentTimeMillis();
        int num = requestNum;
        for (int i = 0; i < num; i++) {
            writer.println("hello world!");
            writer.flush();
            reader.readLine();
        }
        socket.close();
        long end = System.currentTimeMillis();
        System.out.println("cost time:" + (end - begin) + " ms, qps:" + ((double) num / (end - begin) * 1000));
    }
}
