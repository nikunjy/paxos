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
		long timeOut = bn.round * 5000;
		long startTime = new Date().getTime();
		PingRequestMessage request = new PingRequestMessage(leader,new Command(leader,0,"Ping Request"));
		sendPing(winnerLeader,request);
		while (true) {
			PaxosMessage msg = getPingMessage();
			if (!(msg instanceof PingReplyMessage))
				continue;
			PingReplyMessage reply = (PingReplyMessage)msg; 
			System.out.println(this.me+" Got a reply from "+msg.src);
			if (reply.src.equals(winnerLeader)) {
				success = true;
				break;
			}
			long currentTime = new Date().getTime(); 
			if ( currentTime - startTime > timeOut) 
				break;
		}
	}
	
}
class PingAcceptor extends Process {
	public ProcessId leader;
	public PingAcceptor(ProcessId leader,Env env) { 
		this.leader = leader;
		this.env = env;
		this.me = leader;
		env.addProc(this.me,this);
	}
	public void body() { 
		int id = 0;
		System.out.println("Accepting pings now "+leader);
		while(true) { 
			PaxosMessage msg = getPingMessage();
			if (!(msg instanceof PingRequestMessage))
				continue;
			id++;
			PingReplyMessage reply = new PingReplyMessage(leader,new Command(leader,id,"Ping Reply"));
			sendPing(((PingRequestMessage)msg).src,reply);
		}
		
	}
}
