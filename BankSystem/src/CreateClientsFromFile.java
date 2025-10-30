import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/* CreateClientsBank - создание клиентов по строкам из файла(clients.txt)
@readFile(public)
   Вход - путь к файлу,
   Выход - динамический массив Client
2.@checkFilename(private) - проверка валидности файла (файл не пустой, по указанному пути существует).
  Вход - путь к файлу,
  Выход - boolean результат проверки
3.@parsing(private) - построчно считывает файл. Каждую строку передает в handleLine
4.@handleLine(private) - парсинг по разделителю запятая. Проверка валидности данных
  Вход - строка из файла
*/

 class  CreateClientsBank  {
    private String filename;
    private final List <Client> clients = new ArrayList<>();


    public List<Client> readFile (String input) throws IllegalArgumentException, IOException{
        if(checkFilename(input)){
            parsing();
            return clients;
        }
         return null;
      }

 private boolean checkFilename (String input)  {
      if (input == null || input.trim().isEmpty()) {
          throw new IllegalArgumentException( "Имя файла не может быть пустым");
      }
      try{
      Path path = Paths.get(input);
      if(!Files.exists(path)){
          throw new IllegalArgumentException("По указанному пути файла не существует");
      }
      }catch(InvalidPathException e) {
          throw new IllegalArgumentException("Некорректный синтаксис пути к файлу");
      }
        filename = input;
        return true;
    }

    private void parsing() throws IOException {
     try (BufferedReader reader = new BufferedReader(new FileReader( filename))) {
            String line;

            System.out.println("Из файла считаны следующие записи");
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                handleLine(line);
            }
        }
    }
    private void handleLine(String line ) {
     String[] parts = line.split(",");
        if (parts.length < 3 ){
            System.out.println("Не удалось получить данные о клиенте " + line );
            return;
        }
        try{
            int id = Integer.parseInt(parts[0].trim());
            double balance = Math.round((Double.parseDouble(parts[1].trim())) * 100.0) /100.0;
            if (balance < 0) {
                System.out.println("ОШИБКА: Баланс клиента " +id + " отрицательный" );
                return;
            }
            ExistCurrency currency = ExistCurrency.valueOf(parts[2].trim());
            clients.add(new Client(id, balance, currency ));
            System.out.println("Клиент " +id + " успешно создан. Баланс "+balance +currency);
        } catch (IllegalArgumentException e) {
            System.out.println("ОШИБКА: Неверный формат параметра: " + e.getMessage());
        }catch (ArrayIndexOutOfBoundsException e ) {
            System.out.println("ОШИБКА: Неверная структура записи : " + e.getMessage());
        }
    }
}