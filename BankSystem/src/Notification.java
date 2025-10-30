import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/*Сервис рассылок и виды сообщений:
Реализованы 3 типа уведомлений:
1. Массовая рассылка - ALLNotification
2. Персональная рассылка - PersonalNotification
3. Сообщения об ошибках - SystemNotification
Реализуют базовый абстрактный класс Notification

Сообщения с типом ALLNotification и PersonalNotification имеют обработчики.
Сообщение с типом SystemNotification сразу сохраняется в лог

ОРГАНИЗАНИЯ РАССЫЛКИ:
!!!!Подразумивается, что сервис рассылок это отдельная сущность и она с банком не связана напрямую
@HandlerMessage - общий интерфейс для обработчиков сообщений.Его реализуют:
@HandlerNotificationAll - массовая рассылка.
@HandlerPersonalNotification - персонифицированная рассылка
@FactoryHandler - выбор обработчиков сообщений по их типу. Регистрируются только сообщения, подлежащие рассылке
@ServiceManagerReceiversNotify - сервис рассылок должен запрашивать у банка список отправителей.
Он взаимодействует с банком через interface IServiceManagerReceiversNotify
@MainSenderNotification-главный сервис рассылок. Хранит ссылку на ServiceManagerReceiversNotify и через него
взаимодействует с банком
Сообщения от банка помещаются в неблокирующую очередь (метод addMessage) и для каждого сообщения(!за исключением системных),
вызывается соответствующий обработчик( метод sender).
Также ведется запись всех сообщений в лог(logger.write)
 */

/* Базовый класс содержит общую информацию
@messageType - тип сообщений:  PERSONAL, ALL, SYSTEM, USER
@sender - отправитель:Банк или конкретная касса
@Createtimestamp - время создания
@message - текст сообщений
 */
abstract class  Notification {
    private final Message_type messageType;
    private final String sender;
    private final String Createtimestamp;
    private final String message;

    Notification( String sender, String message, Message_type  messageType ) {
        this.sender = sender;
        this.Createtimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSSSSSS"));
        this.message = message;
        this.messageType = messageType;
    }
    public String getTimestamp() {
        return this.Createtimestamp;
    }
    public String getSender() {
        return sender;
    }
    public String getMessage() {
        return message;
    }
    public Message_type  getMessageType(){return  messageType; }
}

/* Персональные сообщения(Message_type.PERSONAL):
Дополнительно
@id -  получателя
*/
class PersonalNotification extends Notification{
    private final int id;

    PersonalNotification( String sender, String message, int id){
        super(sender, message, Message_type.PERSONAL);
        this.id = id;
    }
    public int getIdReceiver() {
        return id;
    }
    @Override
    public String toString(){
        return super.getTimestamp() + " кому: " + id + " от " + super.getSender() + ": " + super.getMessage();
    }
}

/* Сообщение от пользователя (Message_type.USER)
доп полей нет - только логгирование
*/
class UserNotification extends Notification{  //Сообщение о постоуплении задачи в очередь
    UserNotification(String sender, String message ) {
        super(sender, message, Message_type.USER);
    }

    @Override
    public String toString(){
        return super.getTimestamp() + " сообщение от " + super.getSender() + ": " + super.getMessage();
    }
}

class ALLNotification extends Notification{  //Массовая рассылка - рассылает банк
    ALLNotification(String sender, String message ) {
        super(sender, message, Message_type.ALL);
    }

    @Override
    public String toString(){
        return super.getTimestamp() + " сообщение о поступлении задачи от " + super.getSender() + ": " + super.getMessage();
    }
}

/* Системные сообщения (Message_type.SYSTEM)
доп полей нет
*/
class SystemNotification extends Notification{
    SystemNotification(String sender, String message ) {
        super(sender, message, Message_type.SYSTEM);
    }

    @Override
    public String toString(){
        return super.getTimestamp() + " сообщение об ошибке " + super.getSender() + ": " + super.getMessage();
    }
}


/* HandlerMessage - общий интерфейс для всех обработчиков сообщений
Его реализуют HandlerNotificationAll и HandlerPersonalNotification
Первый осуществляет массовую рассылку
Второй - персонифицированную
Системные считаются особым типом - т.к. не имеют получателя
* */
interface HandlerMessage{
    void handled( Notification notification, IServiceManagerReceiversNotify serviceManagerReceiversNotify );
}

class HandlerNotificationAll implements HandlerMessage {
    @Override
    public void handled( Notification notification, IServiceManagerReceiversNotify serviceManagerReceiversNotify )
    {
        HashMap<Integer, Observer> observers = serviceManagerReceiversNotify.getALLReceivers(); //Запрос всех получателей и рассылка
        if (observers.isEmpty()) {
            return;
        }

        for(Observer observer: observers.values() ){
            observer.onBancEvents(notification);
        }
    }
}

class HandlerPersonalNotification implements HandlerMessage {
    @Override
    public void handled( Notification notification, IServiceManagerReceiversNotify serviceManagerReceiversNotify ){
        //Чтобы можно было отправить сообщение персонально, оно должно быть объектом класса PersonalNotification
        if (notification instanceof PersonalNotification personalNotification) {
            //Если да, выполняет downcasting
            int id = personalNotification.getIdReceiver();
            Observer observer = serviceManagerReceiversNotify.getCurrentReceiver(id);
            if (observer != null) { //Если пользователь с таким ID - клиент банка. Если клиент не найден, сообщение создается на стороне банка
                observer.onBancEvents(notification);
            }
        }
        }
    }


//Класс фабрика, для определения обработчика сообщения по его типу
class FactoryHandler {
    private final Map<Message_type , HandlerMessage> handlers = new HashMap<>();

    public  FactoryHandler(){
        //Регистрация обработчиков
        handlers.put(Message_type .PERSONAL, new HandlerPersonalNotification( ));
        handlers.put(Message_type.ALL, new HandlerNotificationAll( ));
    }
    public HandlerMessage getHandler(Message_type  messageType) { //По типу сообщения получаем обработчик
        return handlers.get(messageType);
    }
}

// Интерфейс-прослойка между банком и сервисом рассылок
interface IServiceManagerReceiversNotify{
    HashMap<Integer, Observer> getALLReceivers(); //Запрос для массовой рассылки
    Observer  getCurrentReceiver( int id);//Для персонифицированной рассылки
}

//Реализация интерфейса IServiceManagerReceiversNotify хранит ссылку на банк
class ServiceManagerReceiversNotify implements IServiceManagerReceiversNotify {
    private final Bank bank;

    ServiceManagerReceiversNotify(Bank bank){
        this.bank = bank;
    }
    @Override
    public HashMap<Integer, Observer> getALLReceivers() {
        return bank.getALLObservers();
    }

    @Override
    public Observer  getCurrentReceiver( int id) {
        return bank.getCurrentObserver(id);
    }
}

interface IMainSenderNotification{
    void addMessage(Notification notification);
    boolean sendNotification();
}
/*Главный сервис рассылок:
Хранит в себе общую неблокирующую очередь сообщений, которую последовательно обрабатывает в методе sendNotification
Который в свою очередь вызывает метод sender, где по типу сообщения вызывается его обработчик и вся переписка
заносится в лог(Класс Logger)
*/
class MainSenderNotification  implements IMainSenderNotification {
    private ConcurrentLinkedQueue<Notification> bufferNotification = new ConcurrentLinkedQueue<>();
    private final FactoryHandler factoryHandler = new FactoryHandler();
    private final IServiceManagerReceiversNotify managerReceiversNotify;

    public MainSenderNotification(IServiceManagerReceiversNotify serviceManagerReceiversNotify) {
        this.managerReceiversNotify = serviceManagerReceiversNotify;
    }

    @Override
    public void addMessage(Notification notification) {
        bufferNotification.offer(notification);
    }

    @Override
    public boolean sendNotification() {
        Notification message;
        while((message = bufferNotification.poll()) != null) {
            sender(message);
        }
        return true;
    }

    private void sender(Notification message) {
        HandlerMessage handlerMessage;
        LoggerSinglton logger = LoggerSinglton.getInctance();

        try {
            logger.write(message.toString()); //Все пишем в логер
        } catch (IOException e) {
            addMessage(new SystemNotification("Сервис логирования", "ОШИБКА:" + e.getMessage()));
        }
        if (message.getMessageType() != Message_type.SYSTEM &&  message.getMessageType() != Message_type.USER) { //Рассылка сообщений по получателям
            handlerMessage = factoryHandler.getHandler(message.getMessageType()); //Запрашиваем обработчик
            if (handlerMessage != null) {
                handlerMessage.handled(message, managerReceiversNotify);
            } else {
                addMessage(new SystemNotification("Сервис рассылки", "ОШИБКА: Для типа сообщения " + message.getMessageType() + " не найден обработчик.Отправка сообщения не прошла"));
            }
        }
    }
}
