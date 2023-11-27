package Server.Database;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

public class Database {
    private String dbFileName;
    private ArrayList<User> classifica;
    private List<User> top3;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public Database(String fileName) throws Exception {
        this.dbFileName = fileName;
        this.classifica = startDatabase();
        this.top3 = getTop3(this.classifica);
        System.out.println(this.getClassifica());

    }

    public void stampatop3(){
//        println("[TOP3 vecchia]");
        for (User tmp: this.top3) {
            if(tmp != null) System.out.println(tmp.toString2());
            else System.out.println("null");
        }
    }

    private ArrayList<User> getTop3(ArrayList<User> c){
        ArrayList<User> tmp = new ArrayList<User>();
        if(c.size()>=3) {
            tmp.add(c.get(0));
            tmp.add(c.get(1));
            tmp.add(c.get(2));
        }
        //    		return 2 elem e 1 null;
        if(c.size() == 2 ) {
            tmp.add(c.get(0));
            tmp.add(c.get(1));
            tmp.add(null);
        }
        //    		return un elem e 2 null;
        else if (c.size()==1) {
            tmp.add(c.get(0));
            tmp.add(null);
            tmp.add(null);
        }
        else if(c.isEmpty()) {//return 3 null;
            tmp.add(null);
            tmp.add(null);
            tmp.add(null);
        }
        return tmp;
    }

    /**
     * metodo che viene utilizzato dal costruttore di questa classe per creare la classifica
     * aggiornata a quando il server e' stato spento
     */
    private ArrayList<User> startDatabase() throws Exception {
        File file = new File(this.dbFileName);
        //File Vuoto
        if(file.length() == 0) {
            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.write("[]");
                return new ArrayList<User>();
            }
        }
        //file non vuoto
        try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(file)))) {
            Type listOfAccountObject = new TypeToken<ArrayList<User>>() {}.getType();
            ArrayList<User> outputList = gson.fromJson(reader, listOfAccountObject);
            return outputList;
        }
    }

    /**
     * metodo utilizzato per ordinare la classifica in ordine crescente. Al punteggio piu' basso corrisponde
     * il giocatore piu' forte
     */
    public void ordinaClassifica() {
        Collections.sort(this.classifica, new Comparator<User>() {
            public int compare(User u1, User u2) {
                return Double.compare(u1.getScore(), u2.getScore());
            }
        });
    }

    /**
     * @param usernameToSearch usernameToSearch
     * @return the value to which the specified key 'username' is mapped,
     * or defaultValue 'null' if this map contains no mapping for the key.
     */
    public User find(String usernameToSearch) {
        for (User userTmp : this.classifica) {
            if (userTmp.getUsername().equals(usernameToSearch)) return userTmp;
        }
        return null;
    }

    /**
     * metodo utilizzato per controllare se esiste gia' un utente con quell'username
     * @param usernameToSearch usernameToSearch
     * @return true if and only if 'usernameToSearch' is in this list,
     * false otherwise.
     */
    public boolean exist(String usernameToSearch) {
        for (User userTmp : this.classifica) {
            if (userTmp.getUsername().equals(usernameToSearch)) return true;
        }
        return false;
    }

    /**
     * metodo utilizzato per inserire nella classifica un nuovo User
     * che si e' appena registrato
     * @param newUser newUser
     */
    public void insert(User newUser) {
        this.classifica.add(newUser);
        Iterator<User> c = this.classifica.iterator();
        System.out.println("Classifica con nuovo utente");
        while(c.hasNext()) {
            User tmp = c.next();
            System.out.println(tmp.toString2());
        }
    }

    public void salvaInLocale() {
        try (Writer writer = new FileWriter(this.dbFileName)) {
            String classificaJ = gson.toJson(this.classifica);
//            System.out.println(classificaJ);
            writer.write(classificaJ);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Metodo invocato ogni volta che viene estratta una nuova parola segreta.
     * setta il campo 'hasPlayed' di tutti i giocatori registrati a false cosi'
     * che vengano abilitati a giocare
     */
    public void enableAll() {
        for (User user : this.classifica) {
            user.getAccount().getStat().setLastWordPlayed("null");
        }
    }
    public boolean isEmpty () {
        return this.classifica.isEmpty();
    }

    /**
     * Metodo chiamato ogni volta che un utente termina una sessione di gioco,
     * dopo aver aggiornato i dati dell'utente identificato da 'usernameToSearch'
     * chiama ordinaClassifica()
     */
    public void aggiornaUser (String usernameToSearch, double newScore, Statistiche newStatistiche) {
        for (User userTmp : this.classifica) {
            if(userTmp.getUsername().equals(usernameToSearch)){
                userTmp.setScore(newScore);
                userTmp.getAccount().setStat(newStatistiche);
            }
        }
        ordinaClassifica();
    }

    public boolean  checktop3(){
        /*controllo se e' cambiata la top 3*/
//        stampatop3();

        //prendo la top3 attuale(quella dopo l'ordinamento)
        List<User> newTop3 = getTop3(this.classifica);

        System.out.println("[TOP3] post giocata");
        for (User tmp: newTop3){
            if(tmp != null) System.out.println(tmp.toString2());
            else System.out.println("null");
        }
        try{
            for (int i=0;  i<newTop3.size(); i++){
                if(!newTop3.get(i).getUsername().equals(this.top3.get(i).getUsername())){
                    setTop3(newTop3);//aggiorno
                    return true;
                }
            }
        }catch(NullPointerException e){
            setTop3(newTop3);//aggiorno
            return true;
        }
        //non ci sono stati cambiamenti
        return false;
    }

    private void setTop3(List<User> top3) {
        this.top3 = top3;
    }

    public String getClassifica() {
        StringBuilder stringBuilder = new StringBuilder();
//        System.out.println("[DATABASE] Classifica:");
        for (User tmp : classifica) {
            stringBuilder.append(tmp.toString3());
        }
        return stringBuilder.toString();
    }

//    private void println(String s){ System.out.println("[Database] " + s); }
}