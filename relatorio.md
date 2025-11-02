# Relatório

## Arquitetura de software

### Visão geral dos componentes

- Client (CLI)
	- Fala com o Gateway para pesquisar
- Gateway
	- Expõe um serviço RMI para o Cliente e para os Downloaders.
	- Faz balanceamento entre vários Barrels (round‑robin com failover).
	- Mantém uma fila de URLs por indexar (para os Downloaders).
- Downloader (um no host e um no Docker)
	- Pede a próxima URL ao Gateway, faz download/parsing (JSoup), envia a página para os Barrels e devolve ao Gateway os links descobertos.
	- Envia para dois Barrels e tem lógica de retentativa/failover.
- Barrel (um no host e um no Docker)
	- Indexa páginas (inverted index), responde a pesquisas, devolve incoming links e estatísticas.

### Deployment

- Host: Gateway, Barrel1, Downloader1
- Docker: Barrel2, Downloader2, Client

Portas relevantes:
- RMI Registry: 1099 (no container exposto como 1100:1099 para acesso a partir do host)
- Barrel1 (host) objeto remoto: 2000
- Barrel2 (container) objeto remoto: 2001 (exposto 2001:2001)

Configuração por ficheiros `.properties` (com overrides por variáveis de ambiente nos containers):
- `gateway.properties`: bind do Gateway e lista dinâmica de `barrelN.name/host/port`.
- `barrel.properties`: nome, `registry.port`, `rmi.hostname`, `object.port`.
- `downloader.properties`: hosts/portas do Gateway e dos 2 Barrels.

Overrides úteis por ambiente (quando em Docker):
- Barrel: `RMI_HOSTNAME`, `BARREL_NAME`, `OBJECT_PORT`, `REGISTRY_PORT`
- Downloader: `HOST_GATEWAY`, `PORT_GATEWAY`, `HOST_BARREL1`, `PORT_BARREL1`, `HOST_BARREL2`, `PORT_BARREL2`
### Principais fluxos

1) Search

Client -> (RMI) Gateway -> (RMI) Barrel -> (RMI) Gateway -> Client

Gateway usa round‑robin para distribuir a carga. Se um Barrel falhar, faz relookup desse Barrel no Registry e repete a chamada uma vez; se continuar a falhar, tenta o próximo Barrel. Só retorna erro se todos os Barrels falharem.

2) Indexar

Downloader -> Gateway: takeNext()
Downloader -> download da página e parsing (JSoup)
Downloader -> Barrel1/Barrel2: sendPage()
Downloader -> Gateway: putNewURL() para links encontrados

Se o envio para um Barrel falhar, tenta o outro e faz relookup do Barrel que falhou.

## Detalhes do funcionamento do RPC/RMI

### Interfaces RMI
- `GatewayService`
	- SearchResult[] searchWord(String[] terms, int page)
	- String[] getIncomingLinks(String url)
	- void putNewURL(String url)
	- String takeNext()
- `BarrelService`
	- void sendPage(PageDTO page)
	- SearchResult[] searchWord(String[] terms, int page)
	- String[] getIncomingLinks(String url)

### Registry vs Objeto remoto (duas portas diferentes)
- Registry (normalmente 1099): só serve para obter a "proxy".
- Objeto remoto: usa a sua própria porta (`object.port`, p.ex. 2102/2001) para receber as chamadas dos métodos.

### Endereço anunciado (advertisedHost)
- Controlado por `java.rmi.server.hostname` (`rmi.hostname` nos `.properties`).
- Este hostname/IP fica embebido dentro da proxy devolvida pelo Registry.
- Tem de ser um endereço alcançável por quem chama (host e/ou containers). Por isso usamos `host.docker.internal` em ambos os Barrels.

### Exportação com porta fixa (object.port)
- Barrel1 (host): `object.port=2000`
- Barrel2 (container): `object.port=2001`

### Recuperação após falhas (relookup)
Quando um Barrel reinicia, o seu objeto remoto ganha um novo ID. As proxies antigas deixam de funcionar.

- Downloader: ao apanhar uma exception numa chamada a Barrel/Gateway, faz logo um `Naming.lookup` para obter uma proxy nova e continua o trabalho sem reiniciar.
- Gateway: ao falhar uma chamada para um Barrel, faz relookup desse Barrel e repete a chamada uma vez; se continuar a falhar, tenta o próximo Barrel.

## Replicação (reliable multicast)

Não implementado. O sistema atual faz um envio best‑effort para dois Barrels (quando disponíveis), mas não há multicast fiável.
Em caso de falha durante o envio, a recuperação é feita por tentativas ponto‑a‑ponto.


## Crash handler do Barrel

o Barrel guarda o idnex e volta a carregá‑lo no arranque.

- O que é guardado: pages, index e incomingLinks
- No arranque tenta carregar os dados; no shutdown guarda automaticamente através de um shutdown hook.
- Ficheiro: `<BarrelName>_index.txt`

- ''Exceção'' O Barrel2 escreve no ficheiro quando leva shutdown, mas quando é iniciado não consegue ler o ficheiro com a data (index, ...)


## Testes

Foram realizados os seguintes testes de tolerância a falhas e recuperação:

| Cenário de teste | Passou? | Observações |
|------------------|-----------|-------------|
| Matar um Barrel e voltar a ligar | Sim | O sistema deteta a falha, faz relookup do Barrel no Registry e restabelece a ligação automaticamente |

| Matar um Downloader e voltar a ligar | Sim | O Downloader tenta reconectar-se quando a Gateway ou Barrels falham  |

| Matar o Gateway e voltar a ligar | Sim | Cliente e Downloaders fazem relookup quando a Gateway volta |

| Barrels guardam dados quando morrem | Sim | Os dados index, pages e incomingLinks são guardados no ficheiro quando os barrels fecham |

| Downloader/Gateway guardam dados quando morrem | Não | Os dados em memória (queue de Urls) são perdidos |

| Gateway continua a pesquisa quando Barrel falha, procurando num novo | Sim |

| Downloader continua sem Barrels | Não (espera que haja um) | O Downloader bloqueia e aguarda até que pelo menos um Barrel esteja disponível antes de continuar |

## Distribuição de tarefas

Trabalho realizado individualmente.