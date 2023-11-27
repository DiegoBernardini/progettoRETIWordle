package Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MulticastWaitingThread implements Runnable {
    private MulticastSocket multicastSocket;
    private List<String> notifiche;
    private String myUsername = null;

    /**
     * @param multicastIP
     * @param multicastPort
     * @throws IOException se l'indirizzo multicast e' errato
     */
    public MulticastWaitingThread(ArrayList<String> notifiche, String multicastIP , int multicastPort) throws IOException {
        this.notifiche = notifiche;
        this.multicastSocket = new MulticastSocket(multicastPort);
        InetAddress group = InetAddress.getByName(multicastIP);
        this.multicastSocket.joinGroup(group);
        // attacco il termination handler
        Runtime.getRuntime().addShutdownHook(new leavingGroupThread(multicastSocket, group));
    }

    @Override
    public void run () {
        byte[] buf = new byte[1024];
        DatagramPacket dp;
        // Attende ripetutamente notifiche
        while (true) {
            dp = new DatagramPacket(buf, buf.length);
            try {
                multicastSocket.receive(dp);
//                System.out.println("[MulticastWaitingThread]" + new String(dp.getData(), StandardCharsets.UTF_8).toString());
                // aggiungo la notifica alla lista
                String notifica = new String(dp.getData(),0, dp.getLength(), StandardCharsets.US_ASCII);
                this.notifiche.add(notifica);
            }
            catch (IOException e) {
//                System.out.println("multicastWaitingThread interrotto");
            }
        }
    }

    private class leavingGroupThread extends Thread { // thread che viene eseguito quando il client chiuda l'applicazione 'brutalmente'
        private  MulticastSocket ms;
        private InetAddress group;
        public leavingGroupThread(MulticastSocket ms, InetAddress group) {
            this.ms = ms;
            this.group = group;
        }

        @Override
        public void run() {
            try {
                if (!ms.isClosed()) {
                    ms.leaveGroup(this.group);
                    ms.close();
//                    System.out.println("[leavingGroupThread] Multicast chiuso correttamente");
                }
            } catch (IOException e) { System.out.println("[leavingGropuThread] Errore nella chiusura: " + e.getMessage());}
        }
    }


}



