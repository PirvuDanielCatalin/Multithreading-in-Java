package Manager_Pkg;

import Cofetar_Pkg.Cofetar;
import Comanda_Pkg.Comanda;
import Livrator_Pkg.Livrator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Manager extends Thread {
    private final ArrayDeque<Comanda> ListaComenzi_Agent_Manageri;
    private final ThreadPoolExecutor Cofetari_Blat_Executor;
    private final ThreadPoolExecutor Cofetari_Crema_Executor;
    private final ThreadPoolExecutor Cofetari_Decoratiuni_Executor;
    private final ArrayDeque<Comanda> ListaComenzi_Manageri_Livratori;
    private final Lock Lacat;
    private final Condition LivratorLiber;
    private final Condition ComandaGataDeLivrare;
    private final ArrayList<String> ElementeMarcate;
    private final ArrayList<String> Threaduri;
    private final ArrayList<String> Livratori;
    private final ArrayList<String> Manageri;

    public Manager(ArrayDeque<Comanda> listaComenzi_Agent_Manageri, ThreadPoolExecutor cofetari_Blat_Executor, ThreadPoolExecutor cofetari_Crema_Executor, ThreadPoolExecutor cofetari_Decoratiuni_Executor, ArrayDeque<Comanda> listaComenzi_Manageri_Livratori, Lock lacat, Condition livratorLiber, Condition comandaGataDeLivrare, ArrayList<String> elementeMarcate) {
        ListaComenzi_Agent_Manageri = listaComenzi_Agent_Manageri;

        Cofetari_Blat_Executor = cofetari_Blat_Executor;
        Cofetari_Crema_Executor = cofetari_Crema_Executor;
        Cofetari_Decoratiuni_Executor = cofetari_Decoratiuni_Executor;

        ListaComenzi_Manageri_Livratori = listaComenzi_Manageri_Livratori;

        Lacat = lacat;
        LivratorLiber = livratorLiber;
        ComandaGataDeLivrare = comandaGataDeLivrare;

        ElementeMarcate = elementeMarcate;
        Threaduri = new ArrayList<String>(Arrays.asList("M1", "M2", "M3", "M4", "CB", "CC", "CD", "L1", "L2", "L3", "L4", "L5"));
        Livratori = new ArrayList<String>(Arrays.asList("L1", "L2", "L3", "L4", "L5"));
        Manageri = new ArrayList<String>(Arrays.asList("M1", "M2", "M3", "M4"));
    }

    public void run() {
        while (true) {
            try {
                consume();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void consume() throws InterruptedException {
        Comanda comanda;
        synchronized (ListaComenzi_Agent_Manageri) {
            while (ListaComenzi_Agent_Manageri.isEmpty()) {
                ListaComenzi_Agent_Manageri.wait();
            }

            // Managerul scrie pe System.err cand preia o comanda
            comanda = ListaComenzi_Agent_Manageri.removeFirst();
            if (!comanda.getNume().equals("STOP")) {
                System.err.println("Managerul " + Thread.currentThread().getName() + " a preluat comanda " + comanda.getNume());
            }


            ListaComenzi_Agent_Manageri.notifyAll();
        }

        if (comanda.getNume().equals("STOP")) {
            synchronized (ElementeMarcate) {
                // Adaugam managerul curent in lista elementelor marcate
                ElementeMarcate.add(Thread.currentThread().getName());
                ElementeMarcate.notifyAll();

                while (!ElementeMarcate.containsAll(Threaduri)) {

                    // Daca toti managerii sunt marcati, incepem sa marcam cofetarii si livratorii
                    if (ElementeMarcate.containsAll(Manageri)) {

                        // Marcam cofetarii
                        if (!ElementeMarcate.containsAll(new ArrayList<String>(Arrays.asList("CB", "CC", "CD")))) {
                            ElementeMarcate.add("CB");
                            ElementeMarcate.add("CC");
                            ElementeMarcate.add("CD");
                        }

                        while (!ElementeMarcate.containsAll(Livratori)) {
                            // Cat timp livratorii sunt activi, astept sa pot pune comenzi de oprire pe lista Manageri - Livratori
                            Lacat.lock();
                            try {
                                // Astept ca lista Manageri - Livratori sa poate primi o comanda de oprire
                                while (ListaComenzi_Manageri_Livratori.size() > 0)
                                    LivratorLiber.await();

                                // Pun comanda de oprire si notific pt ca un livrator sa o poata lua
                                ListaComenzi_Manageri_Livratori.addLast(comanda);
                                ComandaGataDeLivrare.signal();
                            } finally {
                                Lacat.unlock();
                            }
                        }
                    } else {
                        // Astept ca urmatorul manager sa fi preluat o comanda de oprire
                        ElementeMarcate.wait();
                    }
                }
            }
        } else {

            // Trimite catre cofetari sa faca cele trei parti
            Future<String> rezultat_blat = Cofetari_Blat_Executor.submit(new Cofetar(comanda.getX()), "Done");
            Future<String> rezultat_crema = Cofetari_Crema_Executor.submit(new Cofetar(comanda.getY()), "Done");
            Future<String> rezultat_decoratiuni = Cofetari_Decoratiuni_Executor.submit(new Cofetar(comanda.getZ()), "Done");

            // Managerul curent asteapta ca cele trei parti ale tortului sa fie gata
            while (!rezultat_blat.isDone() && !rezultat_crema.isDone() && !rezultat_decoratiuni.isDone()) {
            }

            // Tortul este gata si se poate livra
            Lacat.lock();
            try {
                // Managerul asteapta sa fie un livrator liber
                while (ListaComenzi_Manageri_Livratori.size() > 0)
                    LivratorLiber.await();

                // Exista livrator liber si managerul plaseaza comanda spre livrare
                System.err.println("Managerul " + Thread.currentThread().getName() + " a dat comanda " + comanda.getNume() + " spre livrare.");
                ListaComenzi_Manageri_Livratori.addLast(comanda);
                ComandaGataDeLivrare.signal();
            } finally {
                Lacat.unlock();
            }
        }
    }
}
