import java.util.UUID;


public class Account {
	public String accountNumber; 
	public String holderName;
	public double balance;
	public Account() { 
		UUID id = UUID.randomUUID(); 
		accountNumber = id.toString();
	}
}
