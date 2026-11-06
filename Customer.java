import java.util.concurrent.Semaphore;
import java.util.Random;

public class Customer {
    static public Semaphore gLock = new Semaphore(1);
    static public int gCount = 0;
    
    static public class ThreadCode extends Thread {
        int id;
        int type = (int) (Math.random() * 2); // Randomly assign a transaction type 

        ThreadCode(int id) {
            this.id = id;
        }

        public void run() {
            try {
                gLock.acquire(); //waits for the lock to be available
                
                System.out.println("Customer " + id + " []: wants to perform a " + (type == 0 ? "deposit" : "withdrawal") + " transaction");
                if(type == 0) {
                    // Perform deposit transaction
                    sendingThread.sendDepositRequest(id);
                } else {
                    // Perform withdrawal transaction
                    sendingThread.sendWithdrawalRequest(id);
                }
                //signal customer that teller is available

                gCount++;
                gLock.release();
            } catch (Exception e) {
                System.err.println("Error in Thread " + id + ": " + e);
            }
        }
    }

    

    static public void main(String[] args) {
        ThreadCode[] threads = new ThreadCode[50];
        //create and start 3 threads
        for (int i = 0; i < 50 ; i++) {
            threads[i] = new ThreadCode(i);
            threads[i].start(); 
            try {
                //waits till the thread is done before continuing
                threads[i].join();
            } catch (InterruptedException e) {
                System.err.println("Error joining thread " + i + ": " + e);
            }
        }
    }
}
