package Client;

import Remote.NotifyEventImpl;
import Remote.NotifyEventInterface;
import Remote.NotifyServerInterface;
import Remote.RemoteRegistrationInterface;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class ClientMainWordle {
    public static final String configFile = "./client.properties";
    public static String hostName;
    public static String serverIp;
    public static String rmiRegistrationName;
    public static String rmiNotifyName;
    public static String mcastIp;
    public static int 	 serverPort;
    public static int 	 rmiPort;
    public static int 	 rmiNotifyPort;
    public static int 	 mcastPort;
    private static Scanner scanner = new Scanner(System.in); //scanner degli input da tastiera dell'utente
    private static Socket socket;
    private static Scanner socketIn;  // Scanner di dati provenienti dalla socket
    private static PrintWriter socketOut; // PrintWriter di dati da mandare alla socket
    private static final ArrayList<String> notifiche = new ArrayList<>(); //struttura che memorizza le notifiche
    private static NotifyServerInterface serverInterface;
    private static NotifyEventInterface stub;
    private static boolean isPlaying = false;

    public static void main(String[] args) {
        //1 Leggo il file di configurazione
        readClientConfig();

        //2 avvio l'interfaccia
        welcomeMenu(); //<- registrazione/Login/Exit
    }

    private static void session() {//<-sessione di gioco [posso fare logout]
        println("SESSIONE DI GIOCO");
        int esito = 0;
        while (isPlaying) {
            System.out.println(
                      "<1> sendWord\n"
                    + "<2> showMeSharing\n"
                    + "<3> showMeRanking\n"
                    + "<4> Logout\n"
            );
            print();
            String command = scanner.nextLine();
            switch(command) {
                case "1"://sendWord
                    if((esito=sendWord()) != 0) {
                        println("SESSIONE DI GIOCO TERMINATA");
                        isPlaying = false;
                    }
                    break;
                case "2"://showMeSharing
                    showMeSharing();
                    break;
                case "3"://showMeRanking
                    showMeRanking();
                    break;
                case "4"://Logout
                    logout();
                    break;
                default:
                    println("Comando non riconosciuto, riprova");
                    break;
            }//END switch(command)
        }//END while(!end)
        println("vuoi condividere il tuo risultato? y/n");
        String option = scanner.nextLine();
        if(option.equals("y"))  share(esito);
        menu();//<- torno al menu principale
    }//END session()

    private static int sendWord () {
        boolean ok = false;
        String guessedWord = null;
        while(!ok) {
            System.out.print("[GuessedWord]> ");
            guessedWord = scanner.nextLine();
            if (guessedWord.length()==10) ok = true;
            else System.out.println("La parola e' di 10 lettere");
        }
        socketOut.println("sendWord"+"#"+guessedWord);

        String response = socketIn.nextLine();//[xxxx#xxxxx] oppure [xxxx#]
        String[] info = response.split("#");// ["xxxx""xxxx" "x"]

        int statusCode = Integer.parseInt(info[0]);
        switch(statusCode) {
            case 0://indizio
                System.out.println("Ogni lettera vale:\n" +
                        "'+/verde' : se indovinata e si trova nella posizione corretta\n" +
                        "'?/giallo' : se indovinata, ma si trova in una posizione diversa\n" +
                        "'x/grigio' : se non compare.\n");
                System.out.printf("[INDIZIO]" + info[1] + " restano " + info[2] + " tentativi\n");
                return 0;
            case 1://la parola non esiste
                println("Non conosco la parola. Il tentativo non e' stato considerato");
                return 0;
            case 2://vinto
                println("VITTORIA! La parola segreta e' " + guessedWord + " che tradotto significa " + info[1]);
                return 1;
            case 3://perso
                println("HAI PERSO! 0 tentativi rimanenti. Attendi per la prossima parola!. La parola segreta era " + info[1] + " che tradotto significa " + info[2]);
                return 2;
            default://boh
                println("Something went wrong. Please, try again later.");
                return 4;
        }
    }//END sendWord()

    private static void menu() {
        println("MENU");
        while (!isPlaying) {
            System.out.println(
                    "<1> playWORDLE\n"
                  + "<2> sendMeStatistics\n"
                  + "<3> showMeSharing\n"
                  + "<4> showMeRanking\n"
                  + "<5> Logout\n");
            print();
            String command = scanner.nextLine();
            switch(command) {
                case "1"://playWORDLE
                    if(playWordle()) isPlaying = true;
                    break;
                case "2"://sendMeStatistics
                    sendMeStatistics();
                    break;
                case "3"://showMeSharing
                    showMeSharing();
                    break;
                case"4"://showMeRanking
                    showMeRanking();
                    break;
                case "5"://Logout
                    logout();
                    break;
                default:
                    println("Comando non riconosciuto, riprova");
                    break;
            }//END switch(command)
        }//END while(!playing)
        session();
    }//END menu()

    private static boolean playWordle () {
        socketOut.println("playwordle" + "#");
        //Risposta
        int retCode = Integer.parseInt(socketIn.nextLine());
        if (retCode == 200) {
//            println("Il Gioco e' iniziato");
            return true;
        }
        else {
            println("Hai gia' giocato, torna piu' tardi");
            return false;
        }
    }//END playWordle()

    private static void welcomeMenu() {
        boolean done = false;
        println("Benvenuto in WORDLE!");
        while (!done) {
            System.out.println(
                      " <1> -> Registrazione\n"
                    + " <2> -> Login\n"
                    + " <3> -> Esci");
            print();
            String command = scanner.nextLine();
            switch(command) {
                case "1"://REGISTRAZIONE (username, password)
                    register();
                    done = true;
                    break;
                case "2":// LOGIN(USERNAME, PASSWORD) + MULTICAST + RMI callback x notifiche
                    if(credenziali()) done = true;//->login(username, password)
                    break;
                case "3"://LOGOUT()
                    scanner.close();
                    System.exit(0);
                    break;
                default:
                    println("Comando non riconosciuto, riprova");
                    break;
            }//END switch(command)
        }//END while(true)
        menu();//<- menu' principale [posso fare logout]
    }//END welcomeMenu()

    private static void register() {
        try {
            Registry r = LocateRegistry.getRegistry(rmiPort);
            RemoteRegistrationInterface registrazione = (RemoteRegistrationInterface)
                    r.lookup(rmiRegistrationName);
            // println("Servizio remoto di registrazione avviato con successo");
            //<-Servizio remoto di registrazione avviato con successo
            while(true) {
                println("Inserisci username,non inserire '#'");
                print();
                String newUsername = scanner.nextLine().trim();
                println("Inserisci password, non inserire '#'");
                print();
                String newPassword = scanner.nextLine().trim();

                if(!newUsername.contains("#") && !newPassword.contains("#") && newPassword.length()!=0 && newUsername.length() != 0) {
                    int err = registrazione.register(newUsername, newPassword);
                    if(err == 0) {
                        println("Registrazione avvenuta con successo");
                        login(newUsername, newPassword);
                        break;
                    }
                    else if(err == 1) println("Username inserito non valido perche' esiste gia', Riprovare");
                    else if(err == 2) println("Si e' verificato un errore durante la registrazione[tryLock fallita]");
                }
                else if(newUsername.contains("#") || newPassword.contains("#")) println("Riprova e non inserire '#'");
                else if (newUsername.length() ==0 || newPassword.length() == 0) println("Riprova, Username o password vuoti");
            }
        }catch(InterruptedException e) {
            println("Registrazione interrotta");
            e.printStackTrace();
        }catch(RemoteException e) {
            println("[RemoteException] errore durante la richiesta di registrazione");
            e.printStackTrace();
            System.exit(1);
        }catch(Exception e) {
            println("[Exception] error invoking object method\n" + e + e.getMessage());
            e.printStackTrace();
        }
    }//END register()

    private static boolean credenziali () {
        if(socket == null)	connect();
        //<-instaurato una connessione tcp con il server
        while(true) {
            println("[LOGIN]");
            System.out.println("Inserisci Username");
            print();
            String tryUsername = scanner.nextLine();
            System.out.println("Inserisci password");
            print();
            String tryPassword = scanner.nextLine();

            if(!tryUsername.contains("#") && !tryPassword.contains("#") && tryUsername.length()!=0 && tryPassword.length()!=0) {
                if(login(tryUsername, tryPassword)) {
                    println("Login terminato, benvenuto '" + tryUsername + "'");
                    return true;//<- loggato
                }
                println("login tentato ->Credenziali non riconosciute, riprova");
                return false;//<-torno a welcomeMenu
            }
            else if(tryUsername.contains("#") || tryPassword.contains("#")) println("# ->credenziali gia' in uso");
            else if(tryUsername.length()==0 || tryPassword.length()==0) println("username o password vuoti");
        }//END while(true)
    }//END credenziali()

    private static boolean login (String username, String password) {
        if(socket == null)	connect();
        socketOut.println("login#"+username+"#"+password);

        int statusCode = Integer.parseInt(socketIn.nextLine());
        switch(statusCode) {
            case 200://loggato con successo
				try {
                println("Ok, logged. Now you can play WORDLE!");
					MulticastWaitingThread multicastWaitingThread = new
							MulticastWaitingThread(notifiche, mcastIp, mcastPort);
					Thread t = new Thread(multicastWaitingThread);
					t.start();//-> joino nella rete multicast
                    if(!registrazioneNotifiche()){//-> mi registro al servizio di notifiche RMI callbacks
                        println("qualcosa e' andato storto");
                    }
				} catch (IOException e) {
					e.printStackTrace();
				}
                return true;
            case 404:
                println("User not found");
                break;
            case 406:
                println("Wrong password!");
                break;
            default:
                println("Something went wrong. Please, try again later. CODE " + statusCode);
                break;
        }
        return false;
    }//END login(username, password)

    /**
     * Si connette al server e inizializza lo Scannner (input) e l'output (PrintWriter) per dialogare con il server
     * stabilisce la connessione client-server
     */
    private static void connect () {
        try {
            socket 		= new Socket(serverIp, serverPort);
            socketIn 	= new Scanner(socket.getInputStream());
            socketOut 	= new PrintWriter(socket.getOutputStream(), true);
            println("connesso al server");
        } catch (Exception e) {
            println("[CLIENT]	Impossibile connettersi al server " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }//END connect

    private static void logout () {
        socketOut.println("logout"+"#");

        int retCode = Integer.parseInt(socketIn.nextLine());
        if (retCode == 200) {
            disconnect();
        }
        else println("Something went wrong. Please, try again later.");
    }

    private static void disconnect() {
        try {
            socketOut.close();
            socketIn.close();
            socket.close();
            scanner.close();
            serverInterface.unregisterForCallback(stub);//<- mi disiscrivo dal sistema di notifiche
            System.exit(0);//<-esco dal multicast
        } catch (IOException e) {
            println("ci sono stati problemi con la disconnessione dal server");
            e.printStackTrace();
        }
    }//END disconnect

    /**
     * Avvia il servizio di notifiche implementato tramite RMI callabck
     */
    private static boolean registrazioneNotifiche() {
        try {
            Registry registry = LocateRegistry.getRegistry(rmiNotifyPort);
            serverInterface = (NotifyServerInterface) registry.lookup(rmiNotifyName);
            //<- mi registro per la callback
            // println("registrandomi per la callback");
            //<- creo l'oggetto remoto
            NotifyEventInterface callbackObj = new NotifyEventImpl();
            //<- esporto l'oggetto remoto che contiene il metodo che usera' il server per le notifiche
            stub = (NotifyEventInterface)
                    UnicastRemoteObject.exportObject(callbackObj, 0);

            serverInterface.registerForCallback(stub);
            return true;
        } catch (RemoteException e) {
            println("RemoteException");
            e.printStackTrace();
            return false;
        } catch (NotBoundException e) {
            println("NotBoundException");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Visualizza tutte le notifiche che sono arrivate dal momento in cui il client si e' connesso al server
     */
    private static void showMeSharing() {
        if(!notifiche.isEmpty())
            for (String notifica : notifiche) {
            println(notifica);
        }
        else println("Non ci sono notifiche");
    }

    /**
     * visualizza le proprie statistiche aggiornate all'ultimo gioco terminato
     */
    private static void sendMeStatistics () {
        socketOut.println("sendmestatics"+ "#");
        String response = socketIn.nextLine();
        String[] statistiche = response.split(",");
            System.out.println("Statistiche:");
            for (String stat: statistiche){
                System.out.println(stat);
            }
        }

    private static void share(int esito) {
        socketOut.println("share" + "#" + esito);
    }

    /**
     * visualizza la classifica globale
     */
    private static void showMeRanking () {
        socketOut.println("showmeranking"+ "#");
        String response = socketIn.nextLine();//[xxxx#xxxxx] oppure [xxxx#]
        String[] info = response.split("#");// ["xxxx""xxxxx"]
        int statusCode = Integer.parseInt(info[0]);
        if (statusCode == 200) {
            String[] classifica = info[1].split("!");
            System.out.println("[CLASSIFICA]");
            for (String player: classifica){
            System.out.println(player);
            }
        }
        else println("Something went wrong. Please, try again later.");
    }

    /**
     * Legge il file di configurazione del client.
     */
    private static void readClientConfig() {
        try(InputStream input = new FileInputStream(configFile)){
            Properties prop = new Properties();
            prop.load(input);
            rmiRegistrationName = prop.getProperty("rmiRegistrationName");
            hostName 		= prop.getProperty("hostName");
            serverIp 		= prop.getProperty("serverIp");
            rmiNotifyName 	= prop.getProperty("rmiNotifyName");
            mcastIp 		= prop.getProperty("mcastIp");
            serverPort 		= Integer.parseInt(prop.getProperty("serverPort"));
            rmiPort 		= Integer.parseInt(prop.getProperty("rmiPort"));
            rmiNotifyPort 	= Integer.parseInt(prop.getProperty("rmiNotifyPort"));
            mcastPort 	= Integer.parseInt(prop.getProperty("mcastPort"));
        }
        catch(IOException e) {
            println("Errore durante la lettura del file di configurazione.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void println(String s) { System.out.println("[CLIENT] " +s);}
    private static void print() { System.out.printf("> ");}
}
