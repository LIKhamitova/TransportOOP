import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/*Класс ExchangeRateService для обновления курсов валют. На основе паттерна SingleTon
Курс валют управляется отдельным потоком. Это отдельная сущность. Она прекращает работу после завершения работы банка
   Краткое описание работы:
   1. Таблица currentExchangeRat инициализируется дефолтными значениями в конструкторе.
   2. В методе generateNewExchangeRate выполняется обновление курсов валют:
         Расчет от базовой валюты - USD
        3.1. Генерация дельты для пар USDRUB и USDEUR c округлением до двух знаков
        3.2. Расчет кросс-курса EURRUB
        3.3Расчет обратных курсв RUBUSD, USDEUR
        Этот метод запускает планировщик из своего потока - обновление каждые 10 сек
    3. Метод getExcangeRate вызывает со своей стороны банк каждые 15 сек
*/

 class ExchangeRateService implements Runnable {
     private static ExchangeRateService instance;
     private final ConcurrentHashMap<CurrencyPair, Double> currentExchangeRate = new ConcurrentHashMap<>();
     private final Random random = new Random();

     private ExchangeRateService() {
         currentExchangeRate.put(CurrencyPair.USDRUB, 81.42);
         currentExchangeRate.put(CurrencyPair.RUBUSD, 0.012);
         currentExchangeRate.put(CurrencyPair.USDEUR, 0.86);
         currentExchangeRate.put(CurrencyPair.EURUSD, 1.16);
         currentExchangeRate.put(CurrencyPair.EURRUB, 94.58);
         currentExchangeRate.put(CurrencyPair.RUBEUR, 0.011);
     }

     //Потокобезопасная ленивая инициализация чтобы гарантировано был создан только один экземпляр
     public static ExchangeRateService getInstance() {
         if (instance == null) {
             synchronized (ExchangeRateService.class) {
                 if (instance == null) {
                     instance = new ExchangeRateService();
                 }
             }
         } return instance;
 }

 @Override
 public void run() {
     generateNewExchangeRate();
 }

    public ConcurrentHashMap<CurrencyPair, Double> getExcangeRate() { //Запрос обновленного курса из банка
        return currentExchangeRate;
    }


    private synchronized void generateNewExchangeRate() {
         //Пара USDRUB
        double  amountUSDRUB = currentExchangeRate.get(CurrencyPair.USDRUB) + random.nextDouble(-1.00, 1.00);
                        if (amountUSDRUB <= 0.00) {
                            amountUSDRUB = 1.00;
                        } else {
                            amountUSDRUB = Math.round( amountUSDRUB * 100.0 ) / 100.0;
                        }
                        currentExchangeRate.put(CurrencyPair.USDRUB, amountUSDRUB);
        //Пара USDEUR
        double  amountUSDEUR = currentExchangeRate.get(CurrencyPair.USDEUR) + random.nextDouble(-1.00, 1.00);
                        if (amountUSDEUR <= 0.00) {
                            amountUSDEUR = 1.00;
                        }else {
                            amountUSDEUR = Math.round( amountUSDEUR * 100.0 ) / 100.0;
                        }
                        currentExchangeRate.put(CurrencyPair.USDEUR, amountUSDEUR);

        //Пара EURRUB
        double  amountEURRUB = Math.round((amountUSDRUB / amountUSDEUR) * 100.0) / 100.0;
                        if (amountEURRUB > 0) {
                            currentExchangeRate.put(CurrencyPair.EURRUB, (amountEURRUB));
                        } else {
                            currentExchangeRate.put(CurrencyPair.EURRUB, (1.00));
                        }

        //Пара RUBUSD
        double amountRUBUSD = Math.round( (1 / amountUSDRUB) * 100)  / 100.0;
                        if( amountRUBUSD > 0 ) {
                            currentExchangeRate.put(CurrencyPair.RUBUSD, amountRUBUSD);
                        }else {
                            currentExchangeRate.put(CurrencyPair.RUBUSD, 1.00);
                        }

        // Пара EURUSD
        double amountEURUSD = Math.round( (1 / amountUSDEUR) * 100)  / 100.0;
                        if (amountEURUSD > 0 ){
                            currentExchangeRate.put(CurrencyPair.EURUSD, amountEURUSD);
                        } else {
                            currentExchangeRate.put(CurrencyPair.EURUSD, 1.00);
                        }

        //Пара RUBEUR
        double amountRUBEUR = Math.round( (1 / amountEURRUB) * 100)  / 100.0;
                        if (amountRUBEUR > 0) {
                            currentExchangeRate.put(CurrencyPair.RUBEUR, amountRUBEUR);
                        } else {
                            currentExchangeRate.put(CurrencyPair.RUBEUR, 1.0);
                        }

                }
            }

