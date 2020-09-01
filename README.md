# CRUD Genérico
By Victor "Texugo" Assunção

## Execução
- No repl.it:
Executar utilizando o botão `Run`

- Outros modos:
Executar o arquivo `Main.java` com o JUnit

## Localização dos arquivos:
Raiz:
  - Main.java

src/main/java:
  - CRUD.java
  - Registro.java
  - Usuario.java

## Explicação
Obs.: Para uma explicação mais profunda, consultar os comentários na classe CRUD

### INSERÇÃO
É feita a leitura e atualização do último ID inserido, no cabeçalho do arquivo. Depois o ID do objeto é setado com o valor do atual ID e, então, é feita a escrita da lápide, tamanho e dados do objeto. O ID atual é retornado.

### LEITURA
É checada a validade do arquivo e a validade do ID inserido. Então é feita a busca sequencial, através dos registros do arquivo. Caso ele seja encontrado, é retornado o objeto.

### ATUALIZAÇÃO
É checada a validade do arquivo e do novo objeto para atualização. É feita a busca sequencial pelos registros e, caso seja encontrado, a atualização é feita da seguinte forma:
- Registro novo <= Registro atual: o byte array é sobrescrito com o novo;
- Registro novo > Registro atual: o registro atual é deletado e o novo registro é feio ao final do arquivo.

### DELEÇÃO
É checada a validade do arquivo e a validade do ID inserido. Então é feita a busca sequencial, através dos registros do arquivo. Caso ele seja encontrado, escreve na lápide o valor `1` para indicar que não é mais um registro válido para buscas.
