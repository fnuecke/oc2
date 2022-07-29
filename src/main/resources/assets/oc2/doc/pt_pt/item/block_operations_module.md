# Módulo de Operações de Bloco
![Break it, quick replace it](item:oc2:block_operations_module)

O módulo de operações de bloco fornece a [robôs](robot.md) a habilidade de quebrar e colocar blocos no mundo.

## API
Nome do dispositivo: `block_operations`

Este é um dispositivo de alto nível. Ele tem de ser controlado utilizando o API de dispositivos de alto nível. A distribuição Linux predefinida oferece bibliotecas Lua para este API. Por exemplo:
`local d = require("devices")`  
`local m = d:find("block_operations")`  
`m:excavate("front")`

### Lados
O parâmetro `side` nos presentes métodos representa um lado local ao bloco do dispositivo. Valores válidos são: `front`, `up` e `down`.

### Métodos
`excavate([side]):boolean` tenta quebrar um bloco na direção especificada. Blocos coletados serão inseridos a partir do slot de inventário atualmente selecionado. Se o slot selecionado estiver cheio, o próximo slot será utilizado e assim sucessivamente. Se o inventário nao possuír espaço para o bloco caídom ele irá cair no mundo.
- `side` é a direção relativa na qual quebrar o bloco. Opcional, por defeito possuí o valor `front`. Veja a secção "Lados".
- Retorna se a operação foi bem-sucedida.

`place([side]):boolean` tenta colocar um bloco na direção especificada. Blocos colocados serão extraídos a partir do slot de inventário atualmente selecionado. Se o slot selecionado estiver vazio, nenhum bloco será colocado.
- `side` é a direção relativa na qual quebrar o bloco. Opcional, por defeito possuí o valor `front`. Veja a secção "Lados".
- Retorna se a operação foi bem-sucedida.

`durability():number` retorna a durabilidade restante da ferramenta de escavação do módulo. Assim que a durabilidade atinge o valor zero, nenhuma operação de escavação será executada até que a ferramenta seja reparada.
- Retorna a durabilidade restante da ferramenta de escavação.

`repair():boolean` tenta reparar a ferramenta de escavação do módulo utilizando materiais no slot de inventário atualmente selecionado. Este método consumirá um item por vez. Qualquer ferramenta normal pode ser usada como fonte para materiais de reparo, como picaretas e pás. A qualidade da ferramenta afeta diretamente a quantidade de durabilidade restaurada.
- Retorna se algum material pode ser utilizado para reparar a ferramenta de escavação do módulo.
