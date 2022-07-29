# Cartão de Interface de Rede
![Comunique com estranhos](item:oc2:network_interface_card)

O cartão de conecção de rede (CCR) permite a [computadores](../block/computer.md) enviar e receber mensagens de e para [conectores de rede](../block/network_connector.md) conectados a computador.

Estes cartões podem ser configurados para apenas conectar a lados específicos (use-o enquanto o segurar). Isto permite que múltiplos cartões constituam um router customizado, por exemplo.

Ao usar a distribuição Linux predefinida, este dispositivo irá fornecer um dispositivo ethernet normal. A configuração de rede pode ser feita manualmente ou usando o conveniente script `setup-network.lua`. Este script fornece a opção de usar tanto uma configuração de endereço fixo ou uma configuração DHCP. Para uma configuração DHCP, unicamente um computador na rede deverá servir como o servidor DHCP.

Após a configuração inicial, utilize o comando `ipconfig` para ver o endereço IP atual.

Computadores *precisam de ser desligados* antes de instalar ou remover este componente. A instalação do mesmo enquanto o computador está ligado resultará em nenhum efeito, porém a sua remoção poderá resultar em erros de sistema.
