package edu.washington.nsre.util;

public class DelimitedEscape {

	private enum State {
		SPECIAL_CHAR, NORMAL
	};

	public static String unescape(String f) {
		StringBuilder sb = new StringBuilder();
		State state = State.NORMAL;
		for (int i = 0; i < f.length(); i++) {
			char c = f.charAt(i);

			if (state == State.SPECIAL_CHAR) {
				if (c == 'n') {
					sb.append('\n');
				} else if (c == 'r') {
					sb.append('\r');
				} else if (c == 't') {
					sb.append('\t');
				} else if (c == '\\') {
					sb.append('\\');
				} else {
					//System.err.println("error unknown special char : " + c);					
				}
				state = State.NORMAL;
			} else if (state == State.NORMAL) {
				if (c == '\\') {
					state = State.SPECIAL_CHAR;
				} else {
					sb.append(c);
				}
			}
		}
		return sb.toString();
	}

	public static String escape(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\')
				sb.append("\\\\");
			else if (c == '\n')
				sb.append("\\n");
			else if (c == '\r')
				sb.append("\\r");
			else if (c == '\t')
				sb.append("\\t");
			else
				sb.append(c);
		}
		return sb.toString();
	}

}
