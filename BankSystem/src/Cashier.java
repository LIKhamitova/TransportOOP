
import org.jetbrains.annotations.NotNull;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/* Класс Cashier.
Хранит ссылку на банк. Связь - композиция
1. Отправляет сообщение о начале работы
2. Пока банк открыт (bank.isOpen()) запрашивает задачи из очереди (executeTask)
Доступны следующие операции: {DEPOSIT, TRANSFER, EXCHANGE, WITHDRAWAL}
3. После завершения работы банка дорабатывают оставшиеся задачи из очереди
*/


public class Cashier implements Runnable {
    private final String nameCashier;
    private final Bank bank;
    CountDownLatch finalCashier;

    Cashier(String nameCashier, Bank bank, CountDownLatch finalCashier) {
        this.nameCashier = nameCashier;
        this.bank = bank;
        this.finalCashier = finalCashier;
    }

    @Override
    public void run() {
        Task task;
        try {
            ALLNotification newMessage = new ALLNotification("Касса " + this.nameCashier, " начала работy");
            bank.getSenderNotification().addMessage(newMessage);
            System.out.println(newMessage);

            while (bank.isOpen().get()) { //Проверяем постоянно очередь пока банк работает
                if ((task = bank.takeTask()) != null) {
                    Thread.sleep(9000);
                    executeTask(task);
                    Thread.sleep(9000);
                }
            }
            System.out.println("Кассы  дорабатывают задачи из очереди..... " + nameCashier);
            while ((task = bank.takeTask()) != null) { //После окончания работы банка, кассы работают пока есть в очереди задачи
                System.out.println("Беру из очереди задачу " +task );
                executeTask(task);

            }
            ALLNotification newMessageEnd = new ALLNotification("Касса " + this.nameCashier, " завершила работy.....");
            bank.getSenderNotification().addMessage(newMessageEnd);
            System.out.println(newMessageEnd);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Кассир " + nameCashier + " был прерван");
        } finally {
            finalCashier.countDown();
            System.out.println( nameCashier + " сбросила счетчик");
        }
    }

    /* Решение задач клиентов
    Перед решением любой задачи, кроме EXCHANGE(т.к. она никак не затрагивает баланс пользователя),
    Кассир пытается получить блокировку client.getLock()
    Только после ее получения выполняется расчет и изменение баланса
    */

   private void executeTask(Task task) {
       switch (task.getOperation()) {

           case BankOperation.DEPOSIT: {
               Client client = bank.getClientID(task.getId()); //Запрос клиента по ID
               ReentrantLock clientlock = client.getLock();
               clientlock.lock(); //Пытаемся заблокировать баланс клиента - ждем
               try {
                   bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Начал обработку:" + task, task.getId()));
                   deposit(client, task);
               } finally {
                   clientlock.unlock();
               }
               break;
           }

           case BankOperation.WITHDRAWAL: {
                    Client client = bank.getClientID(task.getId()); //Запрос клиента по ID
                    ReentrantLock clientlock = client.getLock();
                    clientlock.lock(); //Пытаемся заблокировать баланс клиента - ждем
                    try {
                        bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Начал обработку:" + task, task.getId()));
                        withdrawal(client, task);
                    } finally {
                        clientlock.unlock();
                    }
                    break;
           }
           case BankOperation.TRANSFER: {
                     Client client= bank.getClientID(task.getId());

                    if (task instanceof TransferTask transferTask) {
                        Client receiver = bank.getClientID(transferTask.getIdReceiver()); //Запрос получателя по ID

                        Client firstLock = client.getID() < receiver.getID() ? client : receiver; //Порядок блокировок по ID- сначала с меньшим
                        Client secondLock = client.getID() < receiver.getID() ? receiver : client;

                        ReentrantLock firstlock = firstLock.getLock();
                        firstlock.lock(); //Пытаемся заблокировать баланс клиента - ждем
                        try {
                            Lock secondwriteLock = secondLock.getLock();
                            secondwriteLock.lock(); //Пытаемся заблокировать баланс получателя - ждем
                            try {
                                bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Начал обработку:" + task, task.getId()));
                                transfer(client, task, receiver);
                            } finally {
                                secondwriteLock.unlock();
                            }
                        } finally {
                            firstlock.unlock();
                        }
                    }else{
                        bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {"+task.getUUID()+"}отклонена. Некорректный тип задачи на перевод" + task, task.getId()));
                 }
                    break;
             }
             //Не блокирую клиента. Т.к. не провожу операции с балансом
             case BankOperation.EXCHANGE: {
                 bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Начал обработку:" + task, task.getId()));
                 exchange(task);
                 break;
             }

           default: {
               bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {"+task.getUUID()+"} отклонена. Банковская операция не найдена" + task, task.getId()));
               break;
           }
       }
   }


//Депозит
// 1. Запрос баланса
// 2. Запрос валюты
// 3. Проверка на валюты и необходимость конвертации(конвертация метод CheckAmountWithRate)
// 4. Сохранение нового баланса
// 5. Отправка уведомления пользователю о результатах обработки

    private void deposit(@NotNull Client client, @NotNull Task task) {

        double oldBalance = client.getBalance();
        ExistCurrency clientCurrency = client.getCurrency();
        ExistCurrency taskCurrency = task.getCurrency();
        double tastAmount = task.getAmount();
        try{
            if (clientCurrency != taskCurrency) { //Нужна ли конвертация
                ExchangeResult exchangeResult = CheckAmountWithRate(taskCurrency, clientCurrency,  tastAmount); //Проверка валюты, конвертация
                double newBalance = Math.round((oldBalance + exchangeResult.amount ) * 100.0) / 100.0;
                client.changeBalance(newBalance);
                bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {" +task.getUUID() + "} выполнена: баланс клиента: до " + oldBalance +  clientCurrency + " после " + newBalance + clientCurrency + " обменный  курс " + exchangeResult.rate + exchangeResult.pair, task.getId()));
            }else {
                double newBalance = Math.round( (oldBalance + tastAmount) * 100.0 ) / 100.0;
                client.changeBalance(newBalance);
                bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {" +task.getUUID() + "} выполнена: баланс клиента: до " + oldBalance + clientCurrency + " после " + newBalance + clientCurrency + " Конвертация не проводилась ", task.getId()));
            }
        } catch (NoSuchElementException | IllegalArgumentException e ) {
            bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {" +task.getUUID() + " отклонена. " + e.getMessage(), task.getId()));
        }
    }


    //Снятие
    // 1. Запрос баланса - если баланс ноль, сразу отказ
    // 1.1 Баланс - результат вычисления и поэтому сравниваю только целую часть
    // Округление должно идти в меньшую сторону
    // 2. Запрос курса валюты клиента и конвертация при необходимости
    // 3. Проверка достаточно ли средств на счете - нет отказ
    // 4. Изменение баланса сотрудника
    // 5. Информирование пользователя о результатах

    private void withdrawal(Client client, Task task){
        double oldBalance = client.getBalance(); //Проверка баланса клиента до операции
        int oldBalanceRound = (int)oldBalance; //Проверяем только целую часть
        if(oldBalanceRound <= 0) {
            bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {" +task.getUUID() + "} отклонена: Недостаточно средств. Баланс клиента " + oldBalance, task.getId()));
            return;
        }
        ExistCurrency clientCurrency = client.getCurrency();
        ExistCurrency taskCurrency = task.getCurrency();
        double taskAmount = task.getAmount();
        try {
            if ( clientCurrency != taskCurrency) { // Нужна конвертация
                ExchangeResult exchangeResult = CheckAmountWithRate(taskCurrency, clientCurrency, taskAmount);
                double newBalance = Math.round((oldBalance - exchangeResult.amount ) * 100.0) / 100.0 ;
                if ( newBalance < 0 ) { //Недостаточно средств
                    bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {" +task.getUUID() + "} отклонена: Недостаточно средств. Баланс клиента " + oldBalance + clientCurrency + " Сумма снятия " + taskAmount + taskCurrency + " сумма после конвертации "+ exchangeResult.amount + clientCurrency + " курс " + exchangeResult.pair + " "+  exchangeResult.rate,  task.getId()));
                } else {
                    client.changeBalance(newBalance);
                    bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {"  +task.getUUID() +"} выполнена: баланс клиента: до " + oldBalance + clientCurrency +  " после " + newBalance + clientCurrency + " обменный  курс " + exchangeResult.rate + " " + exchangeResult.pair, task.getId()));
                }
            }else { //Конвертация не нужна
                double newBalance =  Math.round((oldBalance - taskAmount ) * 100.0) / 100.0;
                if ( newBalance < 0 ) { //Недостаточно средств
                    bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {" +task.getUUID() + "} отклонена: Недостаточно средств. Баланс клиента " + oldBalance + clientCurrency + " Сумма снятия " + taskAmount + taskCurrency + " Конвертация не требуется",  task.getId()));
                } else{
                    client.changeBalance(newBalance);
                    bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {" + task.getUUID() +"} выполнена: баланс клиента: до " + oldBalance + clientCurrency + " после " + newBalance + clientCurrency + "Конвертация не проводилась", task.getId()));
                }
            }
        }catch (NoSuchElementException | IllegalArgumentException e ) {
            bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {"+task.getUUID() + "} отклонена. " + e.getMessage(), task.getId()));
        }
    }

    // Перевод
    // 1. Запрещаю перевод самому себе
    // 2. Запрос в банке информации о клиенте
    // 3. Проверяем достаточно ли средств на счете отправителя для перевода
    // 4. Проверка валюты на счете получателя и конвертация при необходимости
    private void transfer(Client client, Task task, Client receiver) {
    try {

        if (client.getID() == receiver.getID()) {
            bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {"+task.getUUID() +"} отклонена: Отправитель и получатель перевода совпадают ", task.getId()));
            return;
        }

        double balanceSender = client.getBalance();
        int balanceSenderRound = (int)balanceSender; //Проверяем только целую часть
        if( balanceSenderRound <= 0) {
            bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {"+task.getUUID() +"} отклонена: Недостаточно средств. Баланс клиента " + balanceSender, task.getId()));
            return;
        }
        ExistCurrency clientCurrency = client.getCurrency();
        ExistCurrency taskCurrency = task.getCurrency();
        double sumSend;
        double sumRecend;
        double taskAmount = task.getAmount();
        if ( clientCurrency != taskCurrency) { //Валюта перевода и на счету клиента отличаются - нужна конвертация
            ExchangeResult exchangeResult = CheckAmountWithRate(taskCurrency, clientCurrency, taskAmount);
            sumSend = exchangeResult.amount;
        } else { //Конвертация не нужна
            sumSend = taskAmount;
        }

        if ( sumSend > balanceSender ) { //На счету отправителя недостаточно средств
            bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {"+task.getUUID()+"}отклонена: На счету отправителя недостаточно средств .Сумма на счете " + balanceSender+ " Сумма перевода " + sumSend + "Расчет произведен в валюте " +taskCurrency,  task.getId()));
            return;
        }

        ExistCurrency receiverCurrency = receiver.getCurrency();
        if ( receiverCurrency != taskCurrency) { //Валюта перевода и на счету получателя отличаются - нужна конвертация
            ExchangeResult exchangeResult = CheckAmountWithRate(taskCurrency, receiverCurrency, taskAmount);
            sumRecend = exchangeResult.amount;
        } else {
            sumRecend =  taskAmount;
        }
        double balanceReceiverBefore = receiver.getBalance();
        double newBalanceSender = Math.round((balanceSender - sumSend ) * 100.0) / 100.0;
        client.changeBalance(newBalanceSender); //Снятие со счета отправителя
        double newBalanceReceiver = Math.round((balanceReceiverBefore + sumRecend) * 100.0) / 100.0;
        receiver.changeBalance(newBalanceReceiver); //Начисление на счет получателя
        bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {"+task.getUUID()+"} выполнена: баланс клиента " +client.getID() + " до :" + balanceSender + " сумма снятия " + sumSend + clientCurrency, task.getId()));
        bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {"+task.getUUID()+"}выполнена: баланс клиента" +receiver.getID()+  " до " + balanceReceiverBefore + " сумма начисления " + newBalanceReceiver + receiverCurrency , receiver.getID()));

    }catch (IllegalArgumentException e) {
        bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {"+task.getUUID()+"}отклонена. " + e.getMessage(), task.getId()));

    }
    }

 private void exchange( Task task) {
    if (task instanceof ExchangeTask exchangeTask) { //Если это точно задача на перевод
        ExistCurrency currencyFirst = exchangeTask.getCurrency();
        ExistCurrency currencySecond = exchangeTask.getSecondCurrency();
        double taskAmount = task.getAmount();
        if ( currencyFirst != currencySecond  ) {
            ExchangeResult exchangeResult = CheckAmountWithRate(currencySecond , currencyFirst, taskAmount);
            double summ = Math.round( (exchangeResult.amount) * 100.0) / 100.0;
            bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {"+task.getUUID()+"}выполнена. Расчитаная сумма " +summ+ " по курсу " +exchangeResult.pair+exchangeResult.rate, task.getId()));

        }else{
            bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {"+task.getUUID()+"} отклонена. Невозможно провести конвертацию. Валюты в задаче совпадают " +currencySecond , task.getId()));
        }
    }else {
        bank.getSenderNotification().addMessage(new PersonalNotification(this.nameCashier, "Транзакция {"+task.getUUID()+"}отклонена. Некорректная задача на перевод ", task.getId()));
    }

 }
 /* Метод ExchangeResult - выполнение конвертации
 Для возврата результата использую ExchangeResult
  */

    record ExchangeResult (double amount, CurrencyPair pair, double rate ) {} //Для возврата результатов конвертации
    private ExchangeResult CheckAmountWithRate(ExistCurrency clientCurrency, ExistCurrency taskCurrency, double amountTask  ) {
        if (clientCurrency == null || taskCurrency == null) {
            throw new IllegalArgumentException("Валюты операции не корректны " + clientCurrency + taskCurrency);
        }
            String namePair = String.valueOf(clientCurrency) + taskCurrency; //Формируем пары для запроса курса
            CurrencyPair pair = CurrencyPair.valueOf(namePair);
            Double rate = bank.getRate(pair); //Запрашиваем курс у банка - может вернуть null, если валюта не найдена
            if (rate == null) {
                throw new NoSuchElementException("Курс для валют " + pair + "не найден");
            }
                return new ExchangeResult(Math.round( (amountTask * rate) * 100.0 ) / 100.0, pair, rate );
        }
    }











