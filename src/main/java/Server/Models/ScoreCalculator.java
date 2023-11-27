package Server.Models;

/**
 * Classe Java per il calcolo del Wordle Average Score (WAS).
 *
 * Per calcolare questo punteggio ho bisogno di 3 cose:
 *
 * 1)   il numero massimo di tentativi che posso fare per ogni parola;
 * 1)   il numero totale di partite giocate (vinte e perse);
 * 2)   la guess distribution, ovvero un vettore di lunghezza pari al
 *      numero massimo di tentativi. La posizione i-esima del vettore
 *      contiene il numero di partite vinte con esattamente i tentativi.
 *
 * Per calcolare il punteggio, moltiplico la posizione i-esima del vettore
 * per i stesso. Sommo tutti i prodotti e divido per il numero totale
 * di partite giocate. Nella somma, tengo conto anche delle parole non
 * indovinate (partite perse). In questo caso, considero un numero
 * di tentativi pari al numero massimo + 1.
 *
 * @author Matteo Loporchio
 */
public class ScoreCalculator {
    /**
     * Numero massimo di tentativi per una singola parola.
     * Questa e' una costante globale del gioco.
     * Nella versione originale di Wordle i tentativi sono 6,
     * mentre nel nostro progetto sono 12.
     */
    public static final int maxAttempts = 12;

    /**
     * Calcola il Wordle Average Score (WAS) dell'utente.
     * @param numPlayed numero totale di partite giocate
     * @param guessDist guess distribution dell'utente
     * @return il Wordle Average Score dell'utente
     */
    public static double computeScore(int numPlayed, int[] guessDist) {
        int sum = 0, numGuessed = 0;
        for (int i = 0; i < guessDist.length; i++) {
            sum += (i + 1) * guessDist[i];
            numGuessed += guessDist[i];
        }
        // Per le parole non indovinate (partite perse),
        // considero un numero di tentativi pari a 13 (ovvero maxAttempts + 1).
        sum += (maxAttempts + 1) * (numPlayed - numGuessed);
        return ((double) sum / (double) numPlayed);
    }
}
