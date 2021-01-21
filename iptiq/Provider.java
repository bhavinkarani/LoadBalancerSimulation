package extra.challenge.iptiq;


import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

final class Provider {
    public static final int MAX_REQUESTS = 10;
    @Getter
    final private String name;
    @Getter
    final private String id;
    @Getter
    @Setter // this attribute is just for the simulation to make it unavailable when required
    private boolean isAlive;

    ExecutorService executor;

    public Provider(String name, boolean isAlive){
        this.name = name;
        this.isAlive = isAlive; //
        this.id = UUID.randomUUID().toString();
        executor = Executors.newFixedThreadPool(MAX_REQUESTS);
        // check for max pool size , elasticity of threads with executor service
    }
    public String returnValue() {
        System.out.println("received request"); // todo: remove IO processes
        try {
            Thread.sleep(2000); // may be add random amount of sleep for doing some work;
        } catch (InterruptedException e) {
            e.printStackTrace(); // remove stack trace // todo: addmeaningful logging
        }
        System.out.println("returning response");
        return "returning from provider ID:"+ id +" and provider name :"+name ;
    }
    public boolean isAlive(){
        return isAlive;
    }

    public Future<String> get(){
        Future<String> returnValue = executor.submit(()->returnValue() );
        return returnValue;
    }

}
