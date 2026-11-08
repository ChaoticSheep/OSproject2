import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.Random;

public class Bank {
    static public Semaphore door = new Semaphore(2); // Allow 2 customers in the bank at a time
    static BlockingQueue<CustomerRequest> line = new LinkedBlockingQueue<>();
    static Semaphore tellerReady = new Semaphore(0); 
    

    static public class CustomerRequest {
        int customerId;
        String transactionType;
        boolean shutdown = false;
        volatile int tellerId = -1; // Teller ID assigned to the customer

        Semaphore goingBank = new Semaphore(0); //customer going to bank
        Semaphore enterBank = new Semaphore(0); //signals customer can approach teller
        Semaphore getInLine = new Semaphore(0); //signals customer can get in line
        Semaphore introduced = new Semaphore(0); // customer signals they've introduced themselves
        Semaphore selectingTeller = new Semaphore(0); // customer signals they've asked their question
        Semaphore asked = new Semaphore(0); // customer signals they've asked their question
        Semaphore completed = new Semaphore(0);
        Semaphore customerLeft = new Semaphore(0);

        CustomerRequest(int id, String transactionType) {
            this.customerId = id;
            this.transactionType = transactionType;
        }
        // Used to stop teller threads
        CustomerRequest() {
            shutdown = true;
            customerId = -1;
            transactionType = "";
        }
    }
    
    static class Customer extends Thread {
        int id;
        String transactionType;

        Customer(int id, String transactionType) {
            this.id = id;
            this.transactionType = transactionType;
        }

        public void run() {
            System.out.println("Customer []:" + id + " wants to perform a "+ transactionType + " transaction");
            try {
                door.acquire(); // Wait to enter the bank
                
                Thread.sleep(100); // Simulate time taken to perform transaction
                CustomerRequest request = new CustomerRequest(id, transactionType);

                //bank
                request.goingBank.release(); 
                request.enterBank.acquire();
                System.out.println("Customer []:" + id + " going to bank.");
                request.enterBank.release();
                request.enterBank.acquire();
                System.out.println("Customer []:" + id + "entering bank.");
                //line
                request.getInLine.release(); 
                request.getInLine.acquire(); 
                line.put(request); 
                System.out.println("Customer []:" + id + " getting in line.");
                //teller
                request.selectingTeller.release();
                request.selectingTeller.acquire();                
                //System.out.println("Customer [Teller + " + Teller.id + " ]:" + id + " selecting a teller.");

                //teller selects self and signals customer to introduce themselves
                
                //introduce
                request.introduced.release(); 
                request.asked.acquire(); // Wait for teller to ask their question
                System.out.println("Customer []: " + id + " wants to perform a " + transactionType + " transaction.");
                door.release(); // Leave the bank
            } catch (InterruptedException e) {
                System.err.println("Error in Customer []:" + id + ": " + e);
            }
        }
    }

    static class Teller extends Thread {
    int id;
    Semaphore safe = new Semaphore(2); // Lock for safe
    Semaphore manager = new Semaphore(1); // Lock for manager

    Teller(int id) {
        this.id = id;
    }
    
    public void run() {
        
            System.out.println("Teller []:" + id + " ready to serve");
            System.out.println("Teller []:" + id + " waiting for a customer");
            tellerReady.release(); // Signal that teller is ready to serve

            while(true){
                try {
                    CustomerRequest request = line.take(); // Wait for a customer to enter the line
                    //System.out.println("Teller []:" + id + " serving Customer []:" + request.customerId);

                } catch (Exception e) {
                System.err.println("Error in Teller []:" + id + ": " + e);

            }
        }
    }
}

static public void main(String[] args)
{
    int customerCount = 50;
    int tellerCount = 3;
    int random = (int) (Math.random() * 2); // Randomly assign a transaction type
    String transactionType = random == 0 ? "deposit" : "withdrawal";
    Teller[] tellers = new Teller[tellerCount];
    Customer[] customers = new Customer[customerCount];

    for(int i = 0; i < tellerCount; i++)
    {
        Teller teller = new Teller(i);
        teller.start();
        try {
            // Wait for the teller to signal that they are ready
            tellerReady.acquire();
        } catch (InterruptedException e) {
            System.err.println("Error waiting for teller " + i + " to be ready: " + e);
        }
    
        
    }
    for(int i = 0; i < customerCount; i++)
    {
        Customer customer = new Customer(i, transactionType);
        customer.start();
    
        
    }
}
}
