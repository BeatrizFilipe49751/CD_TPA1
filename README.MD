# Computação Distribuída

---
## T.P.A. 1 — Primes Ring


### 1. Objetivo

> [!NOTE]  
> Utilização do middleware gRPC na implementação de um sistema com múltiplos servidores de
suporte a balanceamento de carga na execução de pedidos por parte de múltiplos clientes. Cada
pedido de um cliente pode envolver interação com outros servidores ou mesmo exigir
processamento realizado em containers Docker lançado pelos servidores gRPC.

### 2. Descrição

> [!NOTE]
> O Sistema **PrimesRing** é um anel de servidores distribuídos. Ele é composto por:
> 
> - O servidor principal (**RingManager**):
>   - Regista e interliga os servidores secundários.
>   - Distribui os clientes pelos servidores secundários.
> - Os servidores secundários (**PrimeServer**):
>   - São responsáveis por determinar se um número é primo ou não, com a ajuda de containers Docker.
>   - Ao serem inicializados, iniciam um dicionário Redis onde armazenam os números primos já calculados.
> - Os clientes:
>   - Conectam-se ao servidor principal, que os redireciona para um secundário.
>   - Perguntam a um servidor secundário se um número é primo ou não.

---

### 3. Fluxo de Funcionamento

> [!TIP]
> 1. O cliente conecta-se ao RingManager.
> 2. O RingManager redireciona o cliente para um PrimeServer.
> 3. O cliente faz um pedido ao PrimeServer para determinar se um número é primo ou não.
> 4. O PrimeServer verifica se o número já foi armazenado no seu Redis.
>    - Número encontrado, o PrimeServer responde imediatamente ao cliente com o resultado.
>    - Número não encontrado, o PrimeServer envia a mensagem para o anel.
> 5. Cada PrimeServer vê na mensagem se o resultado já foi encontrado.
>    - Resultado já encontrado, atualiza o seu Redis e reenvia a mensagem para o próximo servidor.
>    - Resultado ainda não encontrado, verifica o seu Redis e, se encontrado, atualiza a mensagem e reenvia para o próximo servidor.
> 6. A mensagem chega ao PrimeServer que fez o pedido inicial.
>    - Resultado encontrado, o PrimeServer responde imediatamente ao cliente.
>    - Resultado não encontrado, o PrimeServer lança um container Docker para calcular se o número é primo ou não e responde ao cliente.

---

### 4. Pressupostos
> [!CAUTION]
> RingManager está ativo em um IP e port fixos e bem conhecidos (**34.175.102.70:8500**).
> Sistema suporta o registo de novos primeServers sem interromper o funcionamento.
> Sistema não suporta a remoção de primeServers já registados.

### 5. Configuração e Execução

> [!WARNING]
> 1. Colocar em execução a VM do RingManager:
>    - No terminal, correr `java -jar RingManagerServer-1.0-jar-with-dependencies.jar`.
>
> 2. Colocar em execução a VM(s) do(s) PrimeServer(s):
>    - No terminal, correr `sudo java -jar PrimeServer-1.0-jar-with-dependencies.jar <vm external ip> <port>`.
> 
> 3. Colocar em execução o cliente:
>    - No terminal, correr `java -jar PrimesRingClient-1.0-jar-with-dependencies.jar`.

---

### 6. Contratos
[TODO]