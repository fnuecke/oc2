# Módulo de Operações de Inventário
![O que é teu é meu](item:oc2:inventory_operations_module)

O módulo de operações de inventário fornece a [robôs](robot.md) a habilidade de inserir e extrair itens de inventários no mundo. Suporta tanto blocos como entidades.

## API
Nome do dispositivo: `inventory_operations`

Este é um dispositivo de alto nível. Ele tem de ser controlado utilizando o API de dispositivos de alto nível. A distribuição Linux predefinida oferece bibliotecas Lua para este API. Por exemplo:
`local d = require("devices")`  
`local m = d:find("inventory_operations")`  
`m:drop(1, "front")`

### Lados
O parâmetro `side` nos presentes métodos representa um lado local ao bloco do dispositivo. Valores válidos são: `front`, `up` e `down`.

### Métodos
`move(fromSlot:number,intoSlot:number,count:number)` tenta mover o número especificado de itens de um slot de inventário de um robô para outro.
- `fromSlot` é o slot de onde extrair itens.
- `intoSlot` é o slot para onde inserir itens.
- `count` é o número de itens para mover.

`drop(count:number[,side]):number` tenta deixar cair do slot de inventário especificado na direção especificada. Ele vai deixar cair itens tanto num inventário tanto no mundo se nenhum inventário estiver presente.
- `count` é o número de itens para deixar cair.
- `side` é a direção relativa na qual deixar cair itens. Opcional, por defeito possuí o valor `front`. Veja a secção "Lados".
- Retorna o número de itens que foram deixados cair.

`dropInto(intoSlot:number,count:number[,side]):number` tenta deixar cair do slot de inventário especificado na direção especificada. Ele apendas deixa cair itens num inventário.
- `intoSlot` é o slot para onde inserir itens.
- `count` é o número de itens para deixar cair.
- `side` é a direção relativa na qual deixar cair itens. Opcional, por defeito possuí o valor `front`. Veja a secção "Lados".
- Retorna o número de itens que foram deixados cair.

`take(count:number[,side]):number` tenta apanhar o número especificado de itens na direção especificada. Ele vai apanhar itens tanto de um inventário tanto do mundo se nenhum inventário estiver presente.
- `count` é o número de itens para apanhar.
- `side` é a direção relativa na qual apanhar itens. Opcional, por defeito possuí o valor `front`. Veja a secção "Lados".
- Retorna o número de itens que foram apanhados.

`take(count:number[,side]):number` tenta apanhar o número especificado de itens na direção especificada. Ele apenas apanha itens de um inventário.
- `fromSlot` é o slot de onde extrair itens.
- `count` é o número de itens para apanhar.
- `side` é a direção relativa na qual apanhar itens. Opcional, por defeito possuí o valor `front`. Veja a secção "Lados".
- Retorna o número de itens que foram apanhados.

