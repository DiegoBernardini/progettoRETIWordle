package Server.Models;

import java.io.IOException;
import java.io.RandomAccessFile;

public class BinarySearch {
    private static int wordLen = 11; // parole di lunghezza 10+ '\n' = 11


    /**
     * Esegue una ricerca binaria della chiave specificata nel file in input.
     * @param f riferimento al file
     * @param key chiave cercata
     * @return se la chiave viene trovata, restituisce la sua posizione nel file. Altrimenti restituisce -1.
     * @throws IOException in caso di errore di lettura dal file
     */
    public static int binarySearch(RandomAccessFile f, String key) throws IOException {
        final int numElements = ((int) f.length()) / wordLen;
//		System.out.println(f.length());
//        System.out.println("TOT elements" +numElements);
        int lower = 0, upper = numElements - 1, mid;
        while (lower <= upper) {
            mid = (lower + upper) / 2;
            f.seek(mid * wordLen);//sposto il cursore
            String tmp = f.readLine();//leggo una parola (il carattere '\n' non c'e' in tmp)
            int notFound = key.compareTo(tmp);//check
            if(notFound == 0) return mid;//case 1: parola trovata
            else if (notFound < 0) upper = mid -1;//case 2:devo salire nel File
            else lower = mid + 1;//case 3: devo scendere nel File
        }
        return -1;
    }
}
