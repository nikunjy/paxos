import java.util.*;

public class Leader extends Process {
	ProcessId[] acceptors;
	ProcessId[] replicas;
	BallotNumber ballot_number;
	boolean active = false;
	PingAcceptor pingAcceptor;
	Map<Integer, Command> proposals = new HashMap<Integer, Command>();
	public Leader(Env env, ProcessId me, ProcessId[] acceptors,
			ProcessId[] replicas){
		this.env = env;
		this.me = me;
		ballot_number = new BallotNumber(0, me);
		this.acceptors = acceptors;
		this.replicas = replicas;
		env.addProc(me, this);
		pingAcceptor = new PingAcceptor(new ProcessId("pingacceptor:"+me),env);
	}

	public void body(){
		System.out.println("Here I am: " + me);

		new Scout(env, new ProcessId("scout:" + me + ":" + ballot_number),
				me, acceptors, ballot_number);
		for (;;) {
			PaxosMessage msg = getNextMessage();

			if (msg instanceof ProposeMessage) {
				ProposeMessage m = (ProposeMessage) msg;
				if (!proposals.containsKey(m.slot_number)) {
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
								new ProcessId("pingacceptor:"+m.ballot_number.leader_id),ballot_number,env);
						try {
							((Thread)pinger).join();
							System.out.println(this.me +"  success "+((Pinger)pinger).success);
							if (!((Pinger)pinger).success)
								break;
						} catch(Exception e) { 
							System.out.println("Exception pinging");
						}
					}while(true);
					System.out.println(this.me+" continue ");
					ballot_number = new BallotNumber(m.ballot_number.round + 1, me);
					new Scout(env, new ProcessId("scout:" + me + ":" + ballot_number),
							me, acceptors, ballot_number);
					active = false;
				}
			}

			else {
				System.err.println("Leader: unknown msg type");
			}
		}
	}
}
