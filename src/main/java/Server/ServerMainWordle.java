package Server;

import Remote.NotifyServerImpl;
import Remote.NotifyServerInterface;
import Remote.RemoteRegistrationImpl;
import Remote.RemoteRegistrationInterface;
import Server.Database.Database;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ServerMainWordle {
    private static String configFile = "./server.properties";
    private static String databaseFile;
    private static String vocabolary;
    public static String mcastIp;
    private static String serverAddress;
    private static String rmiRegistrationName;
    private static String rmiNotifyName;
    private static int      serverPort;
    private static int 		rmiPort;
    private static int		rmiNotifyPort;
    private static int 		delay;
    public static int 		mcastPort;

    private static Database database;
    private static final ReentrantLock lock = new ReentrantLock();
    private static String secretWord;
    private static MulticastSocket ms;
    private static InetAddress inetAddressMulticast;
    public static NotifyServerImpl notifyServer;

    public static void main(String[] args) {
        //1 leggo il file di configurazione del server.
        readServerConfig();

        //2 ripristino database
        try {
            database = new Database(databaseFile);
        } catch (Exception e) {
            println("qualcosa non va con il ripristino del database");
            System.exit(1);
        }
        println("RIPRISTINO STATO TERMINATO!");

        //3 Avvio il servizio remoto di registrazione
        if (!startRMIregistration()) {
            println("Errore durante l'avvio della registrazione remota");
            System.exit(1);
        }

        //4 Avvio Servizio Callback
		  if(!startNotifySystem()) {
            println("Errore durante l'avvio della registrazione remota");
            System.exit(1);
        }

        //5 Lancio il servizio di multicast x la condivisione dei risultati
        println("INIZIALIZZANDO RETE MULTICAST");
        try {
            ms = new MulticastSocket(mcastPort);
            inetAddressMulticast = InetAddress.getByName(mcastIp);
            ms.joinGroup(inetAddressMulticast);//il server joina nella rete multicast
            println("RETE MULTICAST AVVIATA");
        } catch (IOException e) {
            println("errore con il multicast");
            e.printStackTrace();
        }

        //6 Abilito tutti i giocatori a giocare all'ultima parola estratta
        //Updater della parola e dei risultati.
        if (database.isEmpty() == false) database.enableAll();

        // 7 Creo il Thread che ogni 'delay' seconds estrae una nuova parola segreta e
        // 8 Creo il Thread che ogni 'delay' seconds salva il database in locale
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new WordExtractorThread(vocabolary), 0, delay, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(new DatabaseSavingThread(database),  0, delay, TimeUnit.SECONDS);

        //	9 Creo il threadPool per servire i client
        //	comunicazione client - server TCP. Quando un client si connette, viene eseguito un nuovo ServerThread (worker)
        ExecutorService threadPool = Executors.newCachedThreadPool();
        try (ServerSocket listenerSocket = new ServerSocket(serverPort)) {
            // 10 registro il termination handler per il server, cosi'da non accettare piu richieste, chiudere il threadPool e uscire dalla rete multicast
            Runtime.getRuntime().addShutdownHook(new ServerCtrlCHandler(2000, threadPool, listenerSocket, ms));

            println("Server in ascolto sulla porta " + serverPort);
            while (true) {
                threadPool.execute(new Worker(listenerSocket.accept(), database, vocabolary, lock, ms, inetAddressMulticast));
            }
        }catch (IOException e) {
            println("il server sta per essere chiuso");
            System.exit(0);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
    /**
     * Legge il file di configurazione del server.
     */
    private static void readServerConfig() {
        try(InputStream input = new FileInputStream(configFile)){
            Properties prop = new Properties();
            prop.load(input);
            databaseFile 		= prop.getProperty("databaseFile");
            vocabolary 			= prop.getProperty("vocabolary");
            mcastIp 			= prop.getProperty("mcastIp");
            serverAddress		= prop.getProperty("serverAddress");
            rmiRegistrationName = prop.getProperty("rmiRegistrationName");
            rmiNotifyName 		= prop.getProperty("rmiNotifyName");
            serverPort 		= Integer.parseInt(prop.getProperty("serverPort"));
            rmiPort 		= Integer.parseInt(prop.getProperty("rmiPort"));
            rmiNotifyPort 	= Integer.parseInt(prop.getProperty("rmiNotifyPort"));
            delay 			= Integer.parseInt(prop.getProperty("delay"));
            mcastPort		= Integer.parseInt(prop.getProperty("mcastPort"));
        }
        catch(IOException e) {
            System.err.println("[SERVER] Errore durante la lettura del file di configurazione del server.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Avvia il servizio di registrazione implementato tramite RMI
     */
    private static boolean startRMIregistration() {
        println("INIZIALIZZANDO RMI");
        try {
            /*Creazione di un'istanza di un oggetto remoto*/
            RemoteRegistrationImpl registrazione = new RemoteRegistrationImpl(database, lock);
            /*Esportazione dell'oggetto*/
            RemoteRegistrationInterface stub = (RemoteRegistrationInterface)
                    UnicastRemoteObject.exportObject(registrazione, 0);
            //creazione registry sulla porta rmiPort
            LocateRegistry.createRegistry(rmiPort);//<-creo
            Registry r = LocateRegistry.getRegistry(rmiPort);//<-ottengo il riferimento
            //Pubblicazione dello stub nel registry
            r.bind(rmiRegistrationName, stub);//<- creo un'associazione <NOME SIMBOLICO, stub>

            println("SERVIZIO DI REGISTRAZIONE REMOTO AVVIATO");
            return true;
        } catch (RemoteException e){
            e.printStackTrace();
            println("RemoteException: "+ e);
            return false;
        } catch (AlreadyBoundException e){
            e.printStackTrace();
            println("AlreadyBoundException: "+ e);
            return false;
        }
    }
    /**
     * Avvia il servizio di notifiche implementato tramite RMI CALLBACK
     */
    private static boolean startNotifySystem() {
        println("INIZIALIZZANDO CALLBACK");
		try {
            /*Creazione di un'istanza di un oggetto remoto*/
            notifyServer = new NotifyServerImpl();
            /*Esportazione dell'oggetto*/
            NotifyServerInterface stub = (NotifyServerInterface)
                    UnicastRemoteObject.exportObject(notifyServer, rmiNotifyPort);
            /*creazione registry sulla porta rmiNotifyPort*/
			LocateRegistry.createRegistry(rmiNotifyPort);//<-creo
			Registry registry = LocateRegistry.getRegistry(rmiNotifyPort);//<-ottengo il riferimento
            /*Pubblicazione dello stub nel registry*/
			registry.bind(rmiNotifyName, stub);//<- creo un'associazione <NOME SIMBOLICO, stub>
            println("SERVIZIO DI NOTIFICHE CALLBACK AVVIATO");

            return true;
        } catch (RemoteException e){
			e.printStackTrace();
			println("RemoteException: "+ e);
            return false;
		} catch (AlreadyBoundException e){
			e.printStackTrace();
			println("AlreadyBoundException: "+ e);
            return false;
		}
    }

    public static String getActualSecretWord() {
        return secretWord;
    }

    public static void setActualSecretWord(String newSecretWord){
        secretWord = newSecretWord;
    }

    private static void println(String s) { System.out.println("[SERVER] " +s);}
}

