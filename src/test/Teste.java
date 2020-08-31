import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.Assert;

public class Teste {
    public static final String __author__ = "Texugo";

    public static void main(String[] args) throws Exception {
        // PRÉ-TESTE
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        Date date = format.parse("24/08/1975");
        Usuario user1 = new Usuario(-1, "batatinha", "senhaforte", "Ludosvaldo", "lulu.dosvaldo@gmail.com", date);
        date = format.parse("12/05/1987");
        Usuario user2 = new Usuario(-1, "coxinha", "senhaultraforte", "Irineu", "iri.neu@gmail.com", date);
        date = format.parse("06/02/200");
        Usuario user3 = new Usuario(-1, "sucrilhos", "senhafraca", "Clodovil", "clo.dovilo@gmail.com", date);

        new File("teste.db").delete();
        CRUD<Usuario> file = new CRUD<>(Usuario.class.getConstructor(), "teste.db");

//        System.out.println(user1.toByteArray().length);
//        System.out.println(file.create(user1));
//        System.out.println(file.create(user2));
////        System.out.println(file.delete(1));
//        System.out.println(file.read(1));
//        user1.setUser("batatinho");
//        System.out.println(file.update(user1));
//        System.out.println(file.read(1));
//        user1.setUser("bata");
//        System.out.println(file.update(user1));
//        System.out.println(file.read(1));
//        user1.setUser("Batattaatatatattatatatattaata");
//        System.out.println(file.update(user1));
//        System.out.println(file.read(1));
//        System.out.println(file.read(2));


        // TESTES
        // OBS.: As mensagens dentro dos asserts são apenas mensagens de erro! Só aparecem quando a asserção não se valida!

        // INSERÇÃO NO ARQUIVO VAZIO
        Assert.assertEquals("usuário com ID errado", 1, file.create(user1));
        Assert.assertEquals("usuário com ID errado", 2, file.create(user2));
        Assert.assertEquals("usuário com ID errado", 3, file.create(user3));

        // BUSCA NO ARQUIVO
        Assert.assertEquals("usuário inserido errado", user1.toString(), file.read(1).toString());
        Assert.assertEquals("usuário inserido errado", user2.toString(), file.read(2).toString());
        Assert.assertEquals("usuário inserido errado", user3.toString(), file.read(3).toString());

        // DELEÇÃO
        Assert.assertTrue("falha na deleção do usuário", file.delete(2));
        Assert.assertEquals("usuário com ID = 1 está errado", user1.toString(), file.read(1).toString());
        Assert.assertNull("usuário com ID = 2 não foi deletado", file.read(2));
        Assert.assertEquals("usuário com ID = 3 está errado", user3.toString(), file.read(3).toString());

        // REINSERÇÃO NO ARQUIVO
        Assert.assertEquals("usuário com ID errado", 4, file.create(user2));
        Assert.assertEquals("usuário com ID = 4 está errado", user2.toString(), file.read(4).toString());

        // ATUALIZAÇÃO
        // Novo registro com tamanho igual
        int originalSize = user1.toByteArray().length;
        user1.setUser("batatinho");
        int newSize = user1.toByteArray().length;
        Assert.assertEquals("o tamanho dos dois registros devem ser iguais", originalSize, newSize);
        Assert.assertTrue("falha na atualização do usuário", file.update(user1));
        Assert.assertEquals("o campo user não atualizou", "batatinho", file.read(1).getUser());
        Assert.assertEquals("o usuário não atualizou", user1.toString(), file.read(1).toString());

        // Novo registro com tamanho menor
        originalSize = user1.toByteArray().length;
        user1.setUser("bata");
        newSize = user1.toByteArray().length;
        Assert.assertTrue("o tamanho do novo registro deve ser menor que o original", newSize < originalSize);
        Assert.assertTrue("falha na atualização do usuário", file.update(user1));
        Assert.assertEquals("o campo user não atualizou", "bata", file.read(1).getUser());
        Assert.assertEquals("o usuário não atualizou", user1.toString(), file.read(1).toString());

        // Novo registro com tamanho maior
        originalSize = user1.toByteArray().length;
        user1.setUser("batatatatatatatata");
        newSize = user1.toByteArray().length;
        Assert.assertTrue("o tamanho do novo registro deve ser maior que o original", newSize > originalSize);
        Assert.assertTrue("falha na atualização do usuário", file.update(user1));
        Assert.assertEquals("o campo user não atualizou", "batatatatatatatata", file.read(1).getUser());
        Assert.assertEquals("o usuário não atualizou", user1.toString(), file.read(1).toString());
    }
}
