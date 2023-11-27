package Server;

import Server.ServerMainWordle;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ThreadLocalRandom;

public class WordExtractorThread implements Runnable{
    private   String filePath ;
    // Definisco la lunghezza della singola stringa pari a 11 byte (10 caratteri + 1 carattere '\n').
    public static final int elementLength = 11;
    public WordExtractorThread(String fileName) {
        this.filePath = fileName;
    }


    public void run(){
        // Apro il file contenente le parole in modalita' lettura.
        try (RandomAccessFile f = new RandomAccessFile(filePath, "r")) {
            // Calcolo il numero di stringhe contenute nel file dividendo la lunghezza del file (in byte)
            // per la lunghezza di una singola stringa.
            final int numElements = ((int) f.length()) / elementLength;
//            System.out.printf("The file contains %s strings.\n", numElements);
            // Stampo una parola a caso.
            String newRandomWord = getRandomWord(f, numElements);
            System.out.printf("[WordExtractorThread] Nuova parola estratta: '%s'\n", newRandomWord);
            ServerMainWordle.setActualSecretWord(newRandomWord);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Restituisce una parola a caso fra quelle presenti nel dizionario.
     * @param f file del dizionario
     * @param numElements numero di stringhe contenute nel dizionario
     * @return una parola casuale del dizionario
     * @throws IOException se qualcosa va storto durante la lettura dal file
     */
    public static String getRandomWord(RandomAccessFile f, int numElements) throws IOException {
        // Genero un valore a caso fra 0 e numElements-1.
        int riga = ThreadLocalRandom.current().nextInt(0, numElements);// 0 <= x < 30824
        // In questo array memorizzo i byte letti dal file.
        byte[] resultBytes = new byte[elementLength-1];
        // Vado all'offset corrispondente all'interno del file.
        f.seek(riga * elementLength);
        // Leggo esattamente elementLength-1 byte, tralasciando il '\n' finale di ogni stringa.
        f.read(resultBytes);
        // Creo una nuova stringa con i byte letti e mi assicuro che questi vengano interpretati secondo la codifica UTF-8.
        return new String(resultBytes, "UTF-8");
    }

}
