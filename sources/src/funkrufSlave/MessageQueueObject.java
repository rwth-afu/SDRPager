package funkrufSlave;

public class MessageQueueObject {
	private Message message;
	private MessageQueueObject next;
	private MessageQueueObject previous;
	
	// constructor
	public MessageQueueObject(Message message) {
		this.message = new Message(message);
		this.next = null;
		this.previous = null;
	}
	
	// constructor
	public MessageQueueObject(Message message, MessageQueueObject previous)  {
		this(message);
		this.previous = previous;
	}
	
	// constructor
	public MessageQueueObject(Message message, MessageQueueObject previous, MessageQueueObject next)  {
		this(message, previous);
		this.next = next;
	}
	
	// getter
	public MessageQueueObject getNext() {
		return this.next;
	}
	
	// getter
	public MessageQueueObject getPrevious() {
		return this.previous;
	}
	
	// getter
	public Message getMessage() {
		return this.message;
	}
	
	// setter
	public void setNext(MessageQueueObject mqObject) {
		this.next = mqObject;
	}
	
	// setter
	public void setPrevious(MessageQueueObject mqObject) {
		this.previous = mqObject;
	}
}
