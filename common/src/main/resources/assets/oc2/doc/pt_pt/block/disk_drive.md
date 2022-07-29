# Leitor de disco
![Leve-o a dar uma volta](block:oc2:disk_drive)

O leitor de disco fornece uma opção para troca de dados rápida. [Disquetes](../item/floppy.md) podem ser adicionadas e removidas a qualquer momento, ao contrário dos [discos rígidos](../item/hard_drive.md).

Note que é altamente recomendado explicitamente desmontar a disquete antes da sua remoção do computador para evitar perda de dados.

Num sistema Linux, leitores de disco irão aparecer tipicamente como dispositivos `/dev/vdX`, seguindo quaisquer discos rígidos instalados. Eles não são automaticamente formatados ou montados, mas irão apenas aparecer como blocos de dispositivos crús. Para os usar, eles precisam de ser configurados primeiro. Por exemplo, na distribuição Linux predefinida os comandos seguintes serão úteis:

- `mke2fs /dev/vdX` formata uma disquete. Execute este comando quando instalar o disco pela primeira vez. *Irá eliminar dados na disquete!*
- `mount /dev/vdX <mount directory>` monta uma disquete após formatar a mesma. Garanta que o diretório onde você quer montar esta disquete existe e está vazio.
- `umount <mount directory>` desmonta uma disquete. Execute este comando antes de remover a disquete do leitor de disco para evitar perda de dados.

Computadores *precisam de ser desligados* antes de instalar ou remover este componente. A instalação do mesmo enquanto o computador está ligado resultará em nenhum efeito, porém a sua remoção poderá resultar em erros de sistema.
