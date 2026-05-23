//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
class Animal {
    void sound() {
        System.out.println("Animal sound");
    }
}

class Dog extends Animal {
    void sound() {
        System.out.println("Gâu gâu");
    }

    void run() {
        System.out.println("Dog running");
    }
}

public class Main {
    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        String a = "nguyenanhtu";
        String b = "nguyenanhtu";

        System.out.println(a == b);

        String c = new String("nguyenanhtu");

        System.out.println(c.equals(a) );


        String s =  new String("nguyenanhtu");

        s = "NGUYENANHTU";



        System.out.println(s);

        String s1 = "Nguyenanhtu";

        s1 = "NGUYENANHTU";
        System.out.println(s1 );
    }
}