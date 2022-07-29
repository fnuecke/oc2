# Cartão de Som
![Menos som do silêncio](item:oc2:sound_card)

O cartão de som permite que sejam reproduzidos múltiplos efeitos de som da sua vasta biblioteca de amostras similares a sons reais. Devido a restrições internas de engenharia, a reprodução de efeitos sequencialmente requer uma pequena pausa. Tentativas de reproduzir qualquer som nessa janela temporal resultara em nenhum efeito produzido.

Este é um dispositivo de alto nível. Ele tem de ser controlado utilizando o API de dispositivos de alto nível. A distribuição Linux predefinida oferece bibliotecas Lua para este API. Por exemplo:
`local d = require("devices")`  
`local s = d:find("sound")`  
`s:playSound("entity.creeper.primed")`

## API
Nome do dispositivo: `sound`

### Métodos
`playSound(name:string[,volume:float,pitch:float])` reproduz o efeito sonoro com o nome especificado.
- `name` é o nome do efeito a ser reproduzido.
- `volume` é o volume no qual tocar o efeito, situado no intervalo de `0` a `1`, com `1` sendo o volume normal. Opcional, tendo valor por defeito de `1`.
- `pitch` é a afinação na qual tocar o efeito, situado no intervalo de `0,5` a `2`, com `1` sendo a afinação normal. Opcional, tendo valor por defeito de `1`.
- Lança se o nome especificado for inválido.

`findSound(name:string):table` retorna uma lista de efeitos sonoros disponíveis correspondentes ao nome fornecido. Note que o número de resultados é limitado, portanto, consultas excessivamente genéricas resultarão em resultados truncados.
- `name` é o nome do som a procurar.
- Retorna uma lista de nomes de efeitos sonoros correspondentes à string de consulta.