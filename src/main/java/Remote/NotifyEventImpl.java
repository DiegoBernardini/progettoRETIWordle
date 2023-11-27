package Remote;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

public class NotifyEventImpl extends RemoteObject implements NotifyEventInterface {

    /**
     * crea una nuova callback client
     * @throws RemoteException
     */
    public NotifyEventImpl() throws RemoteException {super();}


    /**
     *  Metodo che puo' essere richiamto dal servente per notificare che
     *  c'e' stato un cambiamento nelle prime 3 posizioni della classifica.
     * @throws RemoteException
     */
    public void notifyTop3() throws RemoteException {
        System.out.println("[CallBack] C'e' stato un cambiamento nelle prime 3 posizioni della classifica");
    }

}
