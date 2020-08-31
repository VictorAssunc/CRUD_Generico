import java.io.IOException;

public interface Registro {
    public static final String __author__ = "Texugo";

    public void setID(int ID);
    public int getID();
    public byte[] toByteArray() throws IOException;
    public void fromByteArray(byte[] byteArray) throws IOException;
}
