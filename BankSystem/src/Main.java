import java.io.IOException;
import java.util.List;
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {//TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        try {
            MainManager.start();
        } catch (NoWorkerException e) {
            System.out.println(e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("В данных клиентов в файле критическая ошибка:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("При работе с файлом возникла критическая ошибка:" + e.getMessage());
        }
    }
}
/* Класс MainManager запускает и завершает все события в программе
1 Создает объект Банк(Bank)
2. Создание сервиса рассылок(отдельная сущность) - mainSenderNotification
   2.1 Он взаимодействует с банком через прокси - serviceManagerReceiversNotify.
   2.2 Связывание банка с сервисом рассылок (метод setSendler)
3 Создание клиентов по данным файла(CreateClientsBank)
4 Запуск потоков клиентов(класс SchedulerClients)
5 Запуск планировщика курсов валют (класс SchedulerExchangeRate).
Также отдельная сущность, которая обновляет курс каждые 10 секунд
6.Запуск планировщика банка ( класс SchedulerBank )
7.Закрытие пулов потоков по завершению работы
*/

class MainManager {
    public static void start() throws IllegalArgumentException, IOException {

        Bank bank = new Bank( );
        System.out.println("Создан банк" );

        ServiceManagerReceiversNotify serviceManagerReceiversNotify = new ServiceManagerReceiversNotify(bank );
        MainSenderNotification  mainSenderNotification = new MainSenderNotification(serviceManagerReceiversNotify);
        bank.setSendler(mainSenderNotification); //Связывание банка с сервисом рассылок

        CreateClientsBank createClients = new CreateClientsBank();
        List<Client> clients = createClients.readFile("clients.txt");

        if (clients.isEmpty()){
            throw new NoWorkerException("Нет клиентов для работы.Работа банка невозможна");
        }
             for( Client client : clients) {
                 client.setBank(bank); //Передача ссылки на банк клиенту(отношение - агрегация)
                 bank.addObserver(client); //Подписываем нового клиента на банк
                 bank.addClients(client); //Добавление банковских клиентов
             }

             SchedulerClients clientsThreads = new SchedulerClients(); //Запуск потоков клиентов
             clientsThreads.startClients(clients);

             SchedulerExchangeRate exchangeRate = new SchedulerExchangeRate();  //Создаем планировщик для курсов валют
             exchangeRate.startExchangeRate();//Запускаем обновление курса валют - каждые 10 секунд

             SchedulerBank schedulerBank = new SchedulerBank(bank); //Создаем планировщик банка
             schedulerBank.work(); //Запускаем планировщик для банка

             clientsThreads.stopClients(); //Завершение потоков клиентов
             exchangeRate.stopExchangeRate(); //Завершение работы курса валют
    }
}

class NoWorkerException  extends RuntimeException{
    public NoWorkerException(String message) {
        super(message);
    }
}

interface Observer {
    int getID();
    void onBancEvents ( Notification message);
}










