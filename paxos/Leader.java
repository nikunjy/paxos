import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Leader extends Process {
	ProcessId[] acceptors;
	ProcessId[] replicas;
	BallotNumber ballot_number;
	boolean active = false;
	Map<Integer, Command> proposals = new HashMap<Integer, Command>();
	PrintWriter writer;
	public Leader(Env env, ProcessId me, ProcessId[] acceptors,
			ProcessId[] replicas){
		this.env = env;
		this.me = me;
		ballot_number = new BallotNumber(0, me);
		this.acceptors = acceptors;
		this.replicas = replicas;
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
		new Scout(env, new ProcessId("scout:" + me + ":" + ballot_number),
				me, acceptors, ballot_number);
		for (;;) {
			writer.flush();
			PaxosMessage msg = getNextMessage();
			if (msg instanceof ProposeMessage) {
				//System.out.println(this.me+" "+((ProposeMessage)msg).command);
				ProposeMessage m = (ProposeMessage) msg;
				if (!proposals.containsKey(m.slot_number)) { // if it hasn't already proposed something for that slot
					System.out.println(this.me +" "+m.command);
					if (m.command.isReadOnly()) {
						System.out.println("read only command using max");
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
					Map<Integer,Integer> maxBCount = new HashMap<Integer,Integer>();
					for (PValue pv : m.accepted) {
						if (pv.ballot_number.round == Env.max_ballot) {
							if (!maxBCount.containsKey(pv.slot_number)) { 
								maxBCount.put(pv.slot_number, 1);
							} else { 
								maxBCount.put(pv.slot_number, maxBCount.get(pv.slot_number)+1);
							}
						}
					}
					for(Integer slot : maxBCount.keySet()) {
						if (maxBCount.get(slot) >= acceptors.length/2)
							continue;
						Iterator<PValue> it = m.accepted.iterator();
						while (it.hasNext()) { 
							if(it.next().slot_number == slot && it.next().ballot_number.round == Env.max_ballot) { 
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
				System.out.println(this.me+" preempted");
				if (ballot_number.compareTo(m.ballot_number) < 0) {
					writer.println(this.me+" preempted ");
					do {
						Process pinger = new Pinger(new ProcessId("pinger:"+this.me.name),
								m.ballot_number.leader_id,ballot_number,env);
						try {
							((Thread)pinger).join();
							if (!((Pinger)pinger).success)
								break;
							else {
								writer.println("Not preempting "+m.ballot_number.leader_id +" successfully pinged");
								writer.flush();
							}
						} catch(Exception e) { 
							writer.println("Exception pinging");
						}
					}while(true);
					writer.println(this.me+" Continue Preempting ");
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
				writer.println("Leader: unknown msg type");
			}
		}
	}
}
