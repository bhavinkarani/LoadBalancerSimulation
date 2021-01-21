package extra.challenge.iptiq;

final class LoadBalancerFactoy {
    public static LoadBalancer getLoadBalancer(LBEnum type){
        if(type == LBEnum.ROUND_ROBIN){
            return LoadBalancerRoundRobin.getInstance();
        }
        if(type == LBEnum.RANDOM){
            return LoadBancerRandom.getInstance();
        }
        return null;
    }
}