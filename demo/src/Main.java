//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
interface Payment
{
    void Pay();
}

class Paypalpayment implements Payment
{
    @Override
    public void Pay()
    {
        System.out.println("Paypal payment method");
    }
}

class VNPayment implements Payment
{
    @Override
    public void Pay()
    {
        System.out.println("VNPayment method");
    }
}

public class Main {
    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
      Payment pay = new Paypalpayment();

      pay.Pay();
    }
}