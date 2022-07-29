# Módulo de Túnel de Rede
![Visão de Túnel?](item:oc2:network_tunnel_module)

O cartão de Túnel de Rede permite a [robôs](robot.md)enviar e receber mensagens de outro dispositivo de túnel (módulos de túnel e [cartões de túnel](network_tunnel_card.md)) conectados ao módulo.

Para conectar dois dispositivos de túnel, abra as suas configurações de interface (utilize enquanto segurar), e insira o outro dispositivo de túnel a conectar. Tal permite a conecção entre quaisquer dois dispositivos de túnel de rede.

Ao usar a distribuição Linux predefinida, este dispositivo irá fornecer um dispositivo ethernet normal. A configuração de rede pode ser feita manualmente ou usando o conveniente script `setup-network.lua`. Este script fornece a opção de usar tanto uma configuração de endereço fixo ou uma configuração DHCP. Para uma configuração DHCP, unicamente um computador na rede deverá servir como o servidor DHCP.

Após a configuração inicial, utilize o comando `ipconfig` para ver o endereço IP atual.

Computadores *precisam de ser desligados* antes de instalar ou remover este componente. A instalação do mesmo enquanto o computador está ligado resultará em nenhum efeito, porém a sua remoção poderá resultar em erros de sistema.
