package serverapp;

public class RedisCheckResult {
        private final boolean found;
        private final boolean isPrime;

        public RedisCheckResult(boolean found, boolean isPrime) {
            this.found = found;
            this.isPrime = isPrime;
        }

        public boolean isFound() {
            return found;
        }

        public boolean isPrime() {
            return isPrime;
        }
}
