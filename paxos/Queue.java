import java.util.*;

 public class Queue<T> {
	LinkedList<T> ll = new LinkedList<T>();
	LinkedList<T> pl = new LinkedList<T>();
	public synchronized void enqueue(T obj){
		ll.add(obj);
		notify();
	}

	public synchronized T bdequeue(){
		while (ll.size() == 0) {
			try { wait(); } catch (InterruptedException e) {}
		}
		return ll.removeFirst();
	}
	public synchronized void enqueuePing(T obj){
		pl.add(obj);
		notify();
	}

	public synchronized T pingDequeue(){
		while (pl.size() == 0) {
			try { wait(); } catch (InterruptedException e) {System.out.println(e);}
		}	
		return pl.removeFirst();
	}
	public synchronized T pingDequeue(long timeOut){
		while (pl.size() == 0) {
			try { Thread.sleep(timeOut); } catch (InterruptedException e) {System.out.println(e);}
		}
		
		return pl.removeFirst();
	}
}
