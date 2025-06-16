package br.uerj.graduacao.tracker;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TrackerMain {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java -jar tracker.jar <nome-do-arquivo>");
            return;
        }
        final String fileName = args[0];
        final int trackerPort = 7000;

        Tracker tracker = new Tracker(fileName);
        tracker.start(trackerPort);
        System.out.println("Tracker iniciado para o arquivo " + fileName + " na porta " + trackerPort);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(tracker::displayPeerStatus, 0, 5, TimeUnit.SECONDS);
    }
}