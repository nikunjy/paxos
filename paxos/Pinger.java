import java.io.PrintWriter;
import java.util.Date;

class Pinger extends Process{
	public ProcessId leader; 
	public ProcessId winnerLeader;
	boolean success; 
	public BallotNumber bn;
	static long initialTimeOut = 5000;
	static void decTimeOut(long timeOut) { 
		if (initialTimeOut - timeOut <= 1000) { 
			return;
		}
		initialTimeOut -= timeOut;
	}
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
		long timeOut = initialTimeOut * (bn.round+1);
		PingRequestMessage request = new PingRequestMessage(leader,new Command(leader,0,"Ping Request"));
		sendMessage(winnerLeader,request);
		PaxosMessage msg = getPingMessage(timeOut);
		if (msg == null) {
			success = false;
			return;
		}
		if (!(msg instanceof PingReplyMessage)) {
			success = false;
			return;
		}
		PingReplyMessage reply = (PingReplyMessage)msg; 
		if (reply.src.equals(winnerLeader)) {
			try {Thread.sleep(2000);}catch(Exception e){}
			success = true;
			return;
		}
	}
}