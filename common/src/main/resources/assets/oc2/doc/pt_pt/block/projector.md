# Projetor
![Sombra na parede](block:oc2:projector)

O projetor fornece um dispositivo framebuffer a [computadores](computer.md). Eles possuem uma resolução de 640 por 480 píxeis, como o formato de cor r5g6b5: 5 bits para o componente de cor vermelha, 6 bits para o componente de cor verde e 5 bits para o componente de cor azul.

Projetores necessitam de ser energizados diretamente para funcionar. O dispositivo BUS não consegue fornecer energia suficiente por si só. Quando subenergizado, o mesmo indica através de um brilho vermelho na lente do projetor.

Num sistema Linux, projetores irão tipicamente aparecer como dispositivos `/dev/fbX`. Para enviar dados para o framebuffer, é possível escrever para esses dispositivos. Por exemplo, para limpar um framebuffer, é possível passar uma cadeia de zeros a esse dispositivo da seguinte maneira: `cat /dev/zero > /dev/fb0`.

Ao usar a distribuição Linux predefinida, o [teclado](keyboard.md) pode ser utilizado para enviar entradas para o terminal virtual, executando no primeiro framebuffer conectado.

Computadores *precisam de ser desligados* antes de instalar ou remover este componente. A instalação do mesmo enquanto o computador está ligado resultará em nenhum efeito, porém a sua remoção poderá resultar em erros de sistema.