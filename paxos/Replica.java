import java.io.PrintWriter;
import java.util.*;

public class Replica extends Process {
	ProcessId[] leaders;
	List<Account> accounts;
	int slot_num = 1;
	Map<Integer /* slot number */, Command> proposals = new HashMap<Integer, Command>();
	Map<Integer /* slot number */, Command> decisions = new HashMap<Integer, Command>();
	Map<Command, Command> readOps = new HashMap<Command,Command>();
	PrintWriter writer;
	public Replica(Env env, ProcessId me, ProcessId[] leaders){
		this.env = env;
		this.me = me;
		this.leaders = leaders;
		try {
			String name = "";
			String [] names = this.me.toString().split(":");
			for (int i = 0; i < names.length; i++) { 
				name += names[i];
			}
			writer = new PrintWriter(name+".txt", "UTF-8");
		} catch (Exception e) { 
			System.out.println(e);
		}
		env.addProc(me, this);
		accounts = new ArrayList<Account>();

	}

	void propose(Command c){
		if (decisions.containsValue(c)) { 
			System.out.println("Contained in decisions "+c);
		}
		if (c.isReadOnly()) { 
			//No need to propose this.
			BankOperation read = BankOperation.factory((String)c.op);
			int readSlot = -1;
			for (Integer slot : proposals.keySet()) { 
				BankOperation write = BankOperation.factory((String)proposals.get(slot).op);
				if (write.holderName.equalsIgnoreCase(read.holderName)) { 
					if (slot > readSlot) { 
						readSlot = slot;
					}
				}
			}
			if (readSlot !=-1) {
				Command writeCommand = proposals.get(readSlot);
				boolean found = false;
				for (Integer slot : decisions.keySet()) { 
					if (decisions.containsValue(writeCommand)) { 
						if (slot < slot_num) { 
							BankOperation op = BankOperation.factory(c.op.toString());
							op.setAccounts(accounts);
							writer.println("" + me + ":perform "+op.operate());
							writer.flush();
							found = true;
							break;
						}
					}
				}
				if (!found) {
					System.out.println(c+ " maped to "+writeCommand);
					readOps.put(c, writeCommand);
				}
			} else { 
				System.out.println("No write found to map to");
			}
			return;
		}
		if (!decisions.containsValue(c)) {
			for (int s = 1;; s++) {
				if (!proposals.containsKey(s) && !decisions.containsKey(s)) {
					proposals.put(s, c);
					for (ProcessId ldr: leaders) {
						sendMessage(ldr, new ProposeMessage(me, s, c));
					}
					break;
				}
			}
		}
	}

	void perform(Command c){
		for (int s = 1; s < slot_num; s++) {
			if (c.equals(decisions.get(s))) {
				slot_num++;
				return;
			}
		}
		BankOperation op = BankOperation.factory(c.op.toString());
		op.setAccounts(accounts);
		writer.println("" + me + ":perform "+op.operate());
		writer.flush();
		slot_num++;
		for (Command cmd : readOps.keySet()) { 
			Command mappedWrite = readOps.get(cmd);
			if (mappedWrite.equals(c)) { 
				op = BankOperation.factory(cmd.op.toString());
				op.setAccounts(accounts);
				writer.println("" + me + ":perform "+op.operate());
				writer.flush();
				readOps.remove(c);
			} 
		}

	}

	public void body(){
		writer.println("Here I am: " + me);
		for (;;) {
			PaxosMessage msg = getNextMessage();
			if (msg instanceof RequestMessage) {
				RequestMessage m = (RequestMessage) msg;
				propose(m.command);
			} else if (msg instanceof DecisionMessage) {
				DecisionMessage m = (DecisionMessage) msg;
				System.out.println(this.me+" "+m.slot_number+" "+m.command);
				decisions.put(m.slot_number, m.command);
				for (;;) {
					Command c = decisions.get(slot_num);
					if (c == null) {
						break;
					}
					Command c2 = proposals.get(slot_num);
					if (c2 != null && !c2.equals(c)) {
						propose(c2);
					}
					perform(c);
				}
				//##end of pending reads
			}
			else {
				writer.println("Replica: unknown msg type");
			}
		}
	}
}
