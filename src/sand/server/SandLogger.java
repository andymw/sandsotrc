package sand.server;

import java.io.*;
import java.text.*;
import java.util.*;

import common.Callback;
import common.network.StatusCode;

import sand.common.SandAction;

public class SandLogger {

	private static final String LOG_MASTER_DIRECTORY = SandServerProperties.SAND_LOG_DIR;
	private static final String DATE_TIME_FORMAT_STR = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	private static final String DATE_FORMAT_STR = "yyyy-MM-dd";
	private static final String FS = " -- "; // field separator

	private static final int MAX_RECENT = 20000;
	private static final LinkedList<String> recentEntries = new LinkedList<String>();

	static { // ensure log directory exists
		new File(LOG_MASTER_DIRECTORY).mkdirs();
	}

	private static String getTimestamp() {
		return new SimpleDateFormat(DATE_TIME_FORMAT_STR).format(new Date());
	}

	private static String getTodayFilename() {
		return new SimpleDateFormat(DATE_FORMAT_STR).format(new Date());
	}

	/**
	 * Logs an action.
	 * @param action the action to log
	 * @param userString the user identifier string
	 * @param code the status code result
	 * @param detail an optional detail string (may be null)
	 */
	public static synchronized boolean logAction(
			SandAction action, String userString, StatusCode code, String detail) {
		boolean success = true;
		FileWriter fw = null;
		String toLog = getTimestamp() + FS + userString + FS + action.toString()
			+ FS + "code " + code + (detail == null ? "\n" : FS + detail + "\n");
		recentEntries.push(toLog);
		if (recentEntries.size() > MAX_RECENT)
			recentEntries.removeLast();
		try {
			String logfile = LOG_MASTER_DIRECTORY + getTodayFilename();
			fw = new FileWriter(logfile, true/*append*/);
			fw.write(toLog);
			fw.close();
			return true;
		} catch (IOException e) { success = false; }
		finally {
			try { if (fw != null) fw.close(); }
			catch (IOException ignore) { }
		}
		return success;
	}

	/**
	 * Called during server boot-up, populates recentEntries with
	 * the latest MAX_RECENT events.
	 */
	public static synchronized void populateSearchEntries() {
		List<String> dateFiles = getSortedLogEntryFiles();

		for (int idx = 0; idx < dateFiles.size()
				&& recentEntries.size() < MAX_RECENT ; ++idx) {
			try (BufferedReader br = new BufferedReader(
					new FileReader(dateFiles.get(idx)))) {
				String line;
				while ((line = br.readLine()) != null) {
					recentEntries.push(line);
					if (recentEntries.size() >= MAX_RECENT)
						return; // done
				}
			} catch (IOException e) {
				System.out.println("File not found, or unable to read file.");
				System.err.println(e.toString());
			}
		}
	}

	private static List<String> getSortedLogEntryFiles() {
		List<String> dateFiles = new ArrayList<String>();
		File[] listOfFiles = new File(LOG_MASTER_DIRECTORY).listFiles();

		for (File file : listOfFiles) {
			if (!file.isFile()) continue;
			String name = file.getName();
			if (name.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
				dateFiles.add(LOG_MASTER_DIRECTORY + name);
			} else {
				System.out.println("Found a file that doesn't match date convention.");
			}
		}

		Collections.sort(dateFiles); // will be by chronological order
		return dateFiles;
	}

	/**
	 * Calls the supplied callback on all recent log entries containing str1 or str2.
	 * Limit to entries.
	 */
	public static synchronized void searchRecentEntries(
			String str1, String str2, int entries, Callback<String> callback) {
		for (String entry : recentEntries) {
			if (entry.contains(str1) || entry.contains(str2)) {
				callback.process(entry);
				entries--;
			}
			if (entries == 0) break;
		}
	}

}
