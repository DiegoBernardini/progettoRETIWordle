package Remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NotifyEventInterface extends Remote {
    /**
     * Metodo invocato dal server per notificare un evento ad un client remoto
     * */
    public void notifyTop3() throws RemoteException;
}