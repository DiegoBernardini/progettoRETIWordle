package Remote;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.List;

public class NotifyServerImpl extends RemoteServer implements NotifyServerInterface{
    /*lista degli utenti che hanno fatto il login*/
    private final List <NotifyEventInterface> playerOnline;

    public NotifyServerImpl() throws RemoteException{
        super();
        playerOnline = new ArrayList<NotifyEventInterface>();
    }

    @Override
    public synchronized void registerForCallback(NotifyEventInterface clientInterface) throws RemoteException {
        // TODO Auto-generated method stub
        if(!playerOnline.contains(clientInterface)) playerOnline.add(clientInterface);
        System.out.println("[NotifyserverImpl] Nuovo user registrato alla callback");
    }

    @Override
    public synchronized void unregisterForCallback(NotifyEventInterface clientInterface) throws RemoteException {
        if(playerOnline.remove(clientInterface)) System.out.println("[NotifyEventImpl] Client unregistered");
        else System.out.println("[NotifyEventImpl] Unable to unregister client");
    }

    /**
     * Notifica di una variazione nella classifica.
     * metodo che quando viene richiamato fa il callback a tutti i clienti registrati
     * @throws RemoteException
     */
    public void update () throws RemoteException{
        doCallbacks();
    }

    private synchronized void doCallbacks() throws RemoteException{
        System.out.println("Starting callbacks.");
        //int numeroClienti = clients.size( );
        for (NotifyEventInterface client : playerOnline) {
            client.notifyTop3();
        }
        System.out.println("Callbacks complete.");}
}
