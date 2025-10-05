import java.util.ArrayList;
import java.util.Scanner;
import java.util.InputMismatchException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        mainProgramme transport = new mainProgramme();
        transport.Execute();
        }
    }
 final class mainProgramme {
// ********************************
// Основное меню программы.
//********************************
    private final HandlerAction handleAction;

    public mainProgramme(){
        handleAction = new HandlerAction();
    }
     public void Execute() {
         Scanner scanner = new Scanner(System.in);

         while (true) {
             existActions.printMenuUser();
             System.out.println(" Для завершения работы программы введите - 99");
             System.out.print(">");
             try {
                 int input = scanner.nextInt();
                 if (input == 99) {
                     System.out.println("Программа завершает работу");
                     scanner.close();
                     break;
                 }
                 handleAction.handle(existActions.getNameEnum(input)); //Вызов главного класса-обработчика
             } catch (InputMismatchException e) {
                 System.out.println("ОШИБКА! Введено не числовое значение");
                 System.out.println("Повторите выбор команды в меню");
                 scanner.next();
             } catch (IllegalArgumentException e) {
                 System.out.println(e.getMessage());
                 System.out.println("Повторите выбор команды в меню");
             }
         }
     }
 }


final class HandlerAction {
    // ********************************
    // Основной класс-обработчик операций меню. Создает и вызывает обработчики для конкретных операций
    // Все классы-обработчики должны имплементировать интерфейс Action
    // ********************************
    private final CreateObject  creator;
    private final Information information;
    private final Fueling fueling;
    private final ControlTransport controlTransport;

    public HandlerAction(){
      this.creator = new CreateObject ();
      this.information = new Information();
      this.fueling = new Fueling();
      this.controlTransport = new ControlTransport();
    }

    public void handle(existActions action) {
        switch (action) {
            case existActions.CREATE -> this.creator.execute(); //Вызов обработчиков
            case existActions.INFO->this.information.execute();
            case existActions.FUELING -> this.fueling.execute();
            case existActions.CONTROL -> this.controlTransport.execute();
            default -> throw new IllegalArgumentException("ОШИБКА!Операция не поддерживается");
            }
        }
    }


interface Action {
//Интерфейс для классов-обработчиков
    void execute(  );
}


class CreateObject implements Action {
//Класс-обработчик для операции "Создание новых транспортных средств"
    @Override
    public void execute(  ){
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.println("СОЗДАНИЕ НОВОГО ТРАНСПОРТНОГО СРЕДСТВА");
            System.out.println("Выберите транспорт из списка");
            for (typeTransport transport : typeTransport.values()) {
                transport.printList();
            }
            System.out.print(">");
            try {
                int input = scanner.nextInt();
                typeTransport informationEnum = typeTransport.getNameEnum(input); //Данные из справочника, которые будут использоваться при создании
                System.out.println("Cоздается объект:" + informationEnum.getDescription() + "(" + informationEnum.getImage() + ")");
                System.out.println("Введите модель"); //Модель запрашиваем дополнительно с консоли
                System.out.print(">");
                String model = scanner.next();
                System.out.println("Введите мах объем бензобака(литры)"); //Модель запрашиваем дополнительно с консоли
                System.out.print(">");
                int maxFuel= scanner.nextInt();
                Transport newTransport;
                switch (informationEnum) {
                    case typeTransport.CAR:
                    { newTransport = new Car( informationEnum.getDescription(), informationEnum.getImage(), model, maxFuel);
                       break;}
                    case typeTransport.TRUCK:{
                        newTransport = new Truck(informationEnum.getDescription(), informationEnum.getImage(), model, maxFuel);
                        break;
                    } case typeTransport.BUS: {
                        newTransport = new Bus(informationEnum.getDescription(), informationEnum.getImage(), model, maxFuel);
                        break;
                    }
                    case typeTransport.TRACTOR: {
                        newTransport = new Tractor(informationEnum.getDescription(), informationEnum.getImage(), model, maxFuel);
                        break;
                    }
                    default:
                        System.out.println( "ОШИБКА! Не удалось создать транспортное средство");
                        continue;
                }
                Storage.mainStorage().addToStorage(newTransport); //Добавление объекта в хранилище
                System.out.println( "Создан новый транспорт");
                Printer.printer(newTransport);//Печать информации о новом транспорте
                System.out.println("Если хотите вернуться в основное меню, то введите 99. Если хотите создать еще транспорт - любое число ");
                System.out.print(">");
                int exit = scanner.nextInt();
                if (exit == 99) {
                    break;
                }
            } catch (InputMismatchException e) {
                System.out.println("ОШИБКА! Введено не числовое значение. Повторите ввод");
                scanner.next();
            } catch (ArithmeticException e) {
                System.out.println("ОШИБКА! Не удалось создать транспорт.Повторите ввод");
                scanner.next();
            } catch (IllegalArgumentException e ) {
                System.out.println(e.getMessage());
            }
        }
    }
}

class Information implements Action {
//Класс-обработчик просмотра информации о существующих объектах
    @Override
    public void execute() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("ПРОСМОТР ИНФОРМАЦИИ О СОЗДАННЫХ ТРАНСПОРТНЫХ СРЕДСТВАХ");
        ArrayList<Transport> list = Storage.mainStorage().getAllObjects();
        if (list.isEmpty()){
            System.out.println("Пусто");
        }
        else {
            System.out.println("Созданы следующие объекты");
            System.out.println("____________________________________________________________________________________________");
            System.out.printf("%-10s %-10s %-10s %-10s %-10s %-10s %-10s\n", "Номер", "Тип", "Состояние", "Топливо", "Объем", "КолТоплива", "Модель");
            System.out.println("____________________________________________________________________________________________");
            for (int i = 0; i < list.size(); i++) {
                TypeFuel type = list.get(i).getEngine().getTypeFuel();
                int maxFuelCount = list.get(i).getEngine().getMaxFuelCount();
                int currentFuelCount = list.get(i).getEngine().getCurrentFuelCount();
                System.out.printf("%-10s %-10s %-10s %-10s %-10s %-10s %-10s\n", i, list.get(i).getType(), list.get(i).getState(), type.getDescription(), maxFuelCount, currentFuelCount, list.get(i).getModel());
            }
            System.out.println("____________________________________________________________________________________________");
        }
        System.out.println("Для возврата в основное меню нажмите любое число");
        System.out.print(">");
        scanner.next();
    }
}

class Fueling implements Action {
//Класс-обработчик для заправки транспортных средств
    @Override
    public void execute() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("ЗАПРАВКА ТРАНСПОРТНЫХ СРЕДСТВ");
        while (true) {
            if (Storage.mainStorage().sizeStorage() == 0) { //Есть созданные объекты
                System.out.println("ОШИБКА! Транспортных средств не существует.Возврат в главное меню");
                break;
            }
            try {
                System.out.println("Введите номер транспортного средства, которое хотите заправить");
                System.out.print(">");
                int input = scanner.nextInt();
                Transport transport = Storage.mainStorage().getInfoByNumber(input); //Запрос объекта из хранилища по номеру
                if (transport == null) { //Нет такого объекта
                    System.out.println("Повторите ввод");
                    continue;
                }
                System.out.println("Выберите тип топлива:");
                TypeFuel.printList();
                System.out.print(">");
                int menu = scanner.nextInt();
                TypeFuel typeFuel = TypeFuel.getNameEnum(menu); //Выбор топлива
                System.out.println("Выберите количество топлива");
                System.out.print(">");
                int count = scanner.nextInt(); //Указание количества топлива
                Fuel fuel = new Fuel(count, typeFuel); //Создаем объект Топливо
                transport.getEngine().fueling(transport, fuel); //Заправляем транспорт
                System.out.println("Результат заправки");  //Выводим результаты
                Printer.printer(transport);
                System.out.println("Если хотите вернуться в основное меню, то введите 99. Если хотите заправить еще один транспорт - любое число ");
                System.out.print(">");
                int exit = scanner.nextInt();
                if (exit == 99) {
                    break;
                }
            } catch (InputMismatchException e) {
                System.out.println("ОШИБКА! Введено не числовое значение. Повторите ввод");
                scanner.next();
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            } catch (IndexOutOfBoundsException e) {
                System.out.println("ОШИБКА!Выбранное значение не входит в список доступных");
            }
        }
    }
}

class ControlTransport implements Action {
    //Класс-обработчик для управления транспортным средством
    @Override
    public void execute() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("ЗАПУСК УПРАВЛЕНИЯ ТРАНСПОРТНЫМ СРЕДСТВОМ");
        while (true) {
            if (Storage.mainStorage().sizeStorage() == 0) { //Проверка на существование транспорта
                System.out.println("Транспортных средств не существует.Возврат в главное меню");
                break;
            }
            try {
                System.out.println("Введите номер транспортного средства, которым хотите управлять");
                System.out.print(">");
                int input = scanner.nextInt();
                Transport transport = Storage.mainStorage().getInfoByNumber(input);//Запрос транспорта из хранилища
                System.out.println("Доступные операции с транспортным средством:");
                Movies.printMenu(); //Печать списка доступных операций с транспортным средством(меню)
                System.out.println("Выберите операцию:");
                System.out.print(">");
                int operation = scanner.nextInt(); //Выбор пользователя из меню
                Movies movies = Movies.getNameEnum(operation); //Получение имени константы
                switch (movies){ //Запуск операции с транспортным средством
                    case Movies.START: {
                        transport.start();
                        break;
                    }
                    case Movies.STOP: {
                        transport.stop();
                        break;
                    }
                    default: {
                        System.out.println("ОШИБКА! Не удалось выполнить действие");
                        continue;
                    }
                }
                System.out.println("Состояние транспортного средства, после изменения"); //Печать результата
                Printer.printer(transport);
                System.out.println("Если хотите вернуться в основное меню, то введите 99. Если хотите выбрать другое транспортное средство - любое число ");
                System.out.print(">");
                int exit = scanner.nextInt();
                if (exit == 99) {
                    break;
                }
            } catch (InputMismatchException e) {
                System.out.println("ОШИБКА! Введено не числовое значение. Повторите ввод");
                scanner.next();
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }catch (IndexOutOfBoundsException e) {
                System.out.println("ОШИБКА!Выбранное значение не входит в список доступных");
            }
        }
    }
}

final class Storage {
//Класс-хранилище для созданных объектов
    private static Storage mainStorage;
    private final ArrayList<Transport> arrayList = new ArrayList<>();

    public static Storage mainStorage() {
        if (mainStorage == null) {
            mainStorage = new Storage();
        }
        return mainStorage;
    }

    public void addToStorage(Transport transport) {
        arrayList.add(transport);
    }

    public int sizeStorage(){
        return arrayList.size();
    }

    public ArrayList<Transport> getAllObjects() {
        return arrayList;
    }

    public Transport getInfoByNumber(int number) {
        return arrayList.get(number);
    }
}

class Printer {
    //Вспомогательный класс для печати информации об объекте
  static void printer(Transport transport){
      TypeFuel fuel = transport.getEngine().getTypeFuel();
      System.out.println("-------------------------------------");
      System.out.printf("%-17s: %-10s%n", "Тип", transport.getType());
      System.out.printf("%-17s: %-10s%n", "Изображение", transport.getImage());
      System.out.printf("%-17s: %-10s%n", "Модель", transport.getModel());
      System.out.printf("%-17s: %-10s%n", "Состояние", transport.getState().getDescription());
      System.out.printf("%-17s: %-10s%n", "ТипТоплива", fuel.getDescription());
      System.out.printf("%-17s: %-10s%n", "МахОбъем(л)", transport.getEngine().getMaxFuelCount());
      System.out.printf("%-17s: %-10s%n", "Сейчас топлива в баке(л)", transport.getEngine().getCurrentFuelCount());
      System.out.println("-------------------------------------");
  }
}


abstract sealed class Transport permits Car, Truck, Bus, Tractor  {
    //Базовый класс для всех транспортных средств
    private final String type;
    private final String image;
    private final String model;
    private State state;
    private Engine engine;

    public void start () {
        //Запуск транспортного средства
        switch (this.state) {
            case State.BROKEN:
            {
                System.out.println("Транспортное средство сломано");
                break;
            }
            case State.STOP : {
                if ( engine.getCurrentFuelCount() > 0 ){
                    this.state = State.RUN;
                    System.out.println("Транспортное средство запущено");
                }
                else{
                    System.out.println("ОШИБКА!Нет топлива. Нужно заправить");
                }
                break;
            }
            case State.FUELED: {
                this.state = State.RUN;
                System.out.println("Транспортное средство запущено");
                break;
            }
            case State.RUN: {
                System.out.println("Транспортное средство уже запущено");
                break;
            }
            default: {
                System.out.println("Недопустимая операция");
                break;
            }
        }
    }
    public void stop() {
        //Остановка транспортного средства
        if (this.state == State.RUN ){
            this.state = State.STOP;
            System.out.println("Транспортное средство остановлено");
        }else {
            System.out.println("Транспортное средство не движется");
        }
    }

    public Transport ( String type,String image, String model ){
        this.type = type;
        this.image = image;
        this.model = model;
        this.state = State.STOP;
        this.engine = null;
    }

    public void setEngine(Engine engine){
        this.engine = engine;
    }

    public void setState(State state) {
        this.state = state;
    }
    public String getType(){ return this.type;}
    public String getModel() {return this.model;}
    public State getState(){ return this.state;}
    public String getImage(){ return this.image; }
    public Engine getEngine(){ return  this.engine; }
}

final class Car extends Transport {
    public Car( String type,String image, String model, int maxFuelCount){
        super( type, image, model);
        Engine newEngine = new PetrolEngine(maxFuelCount, 0);
        setEngine( newEngine);
    }
}

final class Truck extends Transport {
    public Truck(String type, String image, String model, int maxFuelCount) {
        super(type, image, model);
        Engine newEngine = new DieselEngine(maxFuelCount, 0);
        setEngine( newEngine);
    }
}

final class Bus extends Transport {
    public Bus( String type, String image, String model, int maxFuelCount){
        super( type, image, model);
        Engine newEngine = new PetrolEngine(maxFuelCount, 0);
        setEngine( newEngine);
    }
}

final class Tractor extends Transport {
    public Tractor( String type,String image, String model, int maxFuelCount){
        super( type, image, model);
        Engine newEngine = new DieselEngine(maxFuelCount, 0);
        setEngine( newEngine);
    }
}

abstract class Engine{
    //Двигатель - часть Transport
    private final int maxFuelCount;
    private int currentFuelCount;
    private TypeFuel typeFuel;

    public Engine(int maxFuelCount, int currentFuelCount){
        this.maxFuelCount = maxFuelCount;
        this.currentFuelCount = currentFuelCount;

    }
    public int getMaxFuelCount(){
        return this.maxFuelCount;
    }
    public int getCurrentFuelCount(){
        return this.currentFuelCount;
    }

    public  TypeFuel getTypeFuel(){
        return this.typeFuel;
    }

    public void setTypeFuel(TypeFuel typeFuel){
        this.typeFuel = typeFuel;
    }
    public void  fueling( Transport transport, Fuel fuel) {
        this.currentFuelCount += fuel.getCountFuel(); //Количество топлива
        if ( this.currentFuelCount > this.maxFuelCount ){
            System.out.println("Заправка только до полного бака");
            this.currentFuelCount = this.maxFuelCount;
        }
        if (fuel.getTypeFuel() != this.typeFuel ){
            System.out.println("ОШИБКА!Введенный тип топлива недопустим для данного транспорта");
            transport.setState(State.BROKEN);//Не тот тип топлива, транспорт сломан
        }else {
            System.out.println("Транспортное средство успешно заправлено");
            transport.setState(State.FUELED); //Транспорт заправлен
        }
    }
}

class PetrolEngine extends Engine {
    PetrolEngine(int maxFuelCount, int currentFuelCount ){
        super( maxFuelCount, currentFuelCount);
        setTypeFuel(TypeFuel.PETROL);
    }
}

class DieselEngine extends Engine {

    DieselEngine (int maxFuelCount, int currentFuelCount ){
        super( maxFuelCount, currentFuelCount);
        setTypeFuel(TypeFuel.DIESEL);
    }
}

class Fuel {
    //Топливо
    private final int count;
    private final TypeFuel typeFuel;

    Fuel(int count, TypeFuel typeFuel){
        this.count = count;
        this.typeFuel = typeFuel;
    }

    public int getCountFuel(){
        return  this.count;
    }
    public  TypeFuel getTypeFuel(){
        return this.typeFuel;
    }
}
 //  Справочники
enum existActions {
    CREATE(1, "Создать транспортное средство"),
    INFO(2, "Показать информацию о созданных транспортных средствах"),
    FUELING(3, "Заправить транспортное средство"),
    CONTROL(4, "Управлять транспортным средством");

    private final int action;
    private final String description;

   existActions(int action, String description){
        this.action = action;
        this.description = description;
    }

    public static void printMenuUser(){
        System.out.println("---------------------------------------");
        for(existActions value : values()){
            System.out.println(value.action + "-" + value.description);
        }
        System.out.println("---------------------------------------");
    }

    public static existActions getNameEnum (int input) {
        for (existActions value : values()) {
            if (value.action == input) {
                return  value;
            }
        }
        throw new IllegalArgumentException("ОШИБКА!Выбрана недопустимая операция");
    }
}

enum typeTransport{
     CAR( 1,"Автомобиль", "🚗" ),
     TRUCK(2, "Грузовик", "🚚"),
     BUS(3, "Автобус", "🚌" ),
     TRACTOR(4, "Трактор","🚜");

    private final int number;
    private final String description;
    private final String image;

  typeTransport(int number, String description,String image){
        this.number = number;
        this.description = description;
        this.image = image;
    }

   public String getDescription(){
        return description;
    }

    public String getImage(){
        return image;
    }

    public void printList() {
        System.out.println(this.number + "-" + this.description + "(" + this.image + ")");
    }
    public static typeTransport getNameEnum(int input) {
        for (typeTransport value : values()) {
            if (value.number == input) {
                return  value;
            }
        }
        throw new IllegalArgumentException("ОШИБКА!Тип транспорта отсутствует в списке");
    }
}

enum State {
    RUN("Транспортное средство едет"),
    STOP("Транспортное средство стоит"),
    BROKEN("Транспортное средство сломано"),
    FUELED("Транспортное средство заправлено");
private final String description;

State(String description){
    this.description = description;
}

public String getDescription(){
   return this.description;
}
}
enum Movies {
    START (1, "Запустить транспортное средство"),
    STOP(2, "Остановить транспортное средство");
    private final int number;
    private final String description;

    Movies(int number, String description){
        this.number = number;
        this.description = description;
    }

    public static void printMenu(){
        System.out.println("---------------------------------------");
        for(Movies value : values()){
            System.out.println(value.number + "-" + value.description);
        }
        System.out.println("---------------------------------------");
    }
    public static Movies getNameEnum(int input) {
        for (Movies value : values()) {
            if (value.number == input) {
                return  value;
            }
        }
        throw new IllegalArgumentException("Ошибка!Выбрана недопустимая операция");
    }
}


enum TypeFuel {
    PETROL(1, "Бензин"),
    DIESEL(2, "Дизель");

    private final int number;
    private final String description;

 TypeFuel(int number, String description){
    this.number = number;
    this.description = description;
    }

    public String getDescription() {
        return description ;
    }

    public static TypeFuel getNameEnum(int input) {
        for (TypeFuel value : values()) {
            if (value.number == input) {
                return  value;
            }
        }
        throw new IllegalArgumentException("ОШИБКА!Недопустимый тип топлива");
    }

    public static void printList(){
        for (TypeFuel value : values()) {
            System.out.println(value.number + "-" + value.description);
        }
    }
}




