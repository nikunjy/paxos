import java.io.PrintWriter;
import java.util.*;

public class Commander extends Process {
	ProcessId leader;
	ProcessId[] acceptors, replicas;
	BallotNumber ballot_number;
	int slot_number;
	Command command;
	PrintWriter writer;
	public Commander(Env env, ProcessId me, ProcessId leader, ProcessId[] acceptors,
			ProcessId[] replicas, BallotNumber ballot_number, int slot_number, Command command){
		this.env = env;
		this.me = me;
		this.acceptors = acceptors;
		this.replicas = replicas;
		this.leader = leader;
		this.ballot_number = ballot_number;
		this.slot_number = slot_number;
		this.command = command;
		env.addProc(me, this);
	}

	public void body(){

		P2aMessage m2 = new P2aMessage(me, ballot_number, slot_number, command);
		Set<ProcessId> waitfor = new HashSet<ProcessId>();
		for (ProcessId a: acceptors) {
			sendMessage(a, m2);
			waitfor.add(a);
		}
		System.out.println(this.me +" sent messages to acceptors ");
		while (2 * waitfor.size() >= acceptors.length) {
			PaxosMessage msg = getNextMessage();
			System.out.println(this.me+" "+msg.src);
			if (msg instanceof P2bMessage) {
				P2bMessage m = (P2bMessage) msg;

				if (ballot_number.equals(m.ballot_number)) {
					if (waitfor.contains(m.src)) {
						waitfor.remove(m.src);
					}
				}
				else {
					sendMessage(leader, new PreemptedMessage(me, m.ballot_number));
					return;
				}
			}
		}
		for (ProcessId r: replicas) {
			DecisionMessage msg = new DecisionMessage(me, slot_number, command);
			sendMessage(r, msg);
		}
		System.out.println(this.me +" completed");
	}
}
