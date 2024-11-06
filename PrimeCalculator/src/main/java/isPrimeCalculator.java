
import redis.clients.jedis.Jedis;

import java.util.Random;

public class isPrimeCalculator {

    public isPrimeCalculator() {}

    public static void main(String[] args) {
        long numero = Long.parseLong(args[0]);
        String redisAddress = args[1];
        int redisport = Integer.parseInt(args[2]);
        Jedis jedis = new Jedis(redisAddress, redisport);
        isPrimeCalculator isPrimeCalculator = new isPrimeCalculator();
        boolean isPrime = isPrimeCalculator.isPrime(numero);
        jedis.set(String.valueOf(numero), String.valueOf(isPrime));
        System.out.println("Number " + numero + " is prime: " + isPrime);
    }

    // ex: the number 999998727899999 is prime
    boolean isPrime(long number) {
        if (number <= 1L) return false;
        if (number == 2L || number == 3L) return true;
        if (number % 2L == 0) return false;
        simulateExecutionTime();
        for (long i=3; i <= Math.sqrt(number); i+=2) {
            if (number % i == 0) return false;
        }
        return true;
    }
    void simulateExecutionTime() {
        try {
            // simulate processing time between 200ms and 3s
            Thread.sleep(new Random().nextInt(9800) + 200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

