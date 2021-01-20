package extra.challenge;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * Simulating real world
 * scenario means however that it has to be working properly and effectively in all
 * scenario that can happen in real life (eg. handling parallel requests, managing edge
 * cases etc.)
 *
 * Step 1 – Generate provider
 *
 * Generate a Provider that, once invoked on his
 * get() method, retrieve an unique identifier
 * (string) of the provider instance
 *
 * Step 2 – Register a list of providers
 *
 * Register a list of provider instances to the
 * Load Balancer - the maximum number of
 * providers accepted from the load balancer is
 * 10
 *
 * Step 3 – Random invocation
 *
 * Develop an algorithm that, when invoking multiple
 * times the Load Balancer on its get() method,
 * should cause the random invocation of the get()
 * method of any registered provider instance.
 *
 * Step 4 – Round Robin invocation
 *
 * Develop an algorithm that, when invoking multiple
 * times the Load Balancer on its get() method,
 * should cause the round-robin (sequential)
 * invocation of the get() method of the registered
 * providers.
 *
 * Step 5 – Manual node exclusion / inclusion
 *
 * Develop the possibility to exclude / include a
 * specific provider into the balancer
 *
 * Step 6 – Heart beat checker
 *
 * The load balancer should invoke every X seconds
 * each of its registered providers on a special
 * method called check() to discover if they are alive
 * – if not, it should exclude the provider node from
 * load balancing.
 *
 * Step 7 – Improving Heart beat checker
 *
 * If a node has been previously excluded from the
 * balancing it should be re-included if it has
 * successfully been “heartbeat checked” for 2
 * consecutive times
 */

class LoadBalancer {
    private static LoadBalancer loadBalancer = null;
    private static final int MAX_PROVIDERS_POSSIBLE = 10;
    private static final int MAX_PARALLEL_COUNT_PER_PROVIDER = 10;
    private static final int RETRY_COUNT = 5;
    private static int roundRobinIndex = 0;
    private volatile AtomicInteger requestsInProgress = new AtomicInteger(0);
    Map<String, Provider> providers = new ConcurrentHashMap<>();
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    Map<String, Integer> token = new ConcurrentHashMap<>();
    LinkedList<String> providerNames = new LinkedList<>();
    Lock providerNamesListLoack = new ReentrantLock();

    private LoadBalancer(){ }

    public static LoadBalancer getInstance(){
        if(loadBalancer == null){
            return new LoadBalancer();
        }
        return loadBalancer;
    }
    /**
     * public get method which return future
     * @return Future of string response from providers
     */
    public Future<String> get()  {
        System.out.println("finding provider for request");
        if (requestsInProgress.get() >= providers.size() * MAX_PARALLEL_COUNT_PER_PROVIDER) {
            return ConcurrentUtils.constantFuture("Providers not available. Please try after some time");
        }
        requestsInProgress.getAndIncrement();
        Future<String> returnValue = executorService.submit(() -> callProvider());
        return returnValue;
    }

    private String callProvider() throws ExecutionException, InterruptedException {
        //can use factory pattern / proxy pattern for provider.
        //Provider provider = getNextRandomProvider();
        Provider provider = getRoundRobinProvider();
        if(provider!=null){
            //System.out.println("calling provider now : "+ provider.getName());
            token.put(provider.getName(), token.get(provider.getName())-1); //give away one token
            Future<String> returned = provider.get();
            while(!returned.isDone());
            token.put(provider.getName(), token.get(provider.getName())+1); // add back token so that other request can use it
            requestsInProgress.getAndDecrement();
            return returned.get();
        }else{
            return null;
        }
    }

    private Provider getRoundRobinProvider() {
        providerNamesListLoack.lock();
        String providerName;
        try{
            providerName = providerNames.get(roundRobinIndex);
            if(providerNames.size() <= roundRobinIndex+1){ //calculate next round robin index
                roundRobinIndex =0;
            }else{
                roundRobinIndex++;
            }
        }finally {
            providerNamesListLoack.unlock();
        }
        return providers.get(providerName);
    }

    private Provider getNextRandomProvider() {
        int i =0 ;
        while(i < RETRY_COUNT) {
            List<String> names = new ArrayList<>(providers.keySet());
            Random random = new Random();
            Provider provider = providers.get(names.get( ((Math.abs(random.nextInt()) % providers.size()))));
            if (token.get(provider.getName()) > 0 ) { //also check the availability of tokens
                //token.put(provider.getName(), token.get(provider.getName()) - 1);
                return provider;
            }
            i++;
        }
        return null;
    }


    public void deregisterProvider(String providerName){
        providers.remove(providerName);
    }


    public boolean registerProvider(Provider provider){
        if(providers.containsKey(provider.getName())){
            System.out.println("already registered ");
            return false;
        }
        if(providers.size() >=  MAX_PROVIDERS_POSSIBLE){
            System.out.println("Reached max provider limit. Can not add more providers");
            return false;
        }
        providers.put(provider.getName(), provider);
        addNameToProviderNameList(provider.getName());
        token.put(provider.getName(),MAX_PARALLEL_COUNT_PER_PROVIDER);
        return true;
    }

    private void addNameToProviderNameList(String name) {
        providerNamesListLoack.lock();
        try{
            providerNames.add(name);
        }finally {
            providerNamesListLoack.unlock();
        }
    }

    private void removeProviderFromProviderNameList(String provider) {
        providerNamesListLoack.lock();
        try{
            providerNames.remove(provider);
        }finally {
            providerNamesListLoack.unlock();
        }
    }

    public void checkAndUpdateProviders() throws InterruptedException {
        //periodically check for all the available providers for the heartbeat;
        while(true){
            System.out.println("checking heartbeat for all available providers");
            for(String key : providers.keySet()){
                if(!providers.get(key).isAlive()){
                    removeProviderFromProviderNameList(providers.get(key).getName());
                    providers.remove(key);
                }
            }
            System.out.println("available providers "+providers.keySet());
            Thread.sleep(10000);
        }

    }


    public void reIncludeProvider(Provider provider) throws InterruptedException {
        if(provider!=null && provider.isAlive()){ //check if provider is alive
            Thread.sleep(1000);
            if(provider.isAlive()){ // 2nd HeartBeat Check
                System.out.println("re-registering provider : "+ provider.getName());
                registerProvider(provider);
            }
        }

    }


}




public class LBPlatform {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        LoadBalancer loadBalancer = LoadBalancer.getInstance();
        Provider p1 = new Provider("p1", true );
        System.out.println("registering p1 : "+loadBalancer.registerProvider(p1));
        Provider p2 = new Provider("p2", true);
        System.out.println("registering p2 : "+loadBalancer.registerProvider(p2));
        Provider p3 = new Provider("p3", true);
        System.out.println("registering p3 : "+loadBalancer.registerProvider(p3));
        Provider p4 = new Provider("p4", true);
        System.out.println("registering p4 : "+loadBalancer.registerProvider(p4));
        new Thread(() -> {
            try {
                loadBalancer.checkAndUpdateProviders();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        Thread.sleep(100);
        p4.setAlive(false);
        p3.setAlive(false);
        for(int i=0; i<5; i++){
            Future<String> returned = loadBalancer.get();
            while(!returned.isDone());
            System.out.println(returned.get());
        }



        for(int i=0; i<5; i++){
            Future<String> returned = loadBalancer.get();
            while(!returned.isDone());
            System.out.println(returned.get());
        }
        p3.setAlive(true);
        loadBalancer.reIncludeProvider(p3);

        for(int i=0; i<50; i++){
            Future<String> returned = loadBalancer.get();
            while(!returned.isDone());
            System.out.println(returned.get());
        }

    }

}
