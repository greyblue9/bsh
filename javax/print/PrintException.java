package javax.print;


public class PrintException extends Exception {

   public PrintException(String var1) {
      super(var1);
   }

   public PrintException(Exception var1) {
      super(var1);
   }

   public PrintException(String var1, Exception var2) {
      super(var1, var2);
   }
   
}