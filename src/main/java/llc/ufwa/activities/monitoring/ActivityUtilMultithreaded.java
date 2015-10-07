package llc.ufwa.activities.monitoring;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import llc.ufwa.activities.ActivityUtil;
import llc.ufwa.activities.monitoring.GetAppTasksThread.AppTasksStatus;
import llc.ufwa.activities.monitoring.GetRunningAppProcessesThread.RunningAppProcessesStatus;
import llc.ufwa.concurrency.Callback;
import llc.ufwa.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

public class ActivityUtilMultithreaded {

	private static final Logger logger = LoggerFactory.getLogger(ActivityUtil.class);

	private static final Set<String> taskManagers = new HashSet<String>();

	public static final int SET_CALLBACK = 2134312;

	static {

		taskManagers.add("com.android.systemui.recent.RecentAppFxActivity".toLowerCase(Locale.getDefault()));
		taskManagers.add("com.sec.android.app.controlpanel.activity.JobManagerActivity".toLowerCase(Locale.getDefault()));
		taskManagers.add("com.android.systemui.recent.RecentsActivity".toLowerCase(Locale.getDefault()));

	}

	private static final Set<String> homeLaunchers = new HashSet<String>();

	static {

		homeLaunchers.add("com.motorola.blur.home.HomeActivity");
		homeLaunchers.add("com.htc.launcher.Launcher");
		homeLaunchers.add("com.motorola.mmsp.threed.motohome.HomeActivity");
		homeLaunchers.add("com.facebook.dash.activities.DashActivity");
		homeLaunchers.add("com.jiubang.ggheart.apps.desks.diy.GoLauncher");
		homeLaunchers.add("net.pierrox.lightning_launcher.activities.Dashboard");
		homeLaunchers.add("info.tikusoft.launcher7.MainScreen");
		homeLaunchers.add("ginlemon.flower.Workspace");
		homeLaunchers.add("com.nemustech.regina.ReginaLauncher");
		homeLaunchers.add("com.lx.launcher8.AnallLauncher");
		homeLaunchers.add("com.vire.launcher.VireLauncher");

	}

	public static void getStatus(final String activityClass, final Context context,
			final Set<String> launchers, final Handler mHandler,
			final Callback<Void, ActivityMultithreadedStatus> callback) {
		getStatus(activityClass, context, launchers, false, mHandler, callback);
	}

	public static void getStatus(final String activityClass, final Context context,
			final Set<String> launchers, final boolean showLogs, final Handler mHandler,
			final Callback<Void, ActivityMultithreadedStatus> callback) {

		try {

			final StopWatch watch = new StopWatch();
			watch.start();

			final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

			if (showLogs) {
				logger.debug("watch time 2: " + watch.getTime() + "ms");
			}

			final Message completeMessage = mHandler.obtainMessage(SET_CALLBACK, callback);
			completeMessage.sendToTarget();

			final GetAppTasksThread getAppTasksRunnable = new GetAppTasksThread(mHandler,
					activityManager, showLogs, watch, activityClass, launchers, taskManagers);
			final GetRunningAppProcessesThread getRunningProcessesRunnable = new GetRunningAppProcessesThread(
					mHandler, context, activityManager, showLogs);

			final Thread getAppTasksThread = new Thread(getAppTasksRunnable);
			final Thread getRunningProcessesThread = new Thread(getRunningProcessesRunnable);

			getAppTasksThread.start();
			getRunningProcessesThread.start();

		}
		catch (Exception e) {
			if (showLogs) {
				logger.error("activity status interrupted");
			}
		}

	}

	public static class ActivityMultithreadedStatus {

		private final boolean isForeground;
		private final boolean isVisible;
		private final boolean isBackground;
		private final boolean isRoot;
		private final boolean isRunning;

		private final int activityCount;
		private final boolean isTaskManagerTop;
		private final boolean isLaucherTop;
		private final Set<Integer> tasksWithBaseOfPackage;
		private final Set<Integer> tasksWithTopOfPackage;
		private final boolean isTop;
		private final int id;
		private final boolean isQuickSearch;
		private final boolean isQuickSearchVisible;
		private final boolean isPerceptible;
		private final boolean isProcessForeground;
		private final boolean isSettingsOpen;
		private final boolean isStockGoogleLauncher;
		private final boolean isSystemUIForeground;

		public ActivityMultithreadedStatus(AppTasksStatus appTasksStatusObj,
											RunningAppProcessesStatus appProcessesStatusObj) {

			isRoot = appTasksStatusObj.isRoot();
			isRunning = appTasksStatusObj.isRunning();
			activityCount = appTasksStatusObj.getActivityCount();
			isTaskManagerTop = appTasksStatusObj.isTaskManagerTop();
			isLaucherTop = appTasksStatusObj.isLauncherRoot();
			tasksWithBaseOfPackage = appTasksStatusObj.getTasksWithBaseOfPackage();
			tasksWithTopOfPackage = appTasksStatusObj.getTasksWithTopOfPackage();
			isTop = appTasksStatusObj.isTop();
			id = appTasksStatusObj.getID();
			isSettingsOpen = appTasksStatusObj.isSettingsOpen();
			isStockGoogleLauncher = appTasksStatusObj.isStockGoogleLauncher();

			isForeground = appProcessesStatusObj.isForeground();
			isVisible = appProcessesStatusObj.isVisible();
			isBackground = appProcessesStatusObj.isBackground();
			isQuickSearch = appProcessesStatusObj.isQuickSearch();
			isQuickSearchVisible = appProcessesStatusObj.isQuickSearchVisible();
			isPerceptible = appProcessesStatusObj.isPerceptible();
			isProcessForeground = appProcessesStatusObj.isProcessForeground();
			isSystemUIForeground = appProcessesStatusObj.isSystemUIForeground();

		}

		public int activeTasksSize() {

			int size = 0;

			if (tasksWithBaseOfPackage != null) {
				size += tasksWithBaseOfPackage.size();
			}

			if (tasksWithTopOfPackage != null) {
				size += tasksWithTopOfPackage.size();
			}

			return size;

		}

		public boolean isSystemUIForeground() {
			return isSystemUIForeground;
		}

		public boolean isStockGoogleLauncher() {
			return isStockGoogleLauncher;
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

		public boolean isSettingsOpen() {
			return isSettingsOpen;
		}

		public int getID() {
			return id;
		}

		public boolean isTop() {
			return isTop;
		}

		public Set<Integer> getTasksWithBaseOfPackage() {
			return new HashSet<Integer>(tasksWithBaseOfPackage);
		}

		public Set<Integer> getTasksWithTopOfPackage() {
			return new HashSet<Integer>(tasksWithTopOfPackage);
		}

		public boolean isLaucherTop() {
			return isLaucherTop;
		}

		public boolean isTaskManagerTop() {
			return isTaskManagerTop;
		}

		public int getActivityCount() {
			return activityCount;
		}

		public boolean isRoot() {
			return isRoot;
		}

		public boolean isForeground() {
			return isForeground;
		}

		public boolean isVisible() {
			return isVisible;
		}

		public boolean isRunning() {
			return isRunning;
		}

		public boolean isBackground() {
			return isBackground;
		}

	}

}
