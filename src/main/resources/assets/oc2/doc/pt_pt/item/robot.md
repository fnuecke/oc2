# Robô
![Eu, por exemplo, dou as boas-vindas aos nossos novos senhores robôs](item:oc2:robot)

Robôs são, essencialmente, [computadores](../block/computer.md) móveis. Devido á sua natureza não estacionária, existe algum comportamento que difere daquele de um computador normal. Eles não podem conectar [interfaces BUS](../block/bus_interface.md), por exemplo. Em vez de dispositivos de cartão, eles suportam dispositivos de módulo. Os mesmos são dispositivos especializados tendo em conta a mobilidade dos robôs.

Robôs têm um inventário com um tamanho fixo e possuem uma bateria interna topo de gama. Apenas o inventário normal de um robô pode ser automaticamente cheio e esvaziado, por exemplo por dispositivos como funís. O componente inventário de um robô tem que ser manualmente configurado.

Na sua configuração predefinida, robôs não podem interagir com o seu próprio inventário. Utilize um [módulo de operações de inventário](inventory_operations_module.md) para possibilitar a robôs a habilidade de mover itens no seu próprio inventário, como também inserir e extrair itens de e para outros inventários.

Para recarregar um robô, é recomendado o uso do [carregador](../block/charger.md). É possível para robôs recarregarem-se a si mesmos ao moverem-se sobre um carregador. Alternativamente, eles podem ser colocados num inventário em cima do carregador.

A distribuição Linux predefinida fornece uma biblioteca Lua útil, `robot`, que facilita o controlo de robôs. O API fornecido oferece métodos assíncronos para movimento. A biblioteca implementa alternativas síncronas, tornando programação sequencial mais eficiente.

## API
Nome do dispositivo: `robot`

Este é um dispositivo de alto nível. Ele tem de ser controlado utilizando o API de dispositivos de alto nível. A distribuição Linux predefinida oferece bibliotecas Lua para este API. Por exemplo:
`local d = require("devices")`  
`local r = d:find("robot")`  
`r:move("forward")`

### Direções
O parâmetro `direction` nos presentes métodos representa uma direção local ao bloco do dispositivo. Valores válidos são: `forward`, `backward`, `upward`, `downward` para ações de movimento, `left` e `right` para ações de rotação. Estas direções são sempre medidas pelo ponto de vista do robô no momento em que ele executa a ação.

Aliases podem ser utilizados por conveniência: `back`, `up`, `down`. Para extrema brevidade, a letra inicial de cada direção pode ser utilizada.

### Métodos
Estes métodos estão presentes no dispositivo robot. Note que a biblioteca oferece wrappers úteis para todos eles. É recomendada a utilização da biblioteca em vés de interagir com o dispositivo diretamente.

`getEnergyStored():number` retorna a quantidade atual de energia armazenada na bateria interna do robô.
- Retorna a quantidade atual de energia.

`getEnergyCapacity():number` retorna a quantidade máxima de energia que pode ser armazenada na bateria interna do robô.
- Retorna a quantidade máxima de energia armazenada.

`getSelectedSlot():number` retorna o slot de inventário do robô selecionado atualmente. Isso é usado por vários módulos como uma entrada implícita.
- Retorna o slot de inventário do robô.

`setSelectedSlot(slot:number):number` define o slot de inventário do robô selecionado atualmente. Isso é usado por vários módulos como uma entrada implícita.
- `slot` é o slot a selecionar.
- Retorna o index do slot recém-selecionado. O valor pode ser diferente de `slot` se o valor especificado for inválido.

`getStackInSlot(slot:number):table` obtém uma descrição do item no slot especificado.
- `slot` é o index da slot da qual obter a descrição do item.

`move(direction):boolean` tenta programar uma ação de movimento na direção especificada.
- `direction` é a direção a mover.
- Retorna se a ação foi programada com sucesso.

`turn(direction):boolean` tenta programar uma ação de rotação na direção especificada.
- `direction` é a direção para a qual rodar.
- Retorna se a ação foi programada com sucesso.

`getLastActionId():number` retorna o ID opaco da última ação programada. Invoque após uma invocação `move()` ou `turn()` bem-sucedida para obter o id associado à ação programada.
- Retorna o id associado à ação programada.

`getQueuedActionCount():number` retorna o número de ações atualmente esperando na fila de ações a serem processadas. Use para aguardar a conclusão das ações quando a fila falhar.
- Retorna o número de ações pendentes no momento.

`getActionResult(actionId:number):string` retorna o resultado da ação com o id especificado. Ids de ação podem ser obtidos de `getLastActionId()`. Apenas um número limitado de resultados de ações anteriores está disponível a cada o momento.
- Retorna o resultado para o ID de ação especificado ou nada se não estiver disponível. Quando disponíveis, os valores possíveis são: `INCOMPLETE`, `SUCCESS` e `FAILURE`.

### API da Biblioteca
- Nome da biblioteca: `robot`

Esta é uma biblioteca Lua. Ela pode ser usada na distribuição Linux predefinida. Por exemplo:
`local d = require("devices")`  
`r.move("forward")`  
`r.turn("left")`

### Métodos
`energy():number` retorna a quantidade atual de energia armazenada na bateria interna do robô.
- Retorna a quantidade atual de energia.

`capacity():number` retorna a quantidade máxima de energia que pode ser armazenada na bateria interna do robô.
- Retorna a quantidade máxima de energia armazenada.

`slot():number` retorna o slot de inventário do robô selecionado atualmente. Isso é usado por vários módulos como uma entrada implícita.
- Retorna o slot de inventário do robô.

`slot(slot:number):number` define o slot de inventário do robô selecionado atualmente. Isso é usado por vários módulos como uma entrada implícita.
- `slot` é o slot a selecionar.
- Retorna o index do slot recém-selecionado. O valor pode ser diferente de `slot` se o valor especificado for inválido.

`stack(slot:number):table` obtém uma descrição do item no slot especificado.
- `slot` é o index da slot da qual obter a descrição do item. Opcional, possui valor por defeito de `slot()`.

`move(direction):boolean` tenta programar uma ação de movimento na direção especificada.
- `direction` é a direção a mover.
- Retorna se a ação foi programada com sucesso.

`moveAsync(direction):boolean` tenta, assincronamente, programar uma ação de movimento na direção especificada.
- `direction` é a direção a mover.

`turn(direction):boolean` tenta programar uma ação de rotação na direção especificada.
- `direction` é a direção para a qual rodar.
- Retorna se a ação foi programada com sucesso.

`turnAsync(direction):boolean` tenta, assincronamente programar uma ação de rotação na direção especificada.
- `direction` é a direção para a qual rodar.
