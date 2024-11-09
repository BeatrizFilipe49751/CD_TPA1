
import redis.clients.jedis.Jedis;

import java.util.logging.Logger;

public class isPrimeCalculator {

    private static final Logger logger = Logger.getLogger(isPrimeCalculator.class.getName());

    public static void main(String[] args) {
        final long number = Long.parseLong(args[0]);
        final String redisAddress = args[1];
        final int redisPort = Integer.parseInt(args[2]);

        final isPrimeCalculator isPrimeCalculator = new isPrimeCalculator();
        boolean isPrime = isPrimeCalculator.isPrime(number);

        try (Jedis jedis = new Jedis(redisAddress, redisPort)) {
            jedis.set(String.valueOf(number), String.valueOf(isPrime));
            System.out.println("Number " + number + " is prime: " + isPrime);
        } catch (Exception e) {
            logger.severe("Exception in function 'main' at isPrimeCalculator: " + e.getMessage());
        }
    }

    // Returns true if the number is prime, false otherwise
    boolean isPrime(long number) {
        if (number <= 1L) return false;
        if (number == 2L || number == 3L) return true;
        if (number % 2L == 0) return false;

        long limit = (long) Math.sqrt(number);
        for (long i = 3; i <= limit; i += 2) {
            if (number % i == 0) return false;
        }
        return true;
    }

}

