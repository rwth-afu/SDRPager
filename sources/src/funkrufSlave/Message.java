package funkrufSlave;

import java.util.ArrayList;

public class Message {
	private int type;
	private int speed;
	private int address;
	private int function;
	private String text;

	private ArrayList<Integer> codeWords; // 0 = framePos, 1 = cw, 2 = cw, 3 =
											// ....

	// constructor
	public Message(String[] parts) {
		this.type = parts[0].charAt(4) - '0';
		this.speed = Integer.parseInt(parts[1]);
		this.address = Integer.parseInt(parts[2], 16);
		this.function = Integer.parseInt(parts[3]);
		this.text = parts[4];

		this.encode();
	}

	// copy-constructor
	public Message(Message message) {
		this.type = message.type;
		this.speed = message.speed;
		this.address = message.address;
		this.function = message.function;
		this.text = message.text;

		this.encode();
	}

	// getter
	public int getType() {
		return this.type;
	}

	// getter
	public int getSpeed() {
		return this.speed;
	}

	// getter
	public long getAddress() {
		return this.address;
	}

	// getter
	public int getFunction() {
		return this.function;
	}

	// getter
	public String getText() {
		return this.text;
	}

	// getter
	public ArrayList<Integer> getCodeWords() {
		return this.codeWords;
	}

	// encode message depending on type
	public boolean encode() {
		switch (this.type) {
		case 5:
			// numeric
			// #00 5:1:9C8:0:094016 130412
			this.codeWords = Pocsag.encodeNum(this.address, this.function, this.text);

			return true;

		case 6:
			// alpha numeric

			this.codeWords = Pocsag.encodeStr(this.address, this.function, this.text);

			return true;
		}

		return false;
	}
}
