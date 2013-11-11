import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;


public class BankOperation {
	List<Account> accounts;
	public enum OperationTypes {
		QUERY("query"),WITHDRAW("withdraw"),DEPOSIT("deposit"),ADDACCOUNT("addAccount");
		public String message;
		public String value() { 
			return message;
		}
		OperationTypes(String message) {
			this.message = message;
		}
	}
	public String op; 
	public String accountNumber; 
	public String holderName;
	public double amount;
	public static  BankOperation factory(String msg) { 
		Gson g = new Gson();
		return g.fromJson(msg,BankOperation.class);
	}
	public BankOperation() {
		
	}
	public String serialize() { 
		Gson g = new Gson(); 
		return g.toJson(this).toString();	
	}
	public void setAccounts(List<Account> accounts) {
		this.accounts = accounts;
	}
	
	public String operate() { 
		if (op.equals(BankOperation.OperationTypes.ADDACCOUNT.value())) { 
			Account act = new Account();
			act.holderName = holderName;
			act.balance = amount;
			accounts.add(act);
			return "Account created for "+holderName+" with balance "+amount;
		}
		Account act = null;
		for (Account ac : accounts) { 
			if (ac.holderName.equalsIgnoreCase(holderName) || ac.accountNumber.equalsIgnoreCase(accountNumber)) { 
				act = ac;
				break;
			}
		}
		if (act == null) { 
			return "No acount can be found";
		}
		if (op.equals(BankOperation.OperationTypes.QUERY.value())) {
			return queryAccount(act);
		} else if (op.equals(BankOperation.OperationTypes.WITHDRAW.value())) {
			return operateWithdraw(act,amount);
		} else {
			return operateDeposit(act,amount);
		}
		
	}
	public String queryAccount(Account account) {
		return "Account balance for client "+account.holderName +" is "+account.balance;
	}
	public String operateDeposit(Account account, double amount) { 
		account.balance -= amount;
		return "Account credited with "+amount+" for person "+account.holderName+" "+". New Balance "+account.balance;
	}
	public String operateWithdraw(Account account, double amount) { 
		account.balance -= amount;
		return "Account  debited with "+amount+" for person "+account.holderName+" "+". New Balance "+account.balance;
	}
}
