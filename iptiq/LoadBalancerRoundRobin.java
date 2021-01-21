package extra.challenge.iptiq;

final class LoadBalancerRoundRobin extends LoadBalancer {
    private LoadBalancerRoundRobin(){ }
    private static LoadBalancerRoundRobin loadBalancerRoundRobin;
    private static int roundRobinIndex = 0;

    public static LoadBalancer getInstance(){
        if(loadBalancerRoundRobin == null){
            synchronized (LoadBalancerRoundRobin.class){
                if(loadBalancerRoundRobin == null){
                    loadBalancerRoundRobin = new LoadBalancerRoundRobin();
                }
            }
        }
        return loadBalancerRoundRobin;
    }

    @Override
    public Provider getProvider() {
        providerNamesListLock.lock();
        String providerName;
        try{
            providerName = providerNames.get(roundRobinIndex);
            if(providerNames.size() <= roundRobinIndex+1){ //calculate next round robin index
                roundRobinIndex =0;
            }else{
                roundRobinIndex++;
            }
        }finally {
            providerNamesListLock.unlock();
        }
        return providers.get(providerName);
    }
}
