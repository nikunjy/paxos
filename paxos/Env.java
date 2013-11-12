import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class Env {
	Map<ProcessId, Process> procs = new HashMap<ProcessId, Process>();
	ProcessId [] acceptors;
	ProcessId [] replicas;
	ProcessId [] leaders;
	public final static int nAcceptors = 3, nReplicas = 2, nLeaders = 2;
	private int numClients;
	synchronized void sendMessage(ProcessId dst, PaxosMessage msg){
		Process p = procs.get(dst);
		if (p != null) {
			p.deliver(msg);
		}
	}
	synchronized void sendPingMessage(ProcessId dst, PaxosMessage msg){
		Process p = procs.get(dst);
		for (ProcessId id : procs.keySet()) {
			if (id.equals(dst)) { 
			 p = procs.get(id);	
			}
		}
		if (p != null) {
			p.deliver(msg);
		}
	}
	synchronized void addProc(ProcessId pid, Process proc){
		procs.put(pid, proc);
		proc.start();
	}
	synchronized void removeProc(ProcessId pid){
		procs.remove(pid);
	}
	class Client extends Thread {
		public ProcessId pid;
		public int clientId;
		public Env env;
		public void run() { 
			try {
				BufferedReader br = new BufferedReader(new FileReader(clientId+".txt"));
				String line = "";
				while ((line = br.readLine()) != null) {
					System.out.println("Client "+clientId +" executing "+line);
					for (int r = 0; r < nReplicas; r++) {
						sendMessage(env.replicas[r],
								new RequestMessage(pid, new Command(pid, 0,line)));
					}	
				}
			} catch(Exception e) {
			}finally {
			}
		}
	};
	void run(String[] args){
		acceptors = new ProcessId[nAcceptors];
		replicas = new ProcessId[nReplicas];
		leaders = new ProcessId[nLeaders];

		for (int i = 0; i < nAcceptors; i++) {
			acceptors[i] = new ProcessId("acceptor:" + i);
			Acceptor acc = new Acceptor(this, acceptors[i]);
		}
		for (int i = 0; i < nReplicas; i++) {
			replicas[i] = new ProcessId("replica:" + i);
			Replica repl = new Replica(this, replicas[i], leaders);
		}
		for (int i = 0; i < nLeaders; i++) {
			leaders[i] = new ProcessId("leader:" + i);
			Leader leader = new Leader(this, leaders[i], acceptors, replicas);
		}
		if (numClients == 1) {
			for (int i = 1; i < 10; i++) {
				ProcessId pid = new ProcessId("client:" + i);
				BankOperation op = new BankOperation();
				op.op = BankOperation.OperationTypes.ADDACCOUNT.value();
				op.holderName = "Client "+i;
				op.amount = 500;
				for (int r = 0; r < nReplicas; r++) {
					sendMessage(replicas[r],
							new RequestMessage(pid, new Command(pid, 0, op.serialize())));
				}
			}
		} else { 
			for (int i = 1; i <= numClients ; i++) { 
				ProcessId pid = new ProcessId("client:" + i);
				System.out.println("starting client"+i);
				Client c = new Client(); 
				c.pid = pid; 
				c.env = this;
				c.clientId = i;
				((Thread)c).run();
			}
		}

	}

	public static void main(String[] args){
		Env obj = new Env();
		if (args.length>0) { 
			obj.numClients = Integer.parseInt(args[0]);
		} else { 
			obj.numClients = 1;
		}
		obj.run(args);
	}
}
