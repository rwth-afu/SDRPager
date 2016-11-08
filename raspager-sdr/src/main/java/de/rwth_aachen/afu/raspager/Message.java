package de.rwth_aachen.afu.raspager;

import java.util.List;

final class Message {
	private final int type;
	private final int speed;
	private final int address;
	private final int function;
	private final String text;
	private List<Integer> codeWords; // 0 = framePos, 1 = cw, 2 = cw, ...

	public Message(String str) {
		this(str.split(":", 5));
	}

	public Message(String[] parts) {
		if (parts.length < 5) {
			throw new IllegalArgumentException("Invalid sized array.");
		}

		type = parts[0].charAt(4) - '0';
		speed = Integer.parseInt(parts[1]);
		address = Integer.parseInt(parts[2], 16);
		function = Integer.parseInt(parts[3]);
		text = parts[4];

		switch (type) {
		case 5:
			// numeric
			// #00 5:1:9C8:0:094016 130412
			codeWords = Pocsag.encodeNumber(address, function, text);
			break;
		case 6:
			// alpha numeric
			codeWords = Pocsag.encodeText(address, function, text);
			break;
		default:
			throw new IllegalArgumentException("Invalid message type: " + type);
		}
	}

	public int getType() {
		return type;
	}

	public int getSpeed() {
		return speed;
	}

	public long getAddress() {
		return address;
	}

	public int getFunction() {
		return function;
	}

	public String getText() {
		return text;
	}

	public List<Integer> getCodeWords() {
		return codeWords;
	}
}
