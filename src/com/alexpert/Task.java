package com.alexpert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Task implements Runnable {
    String host;
    String source;
    String srcFolder, dstFolder;
    int startRow;
    int nRows;
    int nPart;

    Task(String source, String srcFolder, String dstFolder, int startRow, int nRows, int nPart) {
        this.source = source;
        this.srcFolder = srcFolder;
        this.dstFolder = dstFolder;
        this.startRow = startRow;
        this.nRows = nRows;
        this.nPart = nPart;
    }

    @Override
    public void run() {
        String cmd = "ssh -t " + host + " povray " + srcFolder + "/" + source + ".pov" +
                " -D +SR" + (startRow+1) + " +ER" + (startRow + nRows) + " +O" + dstFolder + "/" +
                source + ".part" + nPart + " +FN +RF" + srcFolder +
                "/" + source + ".rad +RFI +B +MB2 +A";

        System.out.println(cmd);


        Runtime rt = Runtime.getRuntime();
        try {
            Process pr = rt.exec(cmd);

            InputStream is = pr.getErrorStream();
            InputStreamReader inputStreamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(inputStreamReader);
            while (pr.isAlive()) {
                //System.out.println(br.readLine());
            }
            System.out.println("\n" + host + " part " + nPart + " => " + pr.exitValue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setHost(String host) {
        this.host = host;
    }
}
