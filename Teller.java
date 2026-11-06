import java.util.concurrent.Semaphore;

public class Teller {
    static public Semaphore gLock = new Semaphore(1);
    static public int gCount = 0;
    
    static public class ThreadCode extends Thread {
        int id;

        ThreadCode(int id) {
            this.id = id;
        }

        public void run() {
            try {
                gLock.acquire(); //waits for the lock to be available
                //signal customer that teller is available
                System.out.println("Teller " + id + " []: ready to serve");

                gCount++;
                gLock.release();
            } catch (Exception e) {
                System.err.println("Error in Thread " + id + ": " + e);
            }
        }
    }

    

    static public void main(String[] args) {
        ThreadCode[] threads = new ThreadCode[5];
        //create and start 3 threads
        for (int i = 0; i < 3; i++) {
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
