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
> - O sistema a implementar é um sistema em anel de números primos.  
> - O sistema é composto por um servidor principal e por um conjunto de servidores secundários.  
> - Um conjunto de servidores secundários cooperam para determinar se um número é primo ou não.  
> - Os clientes possuem um programa que segue um contrato protobuf 
> que lhes permite interagir com um dos servidores secundários.  

### 3. Funcionamento

> [!NOTE]
> - O servidor principal:
>   - interligar os servidores secundários.
>   - não é responsável por determinar se um número é primo ou não.
> - Os servidores secundários:
>   - São responsáveis por determinar se um número é primo ou não.
>   - Quando iniciam também iniciam um dicionário Redis para armazenar os números primos já calculados.
>   - Podem lançar containers Docker para calcular se um número é primo ou não.
> - Os clientes:
>   - Podem-se conectar a um dos servidores secundários.
>   - Podem perguntar a um servidor secundário para determinar se um número é primo ou não.

---

### 4. Instalação

> [!WARNING]
> Requisitos:
> - Instalar o [Docker](https://docs.docker.com/get-docker/):
>   - Após a instalação, vá a definições e ative a opção `[X] Expose daemon on tcp://localhost:2375 without TLS` e guarde.
> 
> - Com o ambiente Docker aberto, 
> abra um terminal e corra `docker run -d --name PrimeServerRedis -p 6000:6379 redis` 
> para iniciar o servidor Redis.

[TODO] Completar


