import java.util.Date;

class Pinger extends Process{
	public ProcessId leader; 
	public ProcessId winnerLeader;
	boolean success; 
	public BallotNumber bn;
	public Pinger(ProcessId leader, ProcessId winnerLeader, BallotNumber bn,Env env) {
		this.leader = leader;
		this.winnerLeader = winnerLeader;
		this.bn = bn;
		success = false;
		this.env = env;
		this.me = leader;
		env.addProc(this.me,this);
	}
	public void body() { 
		long timeOut = 5000/(bn.round+1);
		PingRequestMessage request = new PingRequestMessage(leader,new Command(leader,0,"Ping Request"));
		sendMessage(winnerLeader,request);
		System.out.println("Pinging from "+this.me +" to "+winnerLeader);
		PaxosMessage msg = getPingMessage(timeOut);
		if (msg == null) {
			System.out.println("Pinging from "+this.me +" to "+winnerLeader+ " timed out");
			success = false;
			return;
		}
		if (!(msg instanceof PingReplyMessage)) {
			success = false;
			return;
		}
		PingReplyMessage reply = (PingReplyMessage)msg; 
		System.out.println(this.me+" Got a reply from "+msg.src);
		if (reply.src.equals(winnerLeader)) {
			success = true;
			return;
		}
	}
}