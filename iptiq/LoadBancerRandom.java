package extra.challenge.iptiq;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class LoadBancerRandom extends LoadBalancer {
    private static final int RETRY_COUNT = 5;
    private LoadBancerRandom(){ }
    private static LoadBancerRandom loadBancerRandom;

    public static LoadBalancer getInstance(){
        if(loadBancerRandom == null){
            synchronized (LoadBalancerRoundRobin.class){
                if(loadBancerRandom == null){
                    loadBancerRandom = new LoadBancerRandom();
                }
            }
        }
        return loadBancerRandom;
    }

    @Override
    public Provider getProvider() {
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
}


