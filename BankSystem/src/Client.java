import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
/*Класс клиент
Хранит ссылку на банк, но не в конструкторе. Тип отношений - агрегация. Теоретически возможна смена клиентом банка
После создания ждет открытия банка
После открытия генерирует задачи случайным образом
* */
class Client implements Runnable, Observer {
    private final int id;
    private final ExistCurrency currency;
    private Bank bank;
    private double balance;
    private final ReentrantLock LockBalance = new ReentrantLock();
    private final Random random = new Random();
    double minAmount = 0.00;
    double maxAmount = 10000.00;

    public Client(int id, double balance, ExistCurrency currency) {
        this.id = id;
        this.balance = balance;
        this.currency = currency;
    }
    //Получение ссылки на банк
    public void setBank(Bank bank) {
        this.bank = bank;
    }

    public int getID() {
        return id;
    }
    //Предоставление блокировки на операции с балансом
    public ReentrantLock getLock() {
        return LockBalance;
    }

    public double getBalance() {
        LockBalance.lock();
        try{
            return balance;
        } finally {
            LockBalance.unlock();
        }

    }
    public void changeBalance(double newBalance) {
        LockBalance.lock();
        try{
            this.balance = newBalance;
        } finally {
            LockBalance.unlock();
        }
    }

    public ExistCurrency getCurrency() {
        return currency;
    }

    public void run() {
        synchronized (bank.getLockForOpen()) {
            while (!bank.isOpen().get()) {
                try{
                    System.out.println("Клиент " + id + " ожидает открытия банка...");
                    bank.getLockForOpen().wait();
                }catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Клиент " + id + " ожидающий открытия банка был прерван");
                    return;
                }
            }
        }

        System.out.println("Клиент " + id + " начал работу....." );
        while (bank.isOpen().get()) {
          try {
              generateTask();
          }  catch ( InterruptedException e) {
              Thread.currentThread().interrupt();
              System.out.println("Клиент " + id + " был прерван");
              return;
          }
        }
        }

    private void generateTask() throws InterruptedException {
        int randomID = random.nextInt(6) + 1;
        double randomAmount = Math.round((random.nextDouble(minAmount, maxAmount)) * 100.0 ) / 100.0 ;
        ExistCurrency randomCurrency = ExistCurrency.values()[random.nextInt(2)];
        switch ( randomID ) {
            case 1: {
                Task deposit = new DepositTask( id, randomAmount , randomCurrency );
                System.out.println("Клиент " + id + " создал задачу: " + deposit.toString());
                bank.putInQueue(deposit);
                Thread.sleep(1000);
                break;
            }
            case 2: {
                Task withdrawal = new WithdrawalTask( id, randomAmount , randomCurrency );
                System.out.println("Клиент " + id + " создал задачу: " + withdrawal .toString());
                bank.putInQueue(withdrawal);
                Thread.sleep(2000);
                break;
            }
            case 3 : {
                int idRandomClient = bank.getRandomClientsID();
                Client randomClient = bank.getClientID(idRandomClient);
                Task transfer = new TransferTask( id, randomAmount , randomCurrency, idRandomClient );
                System.out.println("Клиент " + id + " создал задачу: " + transfer .toString());
                bank.putInQueue(transfer);
                Thread.sleep(1000);
                break;
            }
            case 4: {
                ExistCurrency randomCurrencySecond = ExistCurrency.values()[random.nextInt(2)];
                Task exchange = new ExchangeTask( id, randomAmount , randomCurrency, randomCurrencySecond );
                System.out.println("Клиент " + id + " создал задачу: " + exchange.toString());
                bank.putInQueue(exchange);
                Thread.sleep(3000);
                break;
            }
            case 5: {
                System.out.println("Клиент " + id + " проверяет свой баланс: " + this.getBalance() + this.getCurrency() );
                Thread.sleep(1000);
                break;
            } case 6: {
                System.out.println("Клиент " + id + " отдыхает.....");
                Thread.sleep(2000);
                break;
            } default: {
                Thread.sleep(1000);
                break;
            }
        }
    }

    public void onBancEvents(Notification message) {
        System.out.println("Клиент " + id + " получил уведомление: " + message.toString());
    }
}