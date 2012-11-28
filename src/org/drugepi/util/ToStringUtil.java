package org.drugepi.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ToStringUtil {
	private static String DEFAULT_SEPARATOR = "\n";
	private static String DEFAULT_ARROW = "->";
	
	public static String toString(List<?> l, String separator) {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (Object object : l) {
			sb.append(sep).append(object.toString());
			sep = separator;
		}
		return sb.toString();
	}

	public static String toString(List<?> l) {
		return(toString(l, DEFAULT_SEPARATOR));
	}

	public static String toString(Map<?, ?> m, String separator, String arrow) {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (Object object : m.keySet()) {
			sb.append(sep).append(object.toString()).append(arrow)
					.append(m.get(object).toString());
			sep = separator;
		}
		return sb.toString();
	}

	public static String toString(Map<?, ?> m) {
		return(toString(m, DEFAULT_SEPARATOR, DEFAULT_ARROW));
	}

	public static String toString(Set<?> s, String separator) {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (Object object : s) {
			sb.append(sep).append(object.toString());
			sep = separator;
		}
		return sb.toString();
	}

	public static String toString(Set<?> s) {
		return(toString(s, DEFAULT_SEPARATOR));
	}
}