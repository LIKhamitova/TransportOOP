/* Абстрактный класс Task и от него наследуются более специализированные
От него реализованы классы наследники для конкретных банковских операций
DepositTask
WithdrawalTask
TransferTask
ExchangeTask
 */


import java.util.UUID;

abstract class Task {
    private final int id;
    private final double amount;
    private final ExistCurrency currency;
    private final UUID uniqueID; // номер обращения

 Task(  int id, double amount, ExistCurrency currency) {
        this.id = id;
        this.amount = amount;
        this.currency = currency;
        this.uniqueID = UUID.randomUUID();
    }

     public int getId (){
        return id;
    }
     public double getAmount(){
        return  amount;
     }
     public ExistCurrency getCurrency() {
        return currency;
     }

     public UUID getUUID(){
       return uniqueID;
    }

     abstract public BankOperation getOperation();
}


class DepositTask extends Task {
    private final BankOperation operation = BankOperation.DEPOSIT;

    DepositTask( int id, double amount, ExistCurrency currency) {
        super( id,  amount,  currency);
    }

    @Override
    public BankOperation getOperation(){
        return operation;
    }

    @Override
    public String toString(){
        return  "Задача: Банковская операция UUID {" + super.getUUID() + "} = " + this.operation + " заказчик= " + super.getId() + " сумма пополнения = "+ super.getAmount() + super.getCurrency();
    }
}

class WithdrawalTask extends Task{
    private final BankOperation operation = BankOperation.WITHDRAWAL;

    WithdrawalTask( int id, double amount, ExistCurrency currency) {
        super( id,  amount,  currency);
    }
    @Override
    public BankOperation getOperation(){
        return operation;
    }

    @Override
    public String toString(){
        return  "Задача: Банковская операция UUID {" + super.getUUID() + "} = "  + this.operation + " заказчик= " + + super.getId()  + " сумма снятия = " + super.getAmount() + super.getCurrency();
    }
}

class TransferTask extends Task {
    private final BankOperation operation = BankOperation.TRANSFER;
    private final int IdReceiver;

public TransferTask ( int id, double amount, ExistCurrency currency, int IdReceiver) {
    super( id,  amount,  currency);
    this.IdReceiver = IdReceiver;
}

public int getIdReceiver() {
    return IdReceiver;
}

    @Override
    public BankOperation getOperation(){
        return operation;
    }

    @Override
public String toString(){
        return  "Задача: Банковская операция UUID {" + super.getUUID() + "} = " + this.getOperation() + " заказчик= " + super.getId() + " получатель " + this.IdReceiver + " сумма = " + super.getAmount() + super.getCurrency();
    }
}

class ExchangeTask extends Task {
    private final ExistCurrency secondCurrency;
    private final BankOperation operation = BankOperation.EXCHANGE;

    public ExchangeTask( int id, double amount, ExistCurrency currency, ExistCurrency secondCurrency ) {
        super( id,  amount,  currency);
        this.secondCurrency = secondCurrency;
    }

    @Override
    public BankOperation getOperation(){
        return operation;
    }

    public ExistCurrency getSecondCurrency() {
        return secondCurrency;
    }

    @Override
    public String toString(){
        return  "Задача: Банковская операция UUID {" + super.getUUID() + "} = "  + this.getOperation() + " заказчик= " + super.getId() + " конвертация из валюты " + super.getCurrency() + " в " +  secondCurrency  + " сумма " + super.getAmount() + super.getCurrency();
    }
}



