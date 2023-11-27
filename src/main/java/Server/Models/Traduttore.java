package Server.Models;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.google.gson.stream.JsonReader;

public class Traduttore {
    public static String ind = "https://api.mymemory.translated.net";

    public static String translate(String word){
        try {
            String encQuery = URLEncoder.encode(word, "UTF-8");
            URL url = new URL(ind + String.format("/get?q=%s&langpair=en|it", encQuery));
            System.out.println("MYURL=" + url);
            // Apro una connessione verso il server e invio la richiesta GET
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            //Leggo il risultato attraverso un JsonReader
            //che andra' a leggere i valori dei campi che mi interessano
            InputStream in = connection.getInputStream();
            InfoTraduzione traduzione = readThisObject(word, in);
            if (traduzione != null) System.out.println("[TRADUTTORE]" +traduzione.getTranslation());
            else System.out.println("[TRADUTTORE] qualcosa e' andato storto nella traduzione");
            //chiudo la connessione
            connection.disconnect();
            return traduzione.getTranslation();
        }catch(Exception e) {
            System.out.println("[TRADUTTORE] qualcosa e' andato storto nella traduzione");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @return un oggetto della classe InfoTraduzione o null se viene sollevata un'IOException
     */
    private static InfoTraduzione readThisObject(String segment, InputStream in){
        try(JsonReader reader = new JsonReader(new BufferedReader(new InputStreamReader(in)))){
            String traduzione = null;

            reader.beginObject();//consume '{' <opening brace>

            while (reader.hasNext()) {
                String campo = reader.nextName();
                if(campo.contentEquals("responseData")){
                    //System.out.println(campo +" In attesa di scoprirlo");
                    traduzione = readTranslatedText(reader);
                }
                else {reader.skipValue();}
            }

            reader.endObject();// consume '}' <closing brace>
            return new InfoTraduzione(segment, traduzione);
        }catch(IOException e) {
            System.err.println("Qualcosa non va nella lettura dell'oggetto");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @return il valore del campo TranslatedText o null se viene sollevata un' IOException
     */
    private static String readTranslatedText(JsonReader reader){
        String translatedText = null;
        try {
            reader.beginObject();//consume '{'

            while(reader.hasNext()) {
                String campo = reader.nextName();
                //System.out.println("sono dentro a responseData campo="+campo);
                if(campo.equals("translatedText")) {
                    translatedText = reader.nextString();
                }
                else {
                    reader.skipValue();
                }
            }

            reader.endObject();//consume '}'

            return translatedText;
        }catch(IOException e) {
            System.err.println("Qualcosa non va nella lettura del campo 'translatedText'");
            e.printStackTrace();
            return null;
        }
    }


}
