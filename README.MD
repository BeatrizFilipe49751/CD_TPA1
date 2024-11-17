# Computação Distribuída


## T.P.A. 1 — Primes Ring

### 1. Objetivo

Utilização do middleware gRPC na implementação de um sistema com múltiplos servidores de suporte a balanceamento de carga na execução de pedidos por parte de múltiplos clientes.  
Cada pedido de um cliente pode envolver interação com outros servidores ou mesmo exigir processamento realizado em containers Docker lançado pelos servidores gRPC.

---

### 2. Descrição

O Sistema **PrimesRing** é um anel de servidores distribuídos. Ele é composto por:

- O servidor principal (**RingManager**): `RingManagerServer`
   - Regista e interliga os servidores secundários.
   - Distribui os clientes pelos servidores secundários.  

  
- Os servidores secundários (**PrimeServer**): `PrimeServer`
   - São responsáveis por determinar se um número é primo ou não, com a ajuda de containers Docker.
   - Ao serem inicializados, iniciam um dicionário Redis onde armazenam os números primos já calculados.
  

- Os clientes (**PrimesRingClient**): `PrimesRingClient`
  - Conectam-se ao servidor principal, que os redireciona para um secundário.
  - Perguntam a um servidor secundário se um número é primo ou não.
  

---

### 3. Fluxo de Funcionamento

1. O cliente conecta-se ao `RingManagerServer`.  
   
  
2. O `RingManagerServer` redireciona o cliente para um `PrimeServer`.  
   
  
3. O cliente faz um pedido ao `PrimeServer` para determinar se um número é primo ou não.  
   
  
4. O `PrimeServer` verifica se o número já foi armazenado no seu **_Redis_**. Se o número:
   - For encontrado, o servidor responde imediatamente ao cliente com o resultado.
   - Não for encontrado, o servidor circula a mensagem para o anel.
  

5. Cada `PrimeServer` vê na mensagem se o resultado já foi encontrado.
   - Resultado já encontrado, atualiza o seu **_Redis_** e reenvia a mensagem para o próximo servidor.
   - Resultado ainda não encontrado, verifica o seu **_Redis_** e, se encontrado, atualiza a mensagem e reenvia para o próximo servidor.
  

6. A mensagem chega ao `PrimeServer` que fez o pedido inicial. Se o resultado:
   - Foi encontrado, o servidor responde imediatamente ao cliente.
   - Não foi encontrado, o servidor lança um container _Docker_ para calcular se o número é primo ou não, e responde ao cliente.

---

### 4. Pressupostos

- `RingManagerServer` está ativo num IP e port fixos e bem conhecidos (`34.175.102.70 : 8500`)*.
- Sistema suporta o registo de novos `PrimeServer`'s sem interromper o funcionamento.
- Sistema não suporta a remoção de `PrimeServer`'s já registados.

*_Nota_: Se por alguma razão o `RingManagerServer` tiver outro IP,
o sistema cliente (`PrimesRingClient`) e o servidor (`PrimeServer`) têm automatização para apanhá-lo.

---

### 5. Configuração e Execução

1. Colocar em execução a VM do `RingManagerServer`:
   - No terminal, correr `java -jar RingManagerServer-1.0-jar-with-dependencies.jar`.
  
  
2. Colocar em execução a VM(s) do(s) `PrimeServer(s)`:
   - No terminal, correr `sudo java -jar PrimeServer-1.0-jar-with-dependencies.jar <port>`.
  
  
3. Colocar em execução o cliente `PrimesRingClient`:
   - No terminal, correr `java -jar PrimesRingClient-1.0-jar-with-dependencies.jar`.

---

### 6. Contratos Protobuf

1. **GetServerContract**:
    - `getPrimeServer(Request) returns (EndPoint)`
      - Request: `void`
      - EndPoint: `string ip, int32 port`
      - Permite ao cliente solicitar ao `RingManagerServer` o IP e porto de um `PrimeServer`.
         - É chamado pelo cliente automaticamente ao inicializar a aplicação.  
         - O RingManager redireciona o cliente para um `PrimeServer`, utilizando balanceamento de carga.
             > **Balanceamento de carga**: O index incrementado a cada cliente que se conecta, usado para escolher o PrimeServer da lista de servidores registados.
---
2. **RingManagerContract**:
   - `registServer(ServerInfo) returns (stream ServerInfo)`
     - ServerInfo: `string ip, int32 port`
     - Permite ao `PrimeServer` registar-se no `RingManagerServer`.
       - O uso de stream permite ao `RingManagerServer` manter o anel de servidores atualizado.  
       - O stream envia uma mensagem `ServerInfo` atualizada para o servidor anterior quando um novo `PrimeServer` se regista.  
       - E também envia para o novo servidor com a informação do 1.º servidor do anel, de maneira a manter a circularidade do anel.
---
3. **IsPrimeContract**:
    - `isPrime(Number) returns (Result)`
      - Number: `int64 num`
      - Result: `bool prime`
      - Permite aos clientes solicitarem a um `PrimeServer` se um número long é primo ou não.
        - Se o número já se encontra no **_Redis_** deste servidor, ele responde imediatamente.
        - Se o número não se encontra no **_Redis_**, ele envia a mensagem para o anel (utilizando o `ringMessage`).
 ---
4. **RingMessageContract**:
    - `ringMessage(stream RingRequest) returns (RingResponse)`
      - RingRequest: `int64 number, bool hasInfo, bool isPrime, string originIp, int32 originPort`
      - RingResponse: `bool prime`
      - Permite aos `PrimeServer`'s comunicarem entre si.
        - O uso de stream permite que a mensagem circule pelo anel até chegar ao `PrimeServer` que fez o pedido inicial.
        - O RingRequest é reencaminhado para o próximo PrimeServer, sendo apenas atualizado se o servidor atual 
        encontrar a informação no seu **_Redis_**.
        - O RingResponse é enviado para o servidor original, que através do isPrime responde ao cliente.

---  

---

## Autores

### Grupo 9:
- **Douglas Incáo**, 47269 (a47269@alunos.isel.pt)
- **Beatriz Filipe**, 49751 (a49751@alunos.isel.pt)
- **Danilo Vieira**, 49988 (a49988@alunos.isel.pt)
