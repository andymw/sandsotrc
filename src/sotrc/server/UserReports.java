package sotrc.server;

import java.io.*;
import java.text.*;
import java.util.*;

import common.Callback;

public class UserReports {
	private static final String REPORT_MASTER_DIRECTORY = ServerProperties.SOTRC_REPORTS_DIR;
	private static final String DATE_TIME_FORMAT_STR = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	private static final String DATE_FORMAT_STR = "yyyy-MM-dd";
	private static final String FS = " -- "; // field separator

	private static final int MAX_RECENT = 4096;
	private static final LinkedList<String> recentEntries = new LinkedList<String>();

	static { // ensure log directory exists
		new File(REPORT_MASTER_DIRECTORY).mkdirs();
	}

	private static String getTimestamp() {
		return new SimpleDateFormat(DATE_TIME_FORMAT_STR).format(new Date());
	}

	private static String getTodayFilename() {
		return new SimpleDateFormat(DATE_FORMAT_STR).format(new Date());
	}

	/**
	 * Logs a user report.
	 */
	public static synchronized boolean logReport(UUID chatID,
			String issuingUser, String offendingUser, String detail) {
		boolean success = true;
		FileWriter fw = null;
		String toLog = getTimestamp() + FS + "Chat " + chatID.toString() + FS
			+ "User " + issuingUser + " reported user " + offendingUser
			+ (detail == null ? "\n" : FS + detail + "\n");
		recentEntries.push(toLog);
		if (recentEntries.size() > MAX_RECENT)
			recentEntries.removeLast();
		try {
			String logfile = REPORT_MASTER_DIRECTORY + getTodayFilename();
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
		File[] listOfFiles = new File(REPORT_MASTER_DIRECTORY).listFiles();

		for (File file : listOfFiles) {
			if (!file.isFile()) continue;
			String name = file.getName();
			if (name.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
				dateFiles.add(REPORT_MASTER_DIRECTORY + name);
			} else {
				System.out.println("Found a file that doesn't match date convention.");
			}
		}

		Collections.sort(dateFiles); // will be by chronological order
		return dateFiles;
	}

	/**
	 * Calls the supplied callback on recent log entries.
	 */
	public static synchronized void searchRecentEntries(int entries, Callback<String> callback) {
		for (String entry : recentEntries) {
			callback.process(entry);
			entries--;
			if (entries == 0) break;
		}
	}

}
