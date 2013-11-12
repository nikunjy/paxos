public abstract class Process extends Thread {
	ProcessId me;
	Queue<PaxosMessage> inbox = new Queue<PaxosMessage>();
	Env env;

	abstract void body();

	public void run(){
		body();
		env.removeProc(me);
	}

	PaxosMessage getNextMessage(){
		return inbox.bdequeue();
	}
	
	PaxosMessage getPingMessage() { 
		return inbox.pingDequeue();
	}
	
	void sendPing(ProcessId dst, PaxosMessage msg) {
		System.out.println("pinging "+dst+" from "+msg.src);
		env.sendPingMessage(dst, msg);
	}
	void sendMessage(ProcessId dst, PaxosMessage msg){
		env.sendMessage(dst, msg);
	}

	void deliver(PaxosMessage msg){
		inbox.enqueue(msg);
	}
	void deliverPing(PaxosMessage msg){
		inbox.enqueuePing(msg);
	}
}
