
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

/*
SchedulerBank - класс планировщик работы банка.
1. Открывает банк. Запрос времени работы
2. Заставляет банк каждые 17 секунд запрашивать новый курс
3. При закрытии ждет пока доработают кассы и сервис рассылок
*/

class SchedulerBank {
    ScheduledExecutorService scheduledExecutorService;
    record TimeBank(int start, int finish) { };
    private final Bank bank;

    SchedulerBank(Bank bank) {
        this.bank = bank;
    }

    public void work() {

        TimeBank timeBank = getTime(); //Запрос времени работы банка
        int startBankTime = timeBank.start;
        int closeBankTime = timeBank.finish;

        scheduledExecutorService = Executors.newScheduledThreadPool(2);
        //Планирую задачи к.т не зависят от возвращаемого результата
        Runnable openBank = bank::BankOpen;
        Runnable checkRates = bank::updateRate;

        //Задача на завершение работы банка
        Callable<Boolean> closedBank = bank::BankClosed;
        Callable<Boolean> sendNotifBank = bank::bankWorking;

        scheduledExecutorService.schedule(openBank, startBankTime, TimeUnit.SECONDS);
        scheduledExecutorService.scheduleAtFixedRate(checkRates, startBankTime, 15, TimeUnit.SECONDS);
        ScheduledFuture<Boolean> sendNotificationBank = scheduledExecutorService.schedule(sendNotifBank, startBankTime, TimeUnit.SECONDS);
        ScheduledFuture<Boolean> CloseBank = scheduledExecutorService.schedule(closedBank, closeBankTime, TimeUnit.SECONDS);
        checkStop(CloseBank, sendNotificationBank); //Завершение
    }

    private TimeBank getTime() {
        Scanner scanner = new Scanner(System.in);
        int startBankTime = 0;
        int closeBankTime = 0;

        while (true) {
            System.out.println("Введите количество секунд, через которое банк начнет работу");
            System.out.print(">");
            try {
                startBankTime = scanner.nextInt();
                System.out.println("Введите количество секунд, через которое банк завершит работу");
                System.out.print(">");
                closeBankTime = scanner.nextInt();
                if (startBankTime >= 0 && closeBankTime > startBankTime) {
                    break;
                }else {
                    System.out.println("Время окончания должно быть больше 0 и время окончания позже времени начала.Повторите ввод");
                }
            } catch (InputMismatchException e) {
                System.out.println("Неверный формат. Повторите ввод ");
                scanner.next();
            }
        }
        scanner.close();
        return new TimeBank(startBankTime, closeBankTime);

    }
    private void checkStop (ScheduledFuture<Boolean> CloseBank, ScheduledFuture<Boolean> sendNotificationBank){
        //Проверка на закрытие банка - ждем когда доработают кассы
        try {
            while (true) {
                boolean StopThreadsBankClose = CloseBank.get();
                if (StopThreadsBankClose) {
                    break;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Основной поток: Ошибка при закрытии банка");
        }
        //После закрытия работы касс закрываем поток планировщика
        System.out.println("Планировщик ожидает завершения работы сервиса рассылки");
        try {
            while (true) {
                boolean StopNotify = false;
                StopNotify  = sendNotificationBank.get();
                if (StopNotify) {
                    break;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Основной поток: Ошибка при закрытии банка");
        }
        System.out.println("Планировщик завершает работу");
        scheduledExecutorService.shutdown();
    }

}
/* SchedulerExchangeRate - планировщик пула потока для обновления курса валют.
Класс ExchangeRateService(Биржа) - существует в единственном экземпляре и банк должен периодически сверять с ней свои данные.
Генерирует новый курс каждые 15 секунд
*/
class SchedulerExchangeRate{
    ScheduledExecutorService scheduledExecutorService;

    public void startExchangeRate () {
        ExchangeRateService exchangeRateService = ExchangeRateService.getInstance();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate( exchangeRateService, 0 , 20, TimeUnit.SECONDS );
    }
    public void stopExchangeRate() {
        scheduledExecutorService.shutdown();
        try {
            if(! scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) { //Принудительное закрытие по Timeout
                scheduledExecutorService.shutdownNow(); //Таймаут истек
            }
        }catch (InterruptedException e) {
            System.out.println("Ожидание завершения потока было прервано.Принудительно завершаю");
            scheduledExecutorService.shutdownNow();
        }
    }
}

/* Класс SchedulerClients - создание пул-потоков клиентов на основе данных из файла
Количество клиентов передается из главного менеджера
*/
class SchedulerClients{
    ExecutorService clientThreads;

    public void startClients( List<Client> clients) {
        System.out.println("Количество запущенных потоков клиентов - " + clients.size());
        clientThreads = Executors.newFixedThreadPool(clients.size());
        for( Client client : clients) {
            clientThreads.submit(client);
        }
}
     public void stopClients() {
         clientThreads.shutdown(); //Завершаю работу клиентов
         try {
             System.out.println("Ожидаю завершения работы клиентов в течение 5 секунд");
             boolean timeout =  clientThreads.awaitTermination(5, TimeUnit.SECONDS);
             if (timeout) {
                 System.out.println( "Все потоки клиентов завершены вовремя");
             } else {
                 clientThreads.shutdownNow();
             }
         }catch (InterruptedException e) {
             System.err.println("Основной поток был прерван во время ожидания завершения потоков клиентов");
             clientThreads.shutdownNow();
             Thread.currentThread().interrupt();
         }
     }
}
