package Server;

import Server.Database.Database;

public class DatabaseSavingThread implements Runnable{
    private Database database;

    public DatabaseSavingThread(Database db){
        this.database = db;
    }

    @Override
    public void run() {
        this.database.salvaInLocale();
        System.out.println("[DBSAVINGTHREAD] Database salvato in locale");
    }
}
