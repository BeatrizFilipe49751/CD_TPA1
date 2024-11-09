
import redis.clients.jedis.Jedis;

import java.util.Random;
import java.util.logging.Logger;

public class isPrimeCalculator {

    public isPrimeCalculator() {
    }

    private static final Logger logger = Logger.getLogger(isPrimeCalculator.class.getName());

    public static void main(String[] args) {
        final long numero = Long.parseLong(args[0]);
        final String redisAddress = args[1];
        final int redisPort = Integer.parseInt(args[2]);

        final isPrimeCalculator isPrimeCalculator = new isPrimeCalculator();
        boolean isPrime = isPrimeCalculator.isPrime(numero);

        try (Jedis jedis = new Jedis(redisAddress, redisPort)) {
            jedis.set(String.valueOf(numero), String.valueOf(isPrime));
            System.out.println("Number " + numero + " is prime: " + isPrime);
        } catch (Exception e) {
            logger.severe("Exception in function 'main' at isPrimeCalculator: " + e.getMessage());
        }
    }

    // ex: the number 999998727899999 is prime
    boolean isPrime(long number) {
        if (number <= 1L) return false;
        if (number == 2L || number == 3L) return true;
        if (number % 2L == 0) return false;
        simulateExecutionTime();
        for (long i = 3; i <= Math.sqrt(number); i += 2) {
            if (number % i == 0) return false;
        }
        return true;
    }

    void simulateExecutionTime() {
        try {
            // simulate processing time between 200 ms and 3 s
            Thread.sleep(new Random().nextInt(9800) + 200);
        } catch (InterruptedException e) {
            logger.severe("Exception in function 'simulateExecutionTime' at isPrimeCalculator: " + e.getMessage());
        }
    }
}

