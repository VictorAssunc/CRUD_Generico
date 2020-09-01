import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class CRUD<T extends Registro> {
    public static final String __author__ = "Texugo";

    public final Constructor<T> constructor;
    public final String fileName;

    public CRUD(Constructor<T> constructor, String fileName) {
        this.constructor = constructor;
        this.fileName = fileName;
    }

    public int create(T object) throws IOException {
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");

        int nextID = 1;
        try {
            // LEITURA DO ÚLTIMO ID
            nextID += file.readInt();
        } catch (IOException ignored) {}
        finally {
            // ALTERAÇÂO DO ÚLTIMO ID
            file.seek(0);
            file.writeInt(nextID);
        }

        // ALTERAÇÃO DO ID DO OBJETO
        object.setID(nextID);

        // ESCRITA NO ARQUIVO
        file.seek(file.length());
        byte[] byteArray = object.toByteArray();
        file.writeByte(0);  // Lápide
        file.writeShort(byteArray.length);    // Tamanho
        file.write(byteArray);  // Dados
        file.close();

        return nextID;
    }

    public T read(int ID) throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException {
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");

        // CHECA SE O ARQUIVO ESTÁ VAZIO
        if(file.length() == 0) {
            return null;
        }

        // CHECA SE O ID ALVO É MAIOR QUE O ÚLTIMO ID INSERIDO
        int lastID = file.readInt();
        if(ID > lastID) {
            return null;
        }

        T object = this.constructor.newInstance();
        /*
            ENQUANTO O ID DO OBJETO FOR DIFERENTE DO ID ALVO E NÃO TENHA ALCANÇADO O FIM DO ARQUIVO
            É FEITA A LEITURA DO PRÓXIMO BYTE, QUE SERÁ A LÁPIDE DO REGISTRO, ONDE É CHECADO SEU ESTADO E TEM DUAS POSSIBILIDADES:
                - ARQUIVO MORTO (LÁPIDE = 1):
                    OS PRÓXIMOS BYTES SÃO LIDOS, REFERENTES AO TAMANHO DO REGISTRO, ENTÃO O PONTEIRO DE LEITURA
                    PULA A QUANTIDADE LIDA DE BYTES E VOLTA AO INÍCIO DO LOOP.
                - ARQUIVO ATIVO (LÁPIDE = 0):
                    A LEITURA DOS PRÓXIMOS BYTES É REALIZADA E USADA COMO O TAMANHO DO BYTE ARRAY E OS DADOS DO REGISTRO
                    SÃO LIDOS E ARMAZENADOS NO BYTE ARRAY E, ENFIM, PASSADO PARA O OBJETO UTILIZADO O MÉTODO `fromByteArray()`
        */
        while(object.getID() != ID && file.getFilePointer() != file.length()) {
            if(file.readByte() != 0) {  // Leitura da lápide
                short bytesToNext = file.readShort();
                file.skipBytes(bytesToNext);
                continue;
            }

            short dataLength = file.readShort();
            byte[] data = new byte[dataLength];
            file.read(data);
            object.fromByteArray(data);
        }

        // CHECA SE O OBJETO FINAL É O PROCURADO
        if(object.getID() != ID) {
            return null;
        }

        return object;
    }

    public boolean update(T newObject) throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException {
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");

        // CHECA SE O ARQUIVO ESTÁ VAZIO
        if(file.length() == 0) {
            return false;
        }

        // CHECA SE O ID DO OBJETO É MAIOR QUE O ÚLTIMO ID INSERIDO
        int lastID = file.readInt();
        if(newObject.getID() > lastID) {
            return false;
        }

        T object = this.constructor.newInstance();
        long tombstonePosition = 0;
        /*
            ENQUANTO O ID DO OBJETO FOR DIFERENTE DO ID ALVO E NÃO TENHA ALCANÇADO O FIM DO ARQUIVO
            O `tombstonePosition` equivale à localização da lápide do registro atual.
            É FEITA A LEITURA DO PRÓXIMO BYTE, QUE SERÁ A LÁPIDE DO REGISTRO, ONDE É CHECADO SEU ESTADO E TEM DUAS POSSIBILIDADES:
                - ARQUIVO MORTO (LÁPIDE = 1):
                    OS PRÓXIMOS BYTES SÃO LIDOS, REFERENTES AO TAMANHO DO REGISTRO, ENTÃO O PONTEIRO DE LEITURA
                    PULA A QUANTIDADE LIDA DE BYTES E VOLTA AO INÍCIO DO LOOP.
                - ARQUIVO ATIVO (LÁPIDE = 0):
                    A LEITURA DOS PRÓXIMOS BYTES É REALIZADA E USADA COMO O TAMANHO DO BYTE ARRAY E OS DADOS DO REGISTRO
                    SÃO LIDOS E ARMAZENADOS NO BYTE ARRAY E, ENFIM, PASSADO PARA O OBJETO UTILIZADO O MÉTODO `fromByteArray()`
        */
        while(object.getID() != newObject.getID() && file.getFilePointer() != file.length()) {
            tombstonePosition = file.getFilePointer();
            if(file.readByte() != 0) {  // Leitura da lápide
                short bytesToNext = file.readShort();
                file.skipBytes(bytesToNext);
                continue;
            }

            short dataLength = file.readShort();
            byte[] data = new byte[dataLength];
            file.read(data);
            object.fromByteArray(data);
        }

        // CHECA SE O OBJETO FINAL É O PROCURADO OU SE A POSIÇÃO DA ÚLTIMA LÁPIDE LIDA É IGUAL AO COMEÇO DO ARQUIVO
        if(object.getID() != newObject.getID() || tombstonePosition == 0) {
            return false;
        }

        // CASO TENHA ENCONTRADO O REGISTRO, O PONTEIRO DE LEITURA RETORNA PRA POSIÇÃO DA ÚLTIMA LÁPIDE E FAZ LEITURA DELA
        // MAS ESSA LEITURA NÃO É ARMAZENADA, SERVE APENAS PARA PULAR PRO CAMPO DE TAMANHO DO REGISTRO
        file.seek(tombstonePosition);
        file.readByte();

        // É FEITA A LEITURA DO TAMANHO DO REGISTRO ATUAL
        short originalSize = file.readShort();
        // O OBJETO COM AS ATUALIZAÇÕES É TRANSFORMADO EM BYTE ARRAY
        byte[] newByteArray = newObject.toByteArray();
        // O TAMANHO DO NOVO BYTE ARRAY É COMPARADO COM O ANTIGO
        if(newByteArray.length > originalSize) {
            // CASO O NOVO SEJA MAIOR, O ATUAL REGISTRO É APAGADO E A NOVA LÁPIDE, JUNTAMENTE COM O NOVO TAMANHO, SÃO ESCRITOS NO FIM DO ARQUIVO
            file.seek(tombstonePosition);
            file.writeByte(1);  // Morreu, F
            file.seek(file.length());
            file.writeByte(0);  // Lápide nova, reviveu
            file.writeShort(newByteArray.length);
        }

        // CASO O NOVO REGISTRO TENHO TAMANHO MENOR OU IGUAL, ELE SÓ IRÁ SOBRESCREVER O ATUAL
        // EM AMBOS OS CASOS, A FUNÇÃO DE ESCRITA SERÁ REALIZAD DA MESMA MANEIRA, POR ISSO ESTÁ FORA DO IF
        file.write(newByteArray);
        file.close();

        return true;
    }

    public boolean delete(int ID) throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException {
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");

        // CHECA SE O ARQUIVO ESTÁ VAZIO
        if(file.length() == 0) {
            return false;
        }

        // CHECA SE O ID DO OBJETO É MAIOR QUE O ÚLTIMO ID INSERIDO
        int lastID = file.readInt();
        if(ID > lastID) {
            return false;
        }

        T object = this.constructor.newInstance();
        long tombstonePosition = 0;
        /*
            ENQUANTO O ID DO OBJETO FOR DIFERENTE DO ID ALVO E NÃO TENHA ALCANÇADO O FIM DO ARQUIVO
            O `tombstonePosition` equivale à localização da lápide do registro atual.
            É FEITA A LEITURA DO PRÓXIMO BYTE, QUE SERÁ A LÁPIDE DO REGISTRO, ONDE É CHECADO SEU ESTADO E TEM DUAS POSSIBILIDADES:
                - ARQUIVO MORTO (LÁPIDE = 1):
                    OS PRÓXIMOS BYTES SÃO LIDOS, REFERENTES AO TAMANHO DO REGISTRO, ENTÃO O PONTEIRO DE LEITURA
                    PULA A QUANTIDADE LIDA DE BYTES E VOLTA AO INÍCIO DO LOOP.
                - ARQUIVO ATIVO (LÁPIDE = 0):
                    A LEITURA DOS PRÓXIMOS BYTES É REALIZADA E USADA COMO O TAMANHO DO BYTE ARRAY E OS DADOS DO REGISTRO
                    SÃO LIDOS E ARMAZENADOS NO BYTE ARRAY E, ENFIM, PASSADO PARA O OBJETO UTILIZADO O MÉTODO `fromByteArray()`
        */
        while(object.getID() != ID && file.getFilePointer() != file.length()) {
            tombstonePosition = file.getFilePointer();
            if(file.readByte() != 0) {  // Leitura da lápide
                short bytesToNext = file.readShort();
                file.skipBytes(bytesToNext);
                continue;
            }

            short dataLength = file.readShort();
            byte[] data = new byte[dataLength];
            file.read(data);
            object.fromByteArray(data);
        }

        // CHECA SE O OBJETO FINAL É O PROCURADO
        if(object.getID() != ID) {
            return false;
        }

        // CHECA SE A POSIÇÃO DA ÚLTIMA LÁPIDE LIDA É IGUAL AO COMEÇO DO ARQUIVO
        if(tombstonePosition != 0) {
            // CASO SEJA UMA POSIÇÃO VÁLIDA, ESCREVE 1 NA LÁPIDE, PARA INDICAR A DELEÇÃO
            file.seek(tombstonePosition);
            file.writeByte(1);  // Press F to pay respect
        }

        return true;
    }
}
