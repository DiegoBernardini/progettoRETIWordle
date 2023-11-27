package Server;

import Server.Database.Account;
import Server.Database.Database;
import Server.Database.Statistiche;
import Server.Database.User;
import Server.Models.BinarySearch;
import Server.Models.ScoreCalculator;
import Server.Models.Traduttore;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.*;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Worker implements Runnable {
    private Socket clientSocket;
    private Database database;
    private String vocabolary;
    private MulticastSocket multicastSocket;
    private InetAddress group;
    private String secretWord;
    private Account account;
    private Statistiche statsOfThisAccount;
    private double scoreOfThisAccount;
    private String usernameOfThisAccount;
    private static int maxAttempts = 12;
    private int usedAttempts = -1;
    private  Lock lock;
    // codici dei background per creare le risposte alle parole
    private static final String GREEN_BACKGROUND = "\u001B[42m";
    private static final String YELLOW_BACKGROUND = "\u001B[43m";
    private static final String GREY_BACKGROUND = "\033[0;100m";
    private static final String ANSI_END = "\u001B[0m";



    public Worker(Socket newClientSocket, Database database, String vocabolary, ReentrantLock lock, MulticastSocket multicastSocket, InetAddress inetAddressMulticast) {
        this.clientSocket	= newClientSocket;
        this.database		= database;
        this.vocabolary		= vocabolary;
        this.lock 			= lock;
        this.multicastSocket = multicastSocket;
        this.group = inetAddressMulticast;
    }

    @Override
    public void run() {
        boolean logout = false;
        println("Client connected");

        try(Scanner in = new Scanner(clientSocket.getInputStream());
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            RandomAccessFile f = new RandomAccessFile(vocabolary, "r")){
            while(!logout) {
                while (in.hasNextLine()) {
                    String line = in.nextLine();//[xxxx#xxxxx#xxxxx]
                    println("line= " +line);
                    String[] info = line.trim().split("#");// ["xxxx""xxxx"xxxxx"]
                    String command = info[0];//comando utente
                    switch(command) {
                        case "login":
                            try {
                                workerLogin(out, info[1], info[2]);
                            } catch (InterruptedException e) {
                                println("WorkerLogin interrotto");
                                e.printStackTrace();
                            }
                            break;
                        case "logout":
                            logout = true;
                            println("il client sta cercando di disconnettersi");
                            workerLogout(out);
                            break;
                        case "playwordle":
                            println("il client vuole giocare");
                                startPlaying(out);
                            break;
                        case "sendWord": //[xxx#xxxxxx]
                            println("il client ha inviato una guessedWord");
                            this.usedAttempts++;
                            if(info[1].equals(this.secretWord)){//WIN
                                out.println("2" + "#" + Traduttore.translate(this.secretWord));
                                updateStatsWin();
                                break;
                            }
                            else if(this.usedAttempts == maxAttempts){//LOSE
                                out.println("3" + "#" +this.secretWord + "#" + Traduttore.translate(this.secretWord));
                                updateStatsLose();
                                break;
                            }

                            int found = BinarySearch.binarySearch(f, info[1]);
                            if(found == -1) {
                                out.println("1" + "#");//parola non conosciuta
                                this.usedAttempts--;//il tentativo non va considerato
                            }
                            else out.println("0" + "#" + generaIndizio(info[1]) + "#" + (maxAttempts-this.usedAttempts));//funzione per mandare la stringa con gli indizzi


                            break;
                        case "sendmestatics":
                            //statistiche aggiornate all'ultima parola giocata
                            // (non viene considerata quella che si deve ancora indovinare nel caso in cui si stesse giocando)
                            out.println(this.account.getStat().toString2());
                            break;
                        case "share": //[XXX#X]
                            share(Integer.parseInt(info[1]));
                            this.usedAttempts = -1;
                            break;
                        case "showmeranking":
                            showMeRanking(out);
                            break;
                    }
                }
            }
        }catch(IOException e){
            println("qualcosa e' andato storto con la socket del client");
        }
    }//END run

    /**
     * @param gw guessedWord
     * @return la stringa che rappresenta la parola colorata in base alla parola segreta
     */
    private String generaIndizio(String gw) {
            StringBuilder hints = new StringBuilder();
            String secWord = secretWord;
            for (int i = 0; i < secWord.length(); i++) {
                char currChar = gw.charAt(i);
                String newChar;
                if (secWord.charAt(i) == currChar)// lettera nella posizione giusta
//                  newChar = "+";
                    newChar = GREEN_BACKGROUND + currChar + ANSI_END;
                else if (secWord.indexOf(currChar) != -1) { // lettera c'e' ma non nella posizione i
//                  newChar = "?";
                    newChar = YELLOW_BACKGROUND + currChar + ANSI_END;
                }
                else  { // lettera non c'e'
//                  newChar = "X";
                    newChar = GREY_BACKGROUND + currChar + ANSI_END;
                }
                hints.append(newChar);
            }
            return hints.toString();
        }

    private void updateStatsWin(){
        this.statsOfThisAccount.incrementWon();
        this.statsOfThisAccount.incrementLastStreak();
        this.statsOfThisAccount.guessDistributionAdd(this.usedAttempts-1);
        this.statsOfThisAccount.checkBestStreak();
        this.statsOfThisAccount.calcolaPercVittorie();
        this.scoreOfThisAccount = ScoreCalculator.computeScore(this.statsOfThisAccount.getTotPlayed(), this.statsOfThisAccount.getGuessDistribution());
        aggiornaUser();
   }

    private void updateStatsLose(){
        this.statsOfThisAccount.resetLastStreak();
        this.statsOfThisAccount.calcolaPercVittorie();
        this.scoreOfThisAccount = ScoreCalculator.computeScore(this.statsOfThisAccount.getTotPlayed(), this.statsOfThisAccount.getGuessDistribution());
        aggiornaUser();
   }

    /**
     * metodo utilizzato per aggiornare il profilo dell'user identificato da 'usernameOfThisAccount' con le nuove statistiche ai fini della classifica
     * con il nuovo punteggio e le nuove statistiche quando termina un gioco.
     */
   private void aggiornaUser(){
       try{
           boolean lockAcquired = lock.tryLock(60, TimeUnit.SECONDS);
           if(lockAcquired) {
               try {
                   this.database.aggiornaUser(this.usernameOfThisAccount, this.scoreOfThisAccount, this.statsOfThisAccount);
                   boolean top3isChanged = this.database.checktop3();
                   if(top3isChanged) {//callback
                       try {
                           ServerMainWordle.notifyServer.update();
                           // System.out.println("entro in sleep per 15 sec");
                           // Thread.sleep(15000);
                       } catch (RemoteException e) {
                           println("Problemi nella callback");
                           e.printStackTrace();
                       }
                   }
               }finally {lock.unlock();}
           } else {//non sono riuscito a prendere la lock
//               out.println("503");//service unavailable;
           }
       }catch (InterruptedException e){
           println("AggiornaUser interrotto");
           e.printStackTrace();
       }
   }

    /**
     * @param out outputstream su cui inviare risposte
     */
    private void startPlaying(PrintWriter out)  {
        //recupero la parola segreta attuale
        this.secretWord = ServerMainWordle.getActualSecretWord();
        println("SW ATTUALE '" +secretWord+"'");
        //controllo se l'utente ha gia' giocato all'attuale secretWord
        if(this.statsOfThisAccount.getLastWordPlayed().equals(this.secretWord)){
            println("user ha gia' giocato per questa parola");
            out.println("503");//service unavailable
            return;
        }
        //Non ha ancora giocato e quindi puo giocare
        this.statsOfThisAccount.setLastWordPlayed(this.secretWord);
        this.statsOfThisAccount.incrementPlayed();
        this.usedAttempts = 0;
        out.println("200");
    }

    /**
     * @param out outputstream su cui inviare risposte
     * @param username username
     * @param password password
     * @throws InterruptedException trylock
     */
    private void workerLogin (PrintWriter out, String username, String password) throws InterruptedException {
        User possibleUser;
        boolean lockAcquired = lock.tryLock(60, TimeUnit.SECONDS);
        if(lockAcquired){
            try {
                if ((possibleUser = database.find(username)) == null) {
                    println("username non trovato nel database");
                    out.println("404");
                    //System.out.println("entro in sleep per 15 sec");
                    //Thread.sleep(15000);
                    return;
                }
            }finally {lock.unlock();}
        }else {//non sono riuscito a prendere la lock
            out.println("503");
            return;
        }
        if (!password.equals(possibleUser.getAccount().getPassword())) {
            println("Password errata per " + possibleUser.getUsername());
            out.println("406");
            return;
        }
        this.usernameOfThisAccount = possibleUser.getUsername();
        this.account = possibleUser.getAccount(); //ora ho fissato l'utente nel thread a lui dedicato
        this.statsOfThisAccount = this.account.getStat();
        out.println("200");
        println("autenticato");
    }

    /**
     * @param out outputstream su cui inviare risposte
     */
    private void workerLogout(PrintWriter out) {
        if(this.usedAttempts!=-1){//vuol dire che l'utente stava giocando
            updateStatsLose();//considero come una sconfitta
        }
        try{
            boolean lockAcquired = lock.tryLock(60, TimeUnit.SECONDS);
            if(lockAcquired) {
                try {
                    if (statsOfThisAccount.getTotPlayed() != 0)
                        this.database.aggiornaUser(this.usernameOfThisAccount, this.scoreOfThisAccount, this.statsOfThisAccount);
                    else this.database.aggiornaUser(this.usernameOfThisAccount, 14, this.statsOfThisAccount);
                    out.println("200");
                    //System.out.println("entro in sleep per 15 sec");
                    //Thread.sleep(15000);
                }finally {lock.unlock();}
            } else {//non sono riuscito a prendere la lock
                 out.println("503");//service unavailable
            }
        }catch (InterruptedException e){
            println("workerLogout interrotto");
            e.printStackTrace();
        }
    }

    private void share(int esito){
        try{
            SimpleDateFormat sdf = new SimpleDateFormat("[dd/MM/yyyy - HH:mm]");
            String ora = sdf.format(new GregorianCalendar().getTime());
            byte[] data;
            String notifica;
            if(esito == 1)//vittoria
            notifica = ora + " '" + this.account.getUsername()+"' ha indovinato la parola '" + this.secretWord + "' con " + this.usedAttempts + " tentativi";
            //sconfitta, esito=2
            else notifica = ora + " '" + this.account.getUsername()+"' non e' riuscito ad indovinare '" + this.secretWord +"'";

            data = notifica.getBytes();
            DatagramPacket dp = new DatagramPacket(data, data.length, this.group, ServerMainWordle.mcastPort);
            multicastSocket.send(dp);
       } catch (IOException e) {
            System.out.println("Problemi nell'invio del pacchetto");
            e.printStackTrace();
        }
    }

    /**
     * @param out outputstream su cui inviare risposte
     */
    private void showMeRanking(PrintWriter out){
        try{
            boolean lockAcquired = lock.tryLock(60, TimeUnit.SECONDS);
            if(lockAcquired) {
                try {
                    out.println("200" + "#" + this.database.getClassifica());
//                    System.out.println("entro in sleep per 15 sec");
//                    Thread.sleep(15000);
                }finally {lock.unlock();}
            } else {//non sono riuscito a prendere la lock
                out.println("503");//service unavailable
            }
        }catch (InterruptedException e){
            println("showMeRanking Interrotto");
            e.printStackTrace();
        }
    }
    private static void println(String s) { System.out.println("[WORKER] "+ s);}
}
