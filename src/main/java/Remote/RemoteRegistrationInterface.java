package Remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteRegistrationInterface extends Remote {
    int register(String userName, String password) throws RemoteException, InterruptedException;
}
