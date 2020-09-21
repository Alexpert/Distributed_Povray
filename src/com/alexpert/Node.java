package com.alexpert;

import java.util.Vector;

public class Node extends Thread {
    String host;
    Vector<Task> tasks;

    Node(String host, Vector<Task> tasks) {
        this.host = host;
        this.tasks = tasks;
    }

    @Override
    public void run() {
        Task task;
        while (true) {
            if (!tasks.isEmpty()) {
                task = tasks.get(0);
                tasks.remove(0);
            } else {
                break;
            }
            task.setHost(host);
            task.run();
        }
    }
}
