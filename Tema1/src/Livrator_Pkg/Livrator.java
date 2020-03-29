package Livrator_Pkg;

import Comanda_Pkg.Comanda;

import java.util.ArrayList;
import java.util.concurrent.SynchronousQueue;

public class Livrator extends Thread {
    private final SynchronousQueue<Comanda> ListaComenzi_Manageri_Livratori;
    private final ArrayList<String> ElementeMarcate;

    public Livrator(SynchronousQueue<Comanda> listaComenzi_Manageri_Livratori, ArrayList<String> elementeMarcate) {
        ListaComenzi_Manageri_Livratori = listaComenzi_Manageri_Livratori;
        ElementeMarcate = elementeMarcate;
    }

    public void run() {
        while (true) {
            try {
                consume();
            } catch (Exception ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void consume() throws Exception {
        Comanda comanda;
        if (!ElementeMarcate.contains(Thread.currentThread().getName())) {
            comanda = ListaComenzi_Manageri_Livratori.take();
            // Fiind folosita o coada SynchronousQueue call-ul precedent este unul blocant si asteapta pana cand un manager trimite comanda

            if (comanda.getNume().equals("STOP")) {
                ElementeMarcate.add(Thread.currentThread().getName());
            } else {
                System.out.println("Livratorul " + Thread.currentThread().getName() + " a preluat comanda " + comanda.getNume());

                // Timpul de livrare a comenzii si de intoarcere inapoi la cofetarie
                Thread.sleep(comanda.getT());
            }
        }
    }
}
