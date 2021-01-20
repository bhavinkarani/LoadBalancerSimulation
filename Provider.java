package extra.challenge;


import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

final class Provider {
    @Getter
    final private String name;
    @Getter
    @Setter
    final private String id;
    @Getter
    @Setter // this attribute is just for the simulation to make it unavailable when required
    private boolean isAlive;

    ExecutorService executor;

    /*public Provider(String name){
        this.name = name;
        this.id = UUID.randomUUID().toString();
    }*/
    public Provider(String name, boolean isAlive){
        this.name = name;
        this.isAlive = isAlive; //
        this.id = UUID.randomUUID().toString();
        executor = Executors.newFixedThreadPool(10); // todo: magic number
    }
    public String returnValue() {
        System.out.println("received request");
        try {
            Thread.sleep(2000); // may be add random amount of sleep for doing some work;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("returning response");
        return "returning from provider ID:"+ id +" and provider name :"+name ;
    }
    public boolean isAlive(){
        return isAlive;
    }

    public boolean deRegister(){
        // de-register and update isAlive to false;
        this.isAlive = false;
        return true;
    }

   /* public void register(){
        // reregister and update isAlive to true;
        this.isAlive = true;
    }*/


    public Future<String> get(){
        Future<String> returnValue = executor.submit(()->returnValue() );
        return returnValue;
    }

}
