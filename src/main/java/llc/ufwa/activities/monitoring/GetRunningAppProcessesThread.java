package llc.ufwa.activities.monitoring;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

public class GetRunningAppProcessesThread implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(GetRunningAppProcessesThread.class);

	public final static int RUNNING_APPS_MESSAGE_SUCCESS = 4334;
	public final static int RUNNING_APPS_MESSAGE_FAILED = -12134;

	private final ActivityManager activityManager;
	private final boolean showLogs;
	private final Handler mHandler;
	private final Context context;

	public GetRunningAppProcessesThread(final Handler mHandler, final Context context,
										final ActivityManager activityManager,
										final boolean showLogs) {

		this.activityManager = activityManager;
		this.showLogs = showLogs;
		this.mHandler = mHandler;
		this.context = context;

	}

	@Override
	public void run() {

		boolean isVisible = false;
		boolean isBackground = false;
		boolean isForeground = false;
		boolean isPerceptible = false;
		boolean isQuickSearch = false;
		boolean isQuickSearchVisible = false;
		boolean isProcessForeground = false;
		boolean isSystemUIForeground = false;
		boolean foundProcess = false;

		final List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();

		if (showLogs) {
			//logger.debug("checking for process name: " + packageName);
		}

		for (final RunningAppProcessInfo appProcess : appProcesses) {

			if (appProcess.processName.equalsIgnoreCase("com.google.android.googlequicksearchbox:search")) {
				if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
					isQuickSearch = true;
				}
				else if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
					isQuickSearchVisible = true;
				}
			}

			if (((appProcess.processName.equalsIgnoreCase(context.getPackageName())) && (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND))) {
				isProcessForeground = true;
			}

			if ((appProcess.processName.equalsIgnoreCase("com.android.systemui")) && (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND)) {
				isSystemUIForeground = true;
			}

			if (appProcess.processName.equals(context.getPackageName()) && !foundProcess) {

				foundProcess = true;

				if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
					isBackground = true;
				}
				else if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
					isVisible = true;
				}
				else if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
					isForeground = true;
				}
				else if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE) {
					isPerceptible = true;
				}

				if (showLogs) {
					logger.debug("process:name:" + appProcess.processName);
					logger.debug("process:importance:" + appProcess.importance);
					logger.debug("process:importanceReasonCode:" + appProcess.importanceReasonCode);
					logger.debug("process:importanceReasonPid:" + appProcess.importanceReasonPid);
					logger.debug("process:lru:" + appProcess.lru);
					logger.debug("process:pid:" + appProcess.pid);
					logger.debug("process:uid:" + appProcess.uid);
					logger.debug("process:importanceReasonComponent:" + appProcess.importanceReasonComponent);
				}

				break;

			}

		}

		final RunningAppProcessesStatus runningAppsStatus = new RunningAppProcessesStatus(
				isVisible, isBackground, isForeground, isPerceptible, isQuickSearch,
				isQuickSearchVisible, isProcessForeground, isSystemUIForeground, showLogs);

		final Message completeMessage = mHandler.obtainMessage(RUNNING_APPS_MESSAGE_SUCCESS,
				runningAppsStatus);

		completeMessage.sendToTarget();

	}

	public static class RunningAppProcessesStatus {

		private final boolean isVisible;
		private final boolean isBackground;
		private final boolean isForeground;
		private final boolean isPerceptible;
		private final boolean isQuickSearch;
		private final boolean isQuickSearchVisible;
		private final boolean isProcessForeground;
		private final boolean isSystemUIForeground;

		private RunningAppProcessesStatus(boolean isVisible, boolean isBackground,
											boolean isForeground, boolean isPerceptible,
											boolean isQuickSearch, boolean isQuickSearchVisible,
											boolean isProcessForeground,
											boolean isSystemUIForeground, boolean showLogs) {

			this.isVisible = isVisible;
			this.isBackground = isBackground;
			this.isForeground = isForeground;
			this.isPerceptible = isPerceptible;
			this.isQuickSearch = isQuickSearch;
			this.isQuickSearchVisible = isQuickSearchVisible;
			this.isSystemUIForeground = isSystemUIForeground;
			this.isProcessForeground = isProcessForeground;

			if (showLogs) {
				logger.debug("isForeground: " + isForeground);
				logger.debug("isBackground: " + isBackground);
				logger.debug("isVisible: " + isVisible);
				logger.debug("isQuickSearch: " + isQuickSearch);
				logger.debug("isQuickSearchVisible: " + isQuickSearchVisible);
				logger.debug("isPerceptible: " + isPerceptible);
				logger.debug("isProcessForeground: " + isProcessForeground);
				logger.debug("isSystemUIForeground: " + isSystemUIForeground);
			}

		}

		public boolean isSystemUIForeground() {
			return isSystemUIForeground;
		}

		public boolean isQuickSearch() {
			return isQuickSearch;
		}

		public boolean isQuickSearchVisible() {
			return isQuickSearchVisible;
		}

		public boolean isPerceptible() {
			return isPerceptible;
		}

		public boolean isProcessForeground() {
			return isProcessForeground;
		}

		public boolean isForeground() {
			return isForeground;
		}

		public boolean isVisible() {
			return isVisible;
		}

		public boolean isBackground() {
			return isBackground;
		}

	}

}
