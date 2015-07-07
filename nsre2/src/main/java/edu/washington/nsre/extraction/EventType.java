package edu.washington.nsre.extraction;

public class EventType {
	String str;
	EventPhrase eventphrase;
	String arg1type;
	String arg2type;
	int arg1typelen = 1;
	int arg2typelen = 1;

	public EventType(String str) {
		this.str = str;
		String[] abc = str.split("@");
		String head = abc[0].split(" ")[0];
		eventphrase = new EventPhrase(this, abc[0], head);
		arg1type = abc[1];
		arg2type = abc[2];
		if (arg1type.indexOf("/", 1) > 0) {
			arg1typelen = 2;
		}
		if (arg2type.indexOf("/", 2) > 0) {
			arg2typelen = 2;
		}
	}

	public String getArg1TypeRoot() {
		return getTypeRoot(arg1type);
	}

	public String getArg2TypeRoot() {
		return getTypeRoot(arg2type);
	}

	private String getTypeRoot(String type) {
		String[] ab = type.split("/");
		StringBuilder sb = new StringBuilder();
		for (String x : ab) {
			if (x.length() == 0)
				continue;
			sb.append("/" + x);
			break;
		}
		return sb.toString();
	}

	public int hashCode() {
		return str.hashCode();
	}

	public boolean equals(Object o) {
		if (o instanceof EventType)
			return this.str.equals(((EventType) o).str);
		else
			return false;
	}

	public String toString() {
		return str;
	}
}