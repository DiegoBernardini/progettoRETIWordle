package Server;

import java.io.IOException;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerCtrlCHandler extends Thread {
    private int maxDelay;
    private ExecutorService pool;
    private ServerSocket serverSocket;
    private MulticastSocket ms;

    public ServerCtrlCHandler(int maxDelay, ExecutorService pool, ServerSocket serverSocket, MulticastSocket ms) {
        this.maxDelay = maxDelay;
        this.pool = pool;
        this.serverSocket = serverSocket;
        this.ms = ms;
    }

    public void run() {
        // Avvio la procedura di terminazione del server.
        println("Avvio terminazione...");
        // Chiudo la ServerSocket cosi' da non accettare piu' nuove richieste e la multicastSocket
        try { // se esiste e non è gia stata chiusa la chiudo
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                println("Listener Socket chiuso.");
            }
            if (ms != null && !ms.isClosed()) {
                ms.close();
                println("Multicast terminato.");
            }
        }
        catch (IOException e) {
            println("Errore nella chiusura del socket del server:");
            e.printStackTrace();
        }
        // Faccio terminare il pool di thread.
        pool.shutdown();
        try {
            if (!pool.awaitTermination(maxDelay, TimeUnit.MILLISECONDS)) pool.shutdownNow();
        } catch (InterruptedException e) {pool.shutdownNow();}
        println("Terminazione conclusa con successo!");
    }

    private void println(String s ){ System.out.println("[ServerCtrlCHandler] " + s);}
}

