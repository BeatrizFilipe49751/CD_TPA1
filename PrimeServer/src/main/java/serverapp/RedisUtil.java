package serverapp;

import redis.clients.jedis.Jedis;

public class RedisUtil {
    private Jedis jedis;

    public RedisCheckResult checkIfPrimeInRedis(long number) {
        // TODO - Implement the logic to check if number is in Redis
        System.out.println("Verifying if number " + number + " is in Redis");
        return new RedisCheckResult(false, false);
    }

    public void savePrimeInRedis(long number, boolean isPrime) {
        // TODO - Implement the logic to save number in Redis
        System.out.println("Saving number " + number + " in Redis, isPrime: " + isPrime);
    }

}
