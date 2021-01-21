package extra.challenge.iptiq;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

abstract class LoadBalancer {
    private static final int MAX_PROVIDERS_POSSIBLE = 10;
    private static final int MAX_PARALLEL_COUNT_PER_PROVIDER = 10;
    private volatile AtomicInteger requestsInProgress = new AtomicInteger(0);
    Map<String, Provider> providers = new ConcurrentHashMap<>();
    private ExecutorService executorService = Executors.newFixedThreadPool(10);
    Map<String, Integer> token = new ConcurrentHashMap<>();
    LinkedList<String> providerNames = new LinkedList<>();
    Lock providerNamesListLock = new ReentrantLock();

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
        return executorService.submit(() -> callProvider());
        //todo : set timeout
    }

    private String callProvider() throws ExecutionException, InterruptedException {
        Provider provider = getProvider();
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
        providerNamesListLock.lock();
        try{
            providerNames.add(name);
        }finally {
            providerNamesListLock.unlock();
        }
    }

    private void removeProviderFromProviderNameList(String provider) {
        providerNamesListLock.lock();
        try{
            providerNames.remove(provider);
        }finally {
            providerNamesListLock.unlock();
        }
    }

    public void startHeathCheck(){
        new Thread(() -> {
            try {
                checkAndUpdateProviders();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void checkAndUpdateProviders() throws InterruptedException {
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
    public abstract Provider getProvider();
}
