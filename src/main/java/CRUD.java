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

        if(file.length() == 0) {
            return null;
        }

        int lastID = file.readInt();
        if(ID > lastID) {
            return null;
        }

        T object = this.constructor.newInstance();
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

        if(object.getID() != ID) {
            return null;
        }

        return object;
    }

    public boolean update(T newObject) throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException {
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");

        if(file.length() == 0) {
            return false;
        }

        int lastID = file.readInt();
        if(newObject.getID() > lastID) {
            return false;
        }

        T object = this.constructor.newInstance();
        long tombstonePosition = 0;
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

        if(object.getID() != newObject.getID() || tombstonePosition == 0) {
            return false;
        }

        file.seek(tombstonePosition);
        file.readByte();

        short originalSize = file.readShort();
        byte[] newByteArray = newObject.toByteArray();
        if(newByteArray.length > originalSize) {
            file.seek(tombstonePosition);
            file.writeByte(1);  // Morreu, F
            file.seek(file.length());
            file.writeByte(0);  // Lápide nova, reviveu
            file.writeShort(newByteArray.length);
        }

        file.write(newByteArray);
        file.close();

        return true;
    }

    public boolean delete(int ID) throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException {
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");

        if(file.length() == 0) {
            return false;
        }

        int lastID = file.readInt();
        if(ID > lastID) {
            return false;
        }

        T object = this.constructor.newInstance();
        long tombstonePosition = 0;
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

        if(object.getID() != ID) {
            return false;
        }

        if(tombstonePosition != 0) {
            file.seek(tombstonePosition);
            file.writeByte(1);  // Press F to pay respect
        }

        return true;
    }
}
