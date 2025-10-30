import java.io.*;
import java.io.FileWriter;

 /*Класс LoggerSinglton.
 Реализует паттерн Singleton.
 Осуществляет запись в файл в режиме реального времени
 Для закрытия использую shutdown hook
*/
 class LoggerSinglton {
     private static final String LogName = "logWorkBank.txt";
     private static LoggerSinglton inctance;
     private  FileWriter writer = null;

     private LoggerSinglton() {
         //Открытие файла для записи при создании единственного экземпляра
         try {
             this.writer = new FileWriter(LogName, false);

         } catch (IOException e) {
             System.err.println("Ошибка при открытии файла логов: " + e.getMessage());
         }
         System.out.println("Файл логирования открыт для записи");

         //Регистрируем Shutdown hook  при создании экземпляра
         Runtime.getRuntime().addShutdownHook(new Thread(() -> {
             System.out.println("Закрытие файла логов");
             if ( writer != null) {
                 try {
                     writer.close();
                 } catch (IOException e) {
                     System.err.println("Ошибка при закрытии файла логов: " + e.getMessage());
                 }
             }
         }));
     }

     public static LoggerSinglton getInctance() {
         if (inctance == null) {
             inctance = new LoggerSinglton();
         }
         return inctance;
     }

     public void write(String message) throws IOException {
         writer.write(message + "\n");
         writer.flush(); //Сбрасываю буфер чтобы данные писались в режиме реального времени
     }
 }







