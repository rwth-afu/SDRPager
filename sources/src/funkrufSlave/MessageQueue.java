package funkrufSlave;

public class MessageQueue {
	private MessageQueueObject first;
	private MessageQueueObject last;
	
	
	// constructor
	public MessageQueue() {
		this.first = null;
		this.last = null;
	}
	
	// check if empty
	public boolean isEmpty() {
		return (this.first == null);
	}
	
	// push message
	public void push(Message message) {
		// if this is the first message
		if(this.first == null || this.last == null) {
			
			this.first = new MessageQueueObject(message);
			this.last = this.first;
			
		} else {
			// if not first message 
			
			MessageQueueObject mqObject = this.last;
			this.last = new MessageQueueObject(message, mqObject);
			mqObject.setNext(this.last);
			
		}
		
		
	}
	
	// push message (first position)
	public void pushFirst(Message message) {
		MessageQueueObject mqObject = new MessageQueueObject(message);
		
		// if this is the first message
		if(this.first == null) {
			
			this.first = mqObject;
			this.last = mqObject;
			
		} else {
			// if not first message
			
			mqObject.setNext(first);
			first.setPrevious(mqObject);
			first = mqObject;
			
		}
	}
	
	// get first message
	public Message pop() {
		// if empty
		if(isEmpty()) {
			return null;
		}
		
		// if not empty
		
		// get first message
		MessageQueueObject mqObject = this.first;
		this.first = this.first.getNext();
		
		// if empty now
		if(this.first == null) {
			this.last = null;
		}
		
		// return message
		return mqObject.getMessage();
	}
}
