# Interface de Redstone
![Tudo vermelho](block:oc2:redstone_interface)

A interface de redstone fornece um BUS omnidirecional para receber e emitir sinais de redstone.

Use esta interface para interagir com dispositivos primitivos, como portas e lâmpadas, ou outras máquinas que ofereçam um protocolo de redstone.

Este é um dispositivo de alto nível. Ele tem de ser controlado utilizando o API de dispositivos de alto nível. A distribuição Linux predefinida oferece bibliotecas Lua para este API. Por exemplo:
`local d = require("devices")`  
`local r = d:find("redstone") `  
`r:setRedstoneOutput("up", 15)`

## API
Nome do dispositivo: `redstone`

### Lados
O parâmetro `side` nos presentes métodos representa um lado local ao bloco do dispositivo. Valores válidos são: `up`, `down`, `left`, `right`, `front`, `back`, `north`, `south`, `west` e `east`.

Cada face fo bloco tem um indicador por conveniência. Nomes de lado representam os nomes com o bloco visto pela face primária (indicado por uma única marca). Ao olhar para a face primária:
- `front` e `south` é a face para a qual se está a olhar.
- `back` e `north` é a face atrás do bloco.
- `left` e `west` é a face á sua esquerda.
- `right` e `east` é a face á sua direita.
- `up` e `down` são as faces de topo e fundo, respetivamente.

### Métodos
`getRedstoneInput(side):number` obtém o sinal de redstone recebido pelo lado especificado.
- `side` é uma string representando o lado a ler. Veja a secção "Lados".
- Retorna o número representando o atual valor de entrada.

`setRedstoneOutput(side, value:number)` define o sinal de redstone emitido pelo lado especificado.
- `side` é uma string representando o lado a emitir. Veja a secção "Lados".
- `value` é um número representante da força do sinal a emitir, situado no intervalo [0, 15].

`getRedstoneOutput(side):number` obtém o sinal de redstone emitido pelo lado especificado.
- `side` é uma string representando o lado a ler. Veja a secção "Lados".
- Retorna o número representando o atual valor de saída.