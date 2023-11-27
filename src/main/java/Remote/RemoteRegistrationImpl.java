package Remote;

import Server.Database.Database;
import Server.Database.User;

import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class RemoteRegistrationImpl implements RemoteRegistrationInterface {
    private ReentrantLock lock;
    private Database db;

    public RemoteRegistrationImpl(Database storage, ReentrantLock lock) throws RemoteException {
        this.db 	 = storage;
        this.lock 	 = lock;
    }

    /**
     * @param newUserName
     * @param password
     * @throws RemoteException if an error occurs during the execution of this remote method.
     * @throws InterruptedException if the current thread has been interrupted.
     * @return 	0 when a new object 'User' has been successfully created and
     * 		   	added on the database.
     * 			1 if and only if newUserName already exists.
     * 			2 if and only if this method hasn't been able to check if newUserName already exists.
     */
    public int register(String newUserName, String password) throws RemoteException, InterruptedException {
        boolean lockAcquired = this.lock.tryLock(60, TimeUnit.SECONDS);
        if(lockAcquired){
            try{
//                debug("lockAcquired x registrazione");
                if(this.db.exist(newUserName)) {//<-username gia' in uso
                    return 1;
                }
//              debug("username valido");
                //<-newUserName valido
                //creo un account per il nuovo utente
                User newUser = new User(newUserName, password);
//                debug(newUser.toString());
                this.db.insert(newUser);
//                debug("dice che ho inserito");
                this.db.salvaInLocale();
//                debug("dice che ho salvato sul db");

                return 0;
            }finally{
                this.lock.unlock();
                debug("lock rilasciata, registrazione terminata");
            }
        }
        //timeout della tryLock esaurito, !lockAcquired
        return 2;
    }
    private static void debug(String s) {System.out.println("[DEBUG]" +s);}
}
