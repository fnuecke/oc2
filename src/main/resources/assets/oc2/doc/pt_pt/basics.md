# O Básico
Este documento contem alguma informação fundacional acerca de como os [computadores](block/computer.md) functionam. Isto não significa necessáriamente que esta informação é fácil de ler. Básico aqui significa os conceitos gerais em uso.

## Arquitetura
Computadores executam numa arquitetura RISC-V de único núcleo de uso geral. Isto significa que eles têm registos de 64 bits e suportam operações de ponto flutuante. Isto faz com que seja possível iniciar um kernel Linux moderno neles.

### Dispositivos Nativos
Dispositivos Nativos conectados a computadores são dispositivos mapeados na memória. Isto significa que eles estão mapeados algures na memória física. Computadores utilizam drivers Linux normais para interagir com eles.

Que dispositivos estão disponíveis, e onde esses dispositivos se encontram, é comunicado com o software executado no computador utilizando uma árvore de dispositivos achatada. Esta estrutura de dados pode conter informação adicional do sistema em geral. Em particular, ela também contém o tamanho da [memótia](item/memory.md) instalada. 
Dado que esta estrutura de dados é copiada ao iniciar, não pode ser alterada após tal. Esta é a razão pela qual os computadores têm de ser reiniciados ao mudar dispositivos nativos, tais como o [cartão de interface de rede](item/network_interface_card.md). Dispositivos para o qual isto é o caso possuem tipicamente uma nota correspondente na sua dica de ferramenta.

### Dispositivos APIAN
O outro tipo de dispositivos são os dispositivos de API de alto nível, por vezes chamados de dispositivos RPC. Estes dispositivos utilizam unicamente um único controlador, o qual comunica com os computadores utilizando um único dispositivo serial. Este dispositivo controlador está presente em todos os computadores, e têm a responsabilidade de coletar mensagens de vários dispositivos, e distribuir mensagens para dispositivos. O protocolo que este controlador usa é um protocolo de mensagem JSON simples. A biblioteca Lua `devices` fornecida com a biblioteca Linux predefinida envolve o dispositivo serial utilizado para conectar a este controlador. Como tal, a utilização da biblioteca `devices` sempre que é utilizado um dispositivo APIAN, como o [bloco de interface de redstone](block/redstone_interface.md) é *altamente* recomendado.

Devido á natureza do protocolo envolvido, a taxa de dados é a modos que limitada. A maioria dos dispositivos, portanto, normalmente apenas fornece APIs comparativamente simples que não necessitam de grandes quantidades de dados de qualquer forma.

## Configuração
Computadores podem ser configurados até certo ponto. A quantidade de memória, armazenamento extra na forma de [discos rígidos](item/hard_drive.md) e mais importante, que cartões instalar, está praticamente ao gosto do utilizador. Note que a distribuição Linux predefinida requer pelo menos 20M de memória, sendo 24M recomendados.

A maioria dos componentes contribui para o consumo de energia geral do computador. Para conservar energia, escolher os componentes exclusivamente necessários é essencial.

## Linux
A distribuição Linux predefinida contem algumas ferramentas de terminal básicas, e a possibilidade de escrever e executar programas em Lua. Para uma visão geral de como interagir com dispositivos APIAN utilizando Lua, veja a entrada [scripting](scripting.md) do manual.

Dispositivos nativos utilizam drivers Linux normais. Por exemplo, discos rígidos aparecem como dispositivos `/dev/vdaX` e podem ser formatados e montados regularmente.

Computadores fornecem dois dispositivos de relógio de hardware (RTC). O primeiro mede o tempo numa escala que a maior parte dos utilizadores pensa regularmente. É utilizado por predefinição em, por exemplo, ferramentas de terminal como `date` e `time`. O segundo mede o tempo como um computador real. Para obter o tempo real, utilize `hwclock -f /dev/rtc1`
