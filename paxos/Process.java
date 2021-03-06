import java.util.Date;

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
	PaxosMessage getPingMessage(long timeOut) { 
		return inbox.bdequeue(timeOut);
	}
	void sendMessage(ProcessId dst, PaxosMessage msg){
		env.sendMessage(dst, msg);
	}

	void deliver(PaxosMessage msg){
		inbox.enqueue(msg);
	}
}
