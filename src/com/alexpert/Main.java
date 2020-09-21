package com.alexpert;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        String source = "slime";
        int width = 800, height = 600;//TODO
        Properties prop = new Properties();

        try {
            prop = readPropertiesFile("distrib.properties");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        int nSplits = Integer.parseInt(prop.getProperty("nSplits"));

        ArrayList<String> hosts = new ArrayList<>();
        Collections.addAll(hosts, prop.getProperty("hosts").split(" "));

        Vector<Task> tasks = new Vector<>();
        int nRows = height / nSplits;
        for (int i = 0; i < nSplits; i++)
            tasks.add(new Task(source, prop.getProperty("srcFolder"), prop.getProperty("dstFolder"),
                    i * nRows, nRows, i));

        ArrayList<Node> nodes = hosts.stream().map((e) -> new Node(e, tasks)).collect(Collectors
                .toCollection(ArrayList::new));

        //Pretrace
        String cmd = "ssh -t " + prop.getProperty("radiosityHost") + " povray -D " + prop.getProperty("srcFolder") +
                "/" + source + ".pov +RF" + prop.getProperty("srcFolder") + "/" + source + ".rad +RFO -RVP " +
                "-H" + height + " -W" + width + " +B +MB2 -O" + prop.getProperty("imgFolder") + "/" + source + ".rad.png";

        System.out.println(cmd);


        Runtime rt = Runtime.getRuntime();
        try {
            Process pr = rt.exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(pr.getErrorStream(), StandardCharsets.UTF_8));

            while (!br.readLine().startsWith("==== [Rendering...] ===="));
            System.out.println("Pretracing 1/2");
            while (Integer.parseInt(br.readLine().split(" ")[1]) < width * height /2);
            System.out.println("Pretracing 2/2");
            while (Integer.parseInt(br.readLine().split(" ")[1]) > width * height /2);
            System.out.println("End Pretracing");

            Process pr2 = rt.exec("ssh " + prop.getProperty("radiosityHost") + " ps -ux |grep povray |grep " + source + " |cut -d\" \" -f2");
            BufferedReader br2 = new BufferedReader(new InputStreamReader(pr2.getInputStream(), StandardCharsets.UTF_8));

            //FORGIVE ME MY LORD FOR I HAVE SINNED
            rt.exec("ssh " + prop.getProperty("radiosityHost") + " kill -9 " + br2.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Render
        for(Node n: nodes)
            n.start();

        for (Node n: nodes) {
            try {
                n.join();
                System.out.println(n.host + " finished");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //Fusion
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();

        try {
            for (int i = 0; i < nSplits; i++) {
                BufferedImage currImg = ImageIO.read(new File(prop.getProperty("dstFolder") + "/" + source + ".part" + i + ".png"))
                        .getSubimage(0, i * height / nSplits, width, height / nSplits);
                g.drawImage(currImg, 0, i * height / nSplits, null);
            }
            ImageIO.write(img, "PNG", new File(prop.getProperty("imgFolder") , source + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Properties readPropertiesFile(String fileName) throws IOException {
        FileInputStream fis = null;
        Properties prop = null;
        try {
            fis = new FileInputStream(fileName);
            prop = new Properties();
            prop.load(fis);
        } catch(FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        } finally {
            fis.close();
        }
        return prop;
    }
}
