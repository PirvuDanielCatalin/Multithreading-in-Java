package Livrator_Pkg;

import Comanda_Pkg.Comanda;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Livrator extends Thread {
    private final ArrayDeque<Comanda> ListaComenzi_Manageri_Livratori;
    private final Lock Lacat;
    private final Condition LivratorLiber;
    private final Condition ComandaGataDeLivrare;
    private final ArrayList<String> ElementeMarcate;

    public Livrator(ArrayDeque<Comanda> listaComenzi_Manageri_Livratori, Lock lacat, Condition livratorLiber, Condition comandaGataDeLivrare, ArrayList<String> elementeMarcate) {
        ListaComenzi_Manageri_Livratori = listaComenzi_Manageri_Livratori;

        Lacat = lacat;
        LivratorLiber = livratorLiber;
        ComandaGataDeLivrare = comandaGataDeLivrare;

        ElementeMarcate = elementeMarcate;
    }

    public void run() {
        while (true) {
            try {
                consume();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                Thread.currentThread().stop();
            }
        }
    }

    private void consume() throws InterruptedException {
        Comanda comanda;
        Lacat.lockInterruptibly();
        try {
            while (ListaComenzi_Manageri_Livratori.isEmpty())
                ComandaGataDeLivrare.await();

            // Livratorul preia comanda de la manager
            comanda = ListaComenzi_Manageri_Livratori.removeFirst();

            if (comanda.getNume().equals("STOP")) {
                ElementeMarcate.add(Thread.currentThread().getName());

                LivratorLiber.signal();
            } else {
                System.out.println("Livratorul " + Thread.currentThread().getName() + " a preluat comanda " + comanda.getNume());

                // Timpul de livrare a comenzii si de intoarcere inapoi la cofetarie
                Thread.sleep(comanda.getT());

                LivratorLiber.signal();
            }
        } finally {
            Lacat.unlock();
        }
    }
}
