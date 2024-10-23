package serverapp;

import redis.clients.jedis.Jedis;

import java.util.Arrays;

public class RedisUtil {    //docker run -p 6379:6379 -it redis/redis-stack:latest
    private final Jedis jedis = new Jedis("localhost");

    public RedisCheckResult checkIfPrimeInRedis(long number) {
        // TODO - Implement the logic to check if number is in Redis
        System.out.println("Verifying if number " + number + " is in Redis");
        String search = jedis.get(String.valueOf(number));

        boolean found = !search.isEmpty();
        boolean isPrime = Boolean.parseBoolean(search);

        return new RedisCheckResult(found, isPrime);
    }

/*    public static boolean isPrime(long number) {
        // Basic checks
        if (number < 2) return false; // 0 and 1 are not prime
        if (number <= 3) return true;  // 2 and 3 are prime
        if (number % 2 == 0) return false; // Exclude even numbers

        // Check for factors from 5 to sqrt(number)
        for (long i = 5; i * i <= number; i += 6) {
            if (number % i == 0 || number % (i + 2) == 0) {
                return false; // Found a factor
            }
        }
        return true; // No factors found, number is prime
    }*/

    public void savePrimeInRedis(long number, boolean isPrime) {
        // TODO - Implement the logic to save number in Redis
        jedis.set(String.valueOf(number), String.valueOf(isPrime));
        System.out.println("Saving number " + number + " in Redis, isPrime: " + isPrime);
    }

}
