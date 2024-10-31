package serverapp;

import redis.clients.jedis.Jedis;


public class RedisUtil {

    private final Jedis jedis;

    public RedisUtil(int redisHostPort) {
        this.jedis = new Jedis("localhost", redisHostPort);
    }

    public RedisCheckResult checkIfPrimeInRedis(long number) {
        System.out.println("Verifying if number " + number + " is in Redis");
        String search = jedis.get(String.valueOf(number));

        boolean found = (search != null);
        boolean isPrime = found && Boolean.parseBoolean(search);

        return new RedisCheckResult(found, isPrime);
    }

    public void savePrimeInRedis(long number, boolean isPrime) {
        jedis.set(String.valueOf(number), String.valueOf(isPrime));
        System.out.println("Saving number " + number + " in Redis, isPrime: " + isPrime);
    }

}
