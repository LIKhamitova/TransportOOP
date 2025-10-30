
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
/* Класс Банк
Возможные состояния банка:
bankIsOpen = true: Открытие банка
          потоки-клиенты начинают добавлять задачи
          создаются потоки кассы и начинают работать
          начинает работать сервис рассылки

StopSend = true.  Банк прекращает прием новых задач. Кассы и сервис рассылки еще работают
bankIsOpen = false: Банк полностью прекращает работу. Потоки закрываются

 */
class Bank {
    private final HashMap<Integer, Observer> observers = new HashMap<>();
    private final ConcurrentHashMap<Integer,Client> bankclients = new ConcurrentHashMap<>(); //Список банковских клиентов
    private IMainSenderNotification senderNotification; //Связка с сервисом рассылок
    private AtomicBoolean bankIsOpen = new AtomicBoolean(false);
    private AtomicBoolean StopSend = new AtomicBoolean(false);
    private ConcurrentLinkedQueue<Task> queue = new ConcurrentLinkedQueue<>(); //Очередь задач
    private ConcurrentHashMap<CurrencyPair, Double> ExchangeRateBank = new ConcurrentHashMap<>(); //Курсы валют
    private final String bankName = "Банк";
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(); //Блокировка когда обновляется курс валют
    private final List<Cashier> cashiers = new ArrayList<>();
    private ExecutorService cashierThreads;
    private final Random random = new Random();
    private final ExchangeRateService exchangeRateService; //Биржа для обновления курса валют
    private final Object lockForOpen  = new Object(); //Объект блокировки на котором клиенты ожидают открытия банка
    private  CountDownLatch finalCashier; //Счетчик для завершения работы кассиров


    Bank() {
        exchangeRateService = ExchangeRateService.getInstance();
    }
    //Потоки-клиенты пытаются взять эту блокировку и ждут на ней пока банк не откроется
    public Object getLockForOpen() {
        return lockForOpen;
    }

    // Открытие банка
    /*
    1. Создание кассиров. CountDownLatch использую для будущего информирования о завершении работы
    2. Запуск сервиса рассылки уведомлений
    3. Бужу потоки клиенты и они начинают работать
    4. Рассылка уведомлений о старте работы
    */

    public void BankOpen() {

        finalCashier = new CountDownLatch(3); //Запуск касс
        for( int i = 0; i < 3; i++) {
            cashiers.add( new Cashier("Касса №" + i, this, finalCashier));
        }
        cashierThreads = Executors.newFixedThreadPool(cashiers.size());
        for(Cashier cashier : cashiers) {
            cashierThreads.submit(cashier);
        }

        System.out.println("БАНК ОТКРЫТ");
       synchronized (lockForOpen) {  //Бужу клиентов
           bankIsOpen.set(true);
           senderNotification.addMessage(new ALLNotification(this.bankName, "Банк начал работу"));
           lockForOpen.notifyAll();
       }
    }

    /*Закрытие банка
    1 Банк прекращает прием новых задач
    2.Ожидание завершения работы касс. Ожидание когда все кассы скинут счетчик
    3.Закрытие потока касс
    4.Стоп прием уведомлений
    */

    public boolean BankClosed() throws InterruptedException {
        System.out.println("НАЧАЛОСЬ ЗАКРЫТИЕ БАНКА");
        senderNotification.addMessage(new ALLNotification(this.bankName, "Банк прекращает прием новых задач"));
        bankIsOpen.set(false); //Стоп прием новых задач
        System.out.println("Ожидание завершения работы касс");
        System.out.println(finalCashier.toString());
        finalCashier.await(); //Ожидание счетчика
        System.out.println(finalCashier.toString());
        cashierThreads.shutdown(); //Закрытие потоков-кассиров
        System.out.println("Кассы закончили");
        senderNotification.addMessage(new ALLNotification(this.bankName, "Рассылка уведомлений банком завершена"));
        StopSend.set(true); //Прекращение работы сервиса рассылок
        try {
            if(! cashierThreads.awaitTermination(5, TimeUnit.SECONDS)) { //Принудительное закрытие по Timeout
                cashierThreads.shutdownNow(); //Таймаут истек - кассиры не доработали
            }
        }catch (InterruptedException e) {
            System.out.println("Ожидание завершения потока было прервано.Принудительно завершаю");
            cashierThreads.shutdownNow();
        }
        return true;

    }
    public AtomicBoolean isOpen() {
        return bankIsOpen;
    }
    /* Работа сервиса рассылок.
    Сервис рассылки также работает на необлокирующей очереди.
    После StopSend станет thue, дорабатывает заявки в очереди и завершается
     */
    public boolean bankWorking() {
        Boolean finish = false;
        while (!StopSend.get()) {
            finish =  senderNotification.sendNotification();
        }
        finish = false;
        while (!finish) {
            finish  =  senderNotification.sendNotification();
        }
           try{
               Thread.sleep(3000);
               System.out.println("Рассылка уведомлений завершена");
           } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               System.out.println("Поток ожидания завершения рассылок был прерван");
           }

           return true;
    }

    public Client getClientID(int id) {
        Client client = bankclients.get(id);
        if (client == null) {
            throw  new IllegalArgumentException("Клиент с ID " + id + " в данных банка не найден" );
        }
        return client;
  }
    //Обновление курса валют. Работает пока не остановлена рассылка
    public Double getRate(CurrencyPair pair) { //Обновление курсов валют
        while ( !StopSend.get()) {
            lock.readLock().lock(); //Разрешаю всем узнавать курс, если планировщик не обновляет курс
            try {
                return ExchangeRateBank.get(pair);
            } finally {
                lock.readLock().unlock();
            }
        } return  null;
    }
    //Обновление курса валют
    public void updateRate() {
        if (!StopSend.get()) {
            lock.writeLock().lock();
            try {
                ExchangeRateBank = exchangeRateService.getExcangeRate();
            } finally {
                lock.writeLock().unlock();
                senderNotification.addMessage(new ALLNotification(this.bankName, "Курс валют обновлен: " + ExchangeRateBank.toString()));
            }
        }
    }

    /* Управление сервисом рассылок:
    Сервис может запрашивать:
    @getALLObservers - всех получателей, getCurrentObserver - по ID
    *@addObserver - подписка клиентов на уведомления от банка
    *@setSendler - ссылка на сервис рассылок
    *@getSenderNotification - запрашивают кассы для работы с сообщениями
    * */
    public void setSendler(IMainSenderNotification senderNotification) { //Сервис для работы с рассылкой - банк этим не занимается
        this.senderNotification = senderNotification;
    }
    public IMainSenderNotification getSenderNotification() {
        return this.senderNotification;
    }
    public HashMap<Integer, Observer> getALLObservers() { //Запрос для массовой рассылки
        if (!observers.isEmpty()) {
            return observers;
        } else {
            senderNotification.addMessage(new SystemNotification(this.bankName, " клиенты не могут быть предоставлены "));
            return null;
        }
    }
    public Observer getCurrentObserver(int id) { //Запрос конкретного получателя
        Observer currentObserver =  observers.get(id);
        if (currentObserver != null) {
            return currentObserver ;
        } else {
            senderNotification.addMessage(new SystemNotification(this.bankName, " по запрошенному ID " + id + "данные в банке не найдены"));
            return  null;
        }
    }
    public void addObserver(Observer observer) { //Список подписавшихся на уведомления от банка
        observers.put(observer.getID(), observer);
    }

    public void addClients(Client client) { //Добавление клиентов
        bankclients.put(client.getID(), client);
    }
    //Клиенты помещают задачи в очередь - пока банк открыт
    public void putInQueue(Task task) {
        if(bankIsOpen.get()) {//Задачи клиентов-запрещаю добавление после закрытия
            queue.offer(task);
            senderNotification.addMessage(new UserNotification(this.bankName,   task.toString() + " добавлена в очередь"));
        } else {
            senderNotification.addMessage(new PersonalNotification(this.bankName, "Задача :" + task.toString() + " ОТКЛОНЕНА. Банк закрыт", task.getId()));
        }
        }

    public Task takeTask() { //Кассы берут задачи из очереди
        return queue.poll();
    }

    /* Случайный выбор клиента для перевода
    Перекидываю значения в динамический массив и беру по случайному индексу
     */
    public int getRandomClientsID() {
        List<Map.Entry<Integer,Client >> entryList = new ArrayList<>(bankclients.entrySet());
        int chooseIndex =  random.nextInt( bankclients.size());
        Map.Entry<Integer,Client > randomClient = entryList.get(chooseIndex);
        return randomClient.getValue().getID();
    }
}




