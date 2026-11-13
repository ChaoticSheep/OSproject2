import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Bank {
    static public Semaphore door = new Semaphore(2); // Allow 2 customers in the bank at a time
    static BlockingQueue<CustomerRequest> line = new LinkedBlockingQueue<>();
    static Semaphore tellerReady = new Semaphore(0); 
    static Semaphore customerReady = new Semaphore(0); 
    static public Semaphore manager = new Semaphore(1); // Allow only 1 teller to access the manager at a time
    static public Semaphore safe = new Semaphore(2); // Allow only 2 tellers to access the safe at a time
    static public class CustomerRequest {
        int customerId;
        String transactionType;
        boolean shutdown = false;
        volatile int tellerId = -1; // Teller ID assigned to the customer

        Semaphore tellerAssigned = new Semaphore(0); //signals teller has been assigned
        Semaphore introduced = new Semaphore(0); // customer signals they've introduced themselves
        Semaphore transactionRequested  = new Semaphore(0);
        Semaphore transactionProvided = new Semaphore(0);
        Semaphore transactionComplete = new Semaphore(0);
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
            System.out.println("Customer " + id + " []: wants to perform a "+ transactionType + " transaction");
            customerReady.release(); // Signal that customer is ready to perform transaction
            
            
                try {
                    int delay = ThreadLocalRandom.current().nextInt(0, 101); // Random delay between 1 and 5 seconds
                    Thread.sleep(delay); // Simulate time taken to perform transaction
                    // Enter bank
                    System.out.println("Customer " + id + " []: going to bank");     
                    Bank.door.acquire();
                    System.out.println("Customer " + id + " []: entering bank.");
                    

                    //get in line
                    CustomerRequest request = new CustomerRequest(id, transactionType);
                    System.out.println("Customer " + id + " []: getting in line.");
                    Bank.line.put(request); // Wait for a teller to be assigned
                    request.tellerAssigned.acquire(); // Wait for teller to be assigned
                    System.out.println("Customer " + id + " [Teller "+ request.tellerId + " ]: selects teller");
                    
                    //introduces
                    System.out.println("Customer " + id + " [Teller "+ request.tellerId + " ]: introduces themselves");
                    request.introduced.release(); // Signal that customer has introduced themselves
                    request.transactionRequested.acquire(); // Wait for teller to ask their question
                    System.out.println("Customer " + id + " [Teller "+ request.tellerId + " ]: asks for " + transactionType + " transaction");
                    request.transactionProvided.release(); // Signal that customer has provided their transaction request
                    request.transactionComplete.acquire(); // Wait for teller to complete transaction

                    //leaving bank
                    System.out.println("Customer " + id + " [Teller "+ request.tellerId + " ]: leaves teller");
                    request.customerLeft.release(); // Signal that customer has left
                    System.out.println("Customer " + id + " []: goes to door");
                    System.out.println("Customer " + id + " []: leaves the bank.");
                    Bank.door.release();
                } catch (InterruptedException e) {
                    System.err.println("Error in Customer " + id + ": " + e);
                }
            
        }
    }
    static class Teller extends Thread {
    int id;

    Teller(int id) {
        this.id = id;
    }
    
    public void run() {
        
            System.out.println("Teller []:" + id + " ready to serve");
            System.out.println("Teller []:" + id + " waiting for a customer");
            tellerReady.release(); // Signal that teller is ready to serve

            
            try {
                while(true) {
                    CustomerRequest request = Bank.line.take(); // Wait for a customer to enter the line
                    if (request.shutdown) {
                        break; // Exit the loop if shutdown signal is received
                    }
                    request.tellerId = id; // Assign teller ID to the customer request
                    request.tellerAssigned.release(); // Signal that teller has been assigned to the customer
                    request.introduced.acquire(); // Wait for customer to introduce themselves
                    System.out.println("Teller " + id + " [Customer " + request.customerId + "]: asks for transaction");
                    request.transactionRequested.release(); // Signal that teller has asked for transaction
                    request.transactionProvided.acquire(); // Wait for customer to provide transaction request

                    //manager permission
                    if(request.transactionType.equals("withdrawal")) {
                        System.out.println("Teller " + id + " [Customer " + request.customerId + "]: going to manager");
                        Bank.manager.acquire(); // Acquire manager permission for withdrawal
                        try {
                            System.out.println("Teller " + id + " [Customer " + request.customerId + "]: getting manager's permission");
                            int managerDelay = ThreadLocalRandom.current().nextInt(5, 31); // Random delay between 1 and 5 seconds
                            Thread.sleep(managerDelay); // Simulate time taken by manager
                            System.out.println("Teller " + id + " [Customer " + request.customerId + "]: got manager's permission");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            Bank.manager.release(); // Release manager permission
                        }
                        Bank.safe.acquire(); // Acquire safe permission for withdrawal
                        try {
                            System.out.println("Teller " + id + " [Customer " + request.customerId + "]: going to safe");
                            int safeDelay = ThreadLocalRandom.current().nextInt(5, 31); // Random delay between 1 and 5 seconds
                            Thread.sleep(safeDelay); // Simulate time taken by safe
                        } finally {
                            Bank.safe.release(); // Release safe permission
                        }
                        System.out.println("Teller " + id + " [Customer " + request.customerId + "]: transaction complete");
                        
                        
                    }
                    else if (request.transactionType.equals("deposit")) {
                        System.out.println("Teller " + id + " [Customer " + request.customerId + "]: handling deposit transaction");

                        int depositDelay = ThreadLocalRandom.current().nextInt(5, 31);
                        Thread.sleep(depositDelay);

                        System.out.println("Teller " + id + " [Customer " + request.customerId + "]: finishes deposit transaction.");
                    }
                    request.transactionComplete.release(); // Signal that transaction is complete
                    System.out.println("Teller " + id + "[]: waiting for customer");
                    request.customerLeft.acquire(); // Wait for customer to leave
                    

                } System.out.println("Teller " + id + " []: leaving for the day");

            }catch (Exception e) {
                System.err.println("Error in Teller []:" + id + ": " + e);
                Thread.currentThread().interrupt();
        }
    }
}

static public void main(String[] args)
{
    int customerCount = 50;
    int tellerCount = 3;
    Teller[] tellers = new Teller[tellerCount];
    Customer[] customers = new Customer[customerCount];

    for(int i = 0; i < tellerCount; i++)
    {
        tellers[i] = new Teller(i);
        tellers[i].start();
        try {
            // Wait for the teller to signal that they are ready
            tellerReady.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Error waiting for teller " + i + " to be ready: " + e);
            return;
        }
        
    
        
    }
    for(int i = 0; i < customerCount; i++)
    {
        int random = (int) (Math.random() * 2); // Randomly assign a transaction type
        String transactionType = random == 0 ? "deposit" : "withdrawal";
        customers[i] = new Customer(i, transactionType);
        customers[i].start();
        try {
            // Wait for the customer to signal that they are ready
            customerReady.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        
        
    }
    // Wait for every customer to finish.
    for (int i = 0; i < customerCount; i++) {
        try {
            if(customers[i] != null) {
                customers[i].join();
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    // Tell all tellers to shut down.
    for (int i = 0; i < tellerCount; i++) {
        try {
            Bank.line.put(new CustomerRequest());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    // Wait for tellers to finish.
    for (int i = 0; i < tellerCount; i++) {
        try {
            tellers[i].join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    System.out.println("The bank closes for the day.");
}
}
