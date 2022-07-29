# Cartão de Importação/Exportação de Ficheiros
![Destrua-se esta parede!](item:oc2:file_import_export_card)

O cartão de importação/exportação de ficheiros permite a quebra da quarta parede. Ele permite:
- carregar ficheiros do seu computador real para um [computador](../block/computer.md) virtual.
- descarregar ficheiros dum computador virtual para o seu computador real.

Por conveniência, a distribuição Linux predefinida fornece dois scripts de utilidade para essas operações: `import.lua` and `export.lua`.

`import.lua` irá, quando executado, pedir um ficheiro para importar para o computador virtual. O ficheiro irá ser armazenado no diretório de trabalho atual. Se um ficheiro com nome do ficheiro importado já existir, a opção para fornecer um nome alternativo será oferecida.

`export.lua` recebe como primeiro parâmetro o caminho do ficheiro no computador virtual. Após isso, ele descarrega o ficheiro para o seu computador real e oferece uma caixa de diálogo de salvar, oferecendo a escolha de onde salvar o ficheiro descarregado ou cancelar a operação.

Ambos os scripts irão pedir a todos os utilizadores atualmente interagindo com o terminal do computador. Para operações de carregamento (`import.lua`), o primeiro ficheiro carregado será utilizado. Os pedidos nos outros clientes serão cancelados. Para operações de descarregamento, a todos os clientes será oferecida a opção de salvar o ficheiro exportado. 
