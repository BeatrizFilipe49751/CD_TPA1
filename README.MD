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
> - RingManager está ativo em um IP e port fixos e bem conhecidos (**34.175.102.70:8500**).
> - Sistema suporta o registo de novos primeServers sem interromper o funcionamento.
> - Sistema não suporta a remoção de primeServers já registados.

---

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

### 6. Contratos Protobuf

> [!INFO]
> 1. **GetServerContract**:
>    - **getPrimeServer**(Request) returns (EndPoint)
>      - Request: `void`
>      - EndPoint: `string ip, int32 port`
>      - Permite ao cliente solicitar ao RingManager o IP e porto de um PrimeServer.
>        - Chamado pelo cliente automaticamente ao inicializar a aplicação.
>        - RingManager redireciona o cliente para um PrimeServer, utilizando balanceamento de carga  
>        (Index incrementado a cada cliente que se conecta, usado para escolher o PrimeServer da lista de servidores registados).
> ---
> 2. **RingManagerContract**:
>    - **registServer**(ServerInfo) returns (stream ServerInfo)
>      - ServerInfo: `string ip, int32 port`
>      - Permite ao PrimeServer registar-se no RingManager.
>        - O uso de stream permite ao RingManager manter o anel de servidores atualizado.  
>        O stream envia uma mensagem ServerInfo atualizada para o servidor anterior quando um novo PrimeServer se regista.  
>        E também envia para o novo servidor com a informação do 1º servidor do anel, de maneira a manter a circularidade do anel.
> ---
> 3. **IsPrimeContract**:
>    - **isPrime**(Number) returns (Result)
>      - Number: `int64 num`
>      - Result: `bool prime`
>      - Permite aos clientes solicitarem a um PrimeServer se um número long é primo ou não.
>        - Se o número já se encontra no Redis deste servidor, ele responde imediatamente.
>        - Se o número não se encontra no Redis, ele envia a mensagem para o anel (utilizando o ringMessage).
> ---
> 4. **RingMessageContract**:
>    - **ringMessage**(stream RingRequest) returns (RingResponse)
>      - RingRequest: `int64 number, bool hasInfo, bool isPrime, string originIp, int32 originPort`
>      - RingResponse: `bool prime`
>      - Permite aos PrimeServers comunicarem entre si.
>        - O uso de stream permite que a mensagem circule pelo anel até chegar ao PrimeServer que fez o pedido inicial.
>        - O RingRequest é reencaminhado para o próximo PrimeServer, sendo apenas atualizado se o servidor atual 
>        encontrar a informação no seu Redis.
>        - O RingResponse é enviado para o servidor original, que através do isPrime responde ao cliente.

---

### 7. Autores

> [!Important]
> Grupo 9:
> - **Douglas Incáo**, 47269 (a47269@alunos.isel.pt)
> - **Beatriz Filipe**, 49751 (a49751@alunos.isel.pt)
> - **Danilo Vieira**, 49988 (a49988@alunos.isel.pt)