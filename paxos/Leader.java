import java.util.*;

public class Leader extends Process {
	ProcessId[] acceptors;
	ProcessId[] replicas;
	BallotNumber ballot_number;
	boolean active = false;
	Map<Integer, Command> proposals = new HashMap<Integer, Command>();
	Integer max=0;
	public Leader(Env env, ProcessId me, ProcessId[] acceptors,
			ProcessId[] replicas){
		this.env = env;
		this.me = me;
		ballot_number = new BallotNumber(0, me);
		this.acceptors = acceptors;
		this.replicas = replicas;
		env.addProc(me, this);
	}
	
	public void body(){
		System.out.println("Here I am: " + me);

		new Scout(env, new ProcessId("scout:" + me + ":" + ballot_number),
				me, acceptors, ballot_number);
		for (;;) {
			PaxosMessage msg = getNextMessage();
			if (msg instanceof ProposeMessage) {
				ProposeMessage m = (ProposeMessage) msg;
				if (!proposals.containsKey(m.slot_number)) { // if it hasn't already proposed something for that slot
					if (m.command.isReadOnly()) {
						proposals.put(m.slot_number,m.command);
						BallotNumber bn  = new BallotNumber(Env.max_ballot,this.me);
						new Commander(env,
								new ProcessId("commander:" + me + ":" + bn + ":" + m.slot_number),
								me, acceptors, replicas, bn, m.slot_number, m.command);
						continue;
					}
					proposals.put(m.slot_number, m.command);
					if (active) {
						new Commander(env,
								new ProcessId("commander:" + me + ":" + ballot_number + ":" + m.slot_number),
								me, acceptors, replicas, ballot_number, m.slot_number, m.command);
					}
				}
			}else if (msg instanceof AdoptedMessage) {
				AdoptedMessage m = (AdoptedMessage) msg;
				if (ballot_number.equals(m.ballot_number)) {
					int maxBCount = 0;
					for (PValue pv : m.accepted) {
						if (pv.ballot_number.round == Env.max_ballot) { 
							maxBCount++;
						}
					}
					if (maxBCount < acceptors.length/2) { 
						Iterator<PValue> it = m.accepted.iterator();
						while (it.hasNext()) { 
							if(it.next().ballot_number.round == Env.max_ballot) { 
								it.remove();
							}
						}
					}
					Map<Integer, BallotNumber> max = new HashMap<Integer, BallotNumber>();
					for (PValue pv : m.accepted) {
						BallotNumber bn = max.get(pv.slot_number);
						if (bn == null || bn.compareTo(pv.ballot_number) < 0) {
							max.put(pv.slot_number, pv.ballot_number);
							proposals.put(pv.slot_number, pv.command);
						}
					}

					for (int sn : proposals.keySet()) {
						new Commander(env,
								new ProcessId("commander:" + me + ":" + ballot_number + ":" + sn),
								me, acceptors, replicas, ballot_number, sn, proposals.get(sn));
					}
					active = true;
				}
			}

			else if (msg instanceof PreemptedMessage) {
				PreemptedMessage m = (PreemptedMessage) msg;
				if (ballot_number.compareTo(m.ballot_number) < 0) {
					System.out.println(this.me+" preempted ");
					do {
						Process pinger = new Pinger(new ProcessId("pinger:"+this.me+":"+ballot_number.round),
								m.ballot_number.leader_id,ballot_number,env);
						try {
							((Thread)pinger).join();
							if (!((Pinger)pinger).success)
								break;
						} catch(Exception e) { 
							System.out.println("Exception pinging");
						}
					}while(true);
					System.out.println(this.me+" Continue Preempting ");
					ballot_number = new BallotNumber(m.ballot_number.round + 1, me);
					new Scout(env, new ProcessId("scout:" + me + ":" + ballot_number),
							me, acceptors, ballot_number);
					active = false;
				}
			} else if ((msg instanceof PingRequestMessage)) {
				PingReplyMessage reply = new PingReplyMessage(this.me,new Command(this.me,0,"Ping Reply"));
				sendMessage(((PingRequestMessage)msg).src,reply);
			}
			else {
				System.err.println("Leader: unknown msg type");
			}
		}
	}
}
