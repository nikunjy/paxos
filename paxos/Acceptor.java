import java.io.PrintWriter;
import java.util.*;

public class Acceptor extends Process {
	BallotNumber ballot_number = null;
	Set<PValue> accepted = new HashSet<PValue>();
	PrintWriter writer;
	public Acceptor(Env env, ProcessId me){
		this.env = env;
		this.me = me;
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
	}

	public void body(){
		writer.println("Here I am: " + me);
		writer.flush();
		for (;;) {
			PaxosMessage msg = getNextMessage();

			if (msg instanceof P1aMessage) {
				P1aMessage m = (P1aMessage) msg;

				if (ballot_number == null ||
						ballot_number.compareTo(m.ballot_number) < 0) {
					ballot_number = m.ballot_number;
				}
				sendMessage(m.src, new P1bMessage(me, ballot_number, new HashSet<PValue>(accepted)));
			}
			else if (msg instanceof P2aMessage) {
				P2aMessage m = (P2aMessage) msg;
				if (m.ballot_number.round == Env.max_ballot) {
					boolean isFound = false;
					for (PValue val : accepted) { 
						if (val.slot_number == m.slot_number) { 
							isFound = true;
							break;
						}
					}
					if (!isFound) {
						accepted.add(new PValue(m.ballot_number, m.slot_number, m.command));
						sendMessage(m.src, new P2bMessage(me, m.ballot_number, m.slot_number));
					} else {
						sendMessage(m.src, new P2bMessage(me, ballot_number, m.slot_number));
					}
					return;
				}				
				if (ballot_number == null ||
						ballot_number.compareTo(m.ballot_number) <= 0) {
					ballot_number = m.ballot_number;
					accepted.add(new PValue(ballot_number, m.slot_number, m.command));
				}
				sendMessage(m.src, new P2bMessage(me, ballot_number, m.slot_number));
			}
		}
	}
}
