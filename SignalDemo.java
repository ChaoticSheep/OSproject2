import java.util.concurrent.Semaphore;
import java.util.Random;

public class SignalDemo
{
    static int shared = 0;

    // Extend Thread Class For one type of thread, the sending thread
    public static class SendingThread extends Thread{
        Semaphore semaphore = null;
        Random rand = new Random(); 

        // In this example, we pass the semaphore as a parameter
        // This will probably not work as well with your project, because you need
        // Too many semaphores
        public SendingThread(Semaphore semaphore){
            this.semaphore = semaphore;
        }

        //Implement behavior of this thread type
        public void run(){
            try {
                while(true){
                    //do something, then signal
                    int tmp = rand.nextInt(1000);
                    System.out.println("Send: " + tmp);
                    shared = tmp;

                    // Random number is ready, inform Receiver by signally semaphore1
//                    this.semaphore1.release();
                    // We do not want to write over the shared variable while the receiver is using it
                    // wait for a signal from the receiver
//                    this.semaphore2.acquire();

                }
            } catch (Exception e)
            {
                System.out.println(e);
            }
        }
    }

    // Extend Thread Class For one type of thread, the receiving thread
    public static class ReceivingThread extends Thread{
        Semaphore semaphore1 = null;
        Semaphore semaphore2 = null;

        // In this example, we pass the semaphore as a parameter
        // This will probably not work as well with your project, because you need
        // Too many semaphores
        public ReceivingThread(Semaphore semaphore1, Semaphore semaphore2){
            this.semaphore1 = semaphore1;
            this.semaphore2 = semaphore2;
        }

        //Implement behavior of this thread type
        public void run(){
            try{
                while(true){
//                    this.semaphore1.acquire(); //Wait for sender to have number ready
                    //Do Something
                    System.out.println("Received: " + shared);
                    //We are done with the shared number, signal the sender to tell it
                    //to continue
 //                   this.semaphore2.release();
                }
        } catch (Exception e)
        {
            System.out.println(e);
        }
    }
}

}
