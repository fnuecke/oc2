# Rede
Para montar uma rede de [computadores](block/computer.md), utilize [conectores de rede](block/network_connector.md) e [cabos de rede](item/network_cable.md) para conectar os conectores. Finalmente, garanta que um [cartão de interface de rede](item/network_interface_card.md) está instalado em cada computador que deverá ser parte da rede.

Conectores têm de ser anexados a qualquer face exceto a face frontal de um computador. Iso permite que o cartão de rede instalado no mesmo envie e receba pacotes através do conector.

Conectores conectados com cabos irão encaminhar pacotes entre eles. Para redes mais complexas um [concentrador de rede](block/network_hub.md) poderá ser necessário. Ele permite que múltiplos conectores se conectem entre si.

## Saltos
Pacotes de rede podem viajar através de um certo número máximo de "saltos". Um salto pode ser interpretado como uma simples operação de encaminhamento. Por exemplo, um conector encaminhando um pacote utiliza um salto. Um concentrador de rede encaminhando um pacote para cada conector que o pacote não alcançou utiliza um salto. Assim que o número de saltos restantes chegar a zero, o pacote não é encaminhado mais nenhuma vez. Isto impede que pacotes viagem em círculos numa rede com ciclos. Assim sendo, este é um mecanismo de prevenção. Nenhuma rede deverá conter ciclos, dado que isto vai levar a que o mesmo pacote alcance o mesmo computador múltiplas vezes.

## Configuração do computador
Para configurar um computador para pertencer a uma rede, primeiro garanta que um cartão de rede está instalado no mesmo. Após isto, utilizando a distribuição de Linux predefinida, execute o comando `setup-network.lua`. Isto irá fornecer um instalador para configurar a forma como o computador se deverá conectar á rede. Alternativamente, se souber o que está a fazer, configurar a rede segue o mesmo processo de qualquer instalação Linux normal.

## DHCP
DHCP é um protocolo que permite uma configuração de rede simplificada. Apenas um computador necessitará de possuir um endereço de rede configurado estaticamente, enquanto todos os outros computadores na rede poderão ter os seus endereços atribuídos automaticamente. Quando utilizar esta configuração, garanta que existe apenas um único computador que desempenhará o papel de servidor DHCP. Garanta também que nenhum computador utiliza um endereço IP que se encaixe no intervalo de endereços IP dinamicamente atribuídos.

## Ferramentas
Assim que possuir uma rede funcional, com múltiplos computadores numa rede, você têm ao seu dispor todas as opções no mundo. Por exemplo, você pode copiar ficheiros entre computadores utilizando `scp`, entrar num computador remotamente utilizando `ssh` e escrever programas de rede customizados em Lua utilizando a biblioteca `socket`. Para alguns exemplos de como utilizar a biblioteca `socket`, veja o [repositório de exemplos oficial](https://github.com/diegonehab/luasocket/tree/master/samples).
