package llc.ufwa.activities.monitoring;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import llc.ufwa.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

public class GetAppTasksThread implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(GetAppTasksThread.class);

	public final static int GET_APP_TASKS_MESSAGE_SUCCESS = 764343;
	public final static int GET_APP_TASKS_MESSAGE_FAILED = -32532;

	private final ActivityManager activityManager;
	private final boolean showLogs;
	private final StopWatch watch;
	private final String activityClass;
	private final Set<String> launchers;
	private final Handler mHandler;
	private final Set<String> taskManagers;

	public GetAppTasksThread(final Handler mHandler, final ActivityManager activityManager,
								final boolean showLogs, final StopWatch watch,
								final String activityClass, final Set<String> launchers,
								final Set<String> taskManagers) {

		this.activityManager = activityManager;
		this.showLogs = showLogs;
		this.activityClass = activityClass;
		this.watch = watch;
		this.launchers = launchers;
		this.taskManagers = taskManagers;
		this.mHandler = mHandler;

	}

	@Override
	@SuppressWarnings ("deprecation")
	public void run() {

		final List<RunningTaskInfo> tasks = activityManager.getRunningTasks(10);

		if (showLogs) {
			logger.debug("watch time 1: " + watch.getTime() + "ms");
		}

		int id = -1;
		boolean isRoot = false;
		boolean isRunning = false;
		boolean isTop = false;
		int activityCount = 0;
		final boolean isTaskManagerTop;
		boolean isLauncherRoot = false;

		final Set<String> managers;

		synchronized (taskManagers) {
			managers = new HashSet<String>(taskManagers);
		}

		if (tasks.size() > 0 && managers.contains(tasks.get(0).baseActivity.getClassName().toLowerCase(
				Locale.getDefault()))) {
			isTaskManagerTop = true;
		}
		else {
			isTaskManagerTop = false;
		}

		if (tasks.get(0).baseActivity.getClassName().endsWith(".Launcher") || tasks.get(0).baseActivity.getClassName().endsWith(
				".home.HomeActivity")) {
			isLauncherRoot = true;
		}
		else {

			final String baseClass = tasks.get(0).baseActivity.getClassName();

			for (final String launcher : launchers) {

				//TODO optimize for O(Log(N))
				if (baseClass.contains(launcher)) {

					isLauncherRoot = true;
					break;

				}

			}

		}

		if (tasks.get(0).baseActivity.getClassName().equals(activityClass) || tasks.get(0).topActivity.getClassName().equals(
				activityClass)) {
			isTop = true;
		}

		if (showLogs) {
			logger.info("base task activity: " + tasks.get(0).baseActivity.getClassName());
		}

		String packageName = null;

		final Set<Integer> tasksWithBaseOfPackage = new HashSet<Integer>();
		final Set<Integer> tasksWithTopOfPackage = new HashSet<Integer>();

		boolean foundLastSelf = false;
		boolean isSettingsOpen = false;
		boolean isStockGoogleLauncher = false;

		for (final RunningTaskInfo task : tasks) {

			if ((task.baseActivity.getClassName().equals(activityClass) || task.topActivity.getClassName().equals(
					activityClass)) && !foundLastSelf) {

				foundLastSelf = true;

				id = task.id;

				if (packageName == null) {

					if (task.baseActivity.getClassName().equals(activityClass)) {
						packageName = task.baseActivity.getPackageName();
					}
					else {
						packageName = task.topActivity.getPackageName();
					}

				}

				if (task.baseActivity.getPackageName().equals(packageName)) {
					tasksWithBaseOfPackage.add(task.id);
				}

				if (task.topActivity.getPackageName().equals(packageName)) {
					tasksWithTopOfPackage.add(task.id);
				}

				if (task.baseActivity.getClassName().equals(activityClass)) {
					isRoot = true;
				}

				activityCount = task.numActivities;

				if (task.numRunning != 0) {
					isRunning = true;
				}

				break;

			}

			if (task.baseActivity.getPackageName().equals("com.android.settings")) {
				isSettingsOpen = true;
			}

			if ((task.baseActivity.getPackageName().equals("com.android.launcher")) || (task.baseActivity.getPackageName().toLowerCase().equals(("com.google.android.launcher.GEL").toLowerCase()))) {
				isStockGoogleLauncher = true;
			}

			if (showLogs) {
				logger.debug("tasK:id:" + task.id);
				logger.debug("tasK:numActivities:" + task.numActivities);
				logger.debug("tasK:numRunning:" + task.numRunning);
				logger.debug("tasK:baseClassName:" + task.baseActivity.getClassName());
				logger.debug("tasK:basePackageName:" + task.baseActivity.getPackageName());
				logger.debug("tasK:description:" + task.description);
				logger.debug("tasK:topClassName:" + task.topActivity.getClassName());
				logger.debug("tasK:topPackageName:" + task.topActivity.getPackageName());
			}

		}

		final AppTasksStatus appTasksStatus = new AppTasksStatus(isRoot, isRunning, activityCount,
				isTaskManagerTop, tasksWithBaseOfPackage, tasksWithTopOfPackage, isTop, id,
				isSettingsOpen, isStockGoogleLauncher, isLauncherRoot, showLogs);

		final Message completeMessage = mHandler.obtainMessage(GET_APP_TASKS_MESSAGE_SUCCESS,
				appTasksStatus);

		completeMessage.sendToTarget();

	}

	public static class AppTasksStatus {

		private final boolean isRoot;
		private final boolean isRunning;
		private final int activityCount;
		private final boolean isTaskManagerTop;
		private final Set<Integer> tasksWithBaseOfPackage;
		private final Set<Integer> tasksWithTopOfPackage;
		private final boolean isTop;
		private final int id;
		private final boolean isSettingsOpen;
		private final boolean isStockGoogleLauncher;
		private final boolean isLauncherRoot;

		private AppTasksStatus(final boolean isRoot, final boolean isRunning,
								final int activityCount, final boolean isTaskManagerTop,
								final Set<Integer> tasksWithBaseOfPackage,
								final Set<Integer> tasksWithTopOfPackage, final boolean isTop,
								final int id, final boolean isSettingsOpen,
								final boolean isStockGoogleLauncher, final boolean isLauncherRoot,
								final boolean showLogs) {

			this.id = id;
			this.isTop = isTop;
			this.tasksWithBaseOfPackage = new HashSet<Integer>(tasksWithBaseOfPackage);
			this.tasksWithTopOfPackage = new HashSet<Integer>(tasksWithTopOfPackage);
			this.isTaskManagerTop = isTaskManagerTop;
			this.isRunning = isRunning;
			this.isRoot = isRoot;
			this.activityCount = activityCount;
			this.isSettingsOpen = isSettingsOpen;
			this.isStockGoogleLauncher = isStockGoogleLauncher;
			this.isLauncherRoot = isLauncherRoot;

			if (showLogs) {
				logger.debug("isRoot: " + isRoot);
				logger.debug("isRunning: " + isRunning);
				logger.debug("activityCount: " + activityCount);
				logger.debug("isLauncherRoot: " + isLauncherRoot);
				logger.debug("isTaskManagerTop: " + isTaskManagerTop);
				logger.debug("isSettingsOpen: " + isSettingsOpen);
				logger.debug("isStockGoogleLauncher: " + isStockGoogleLauncher);
			}

		}

		public boolean isStockGoogleLauncher() {
			return isStockGoogleLauncher;
		}

		public boolean isLauncherRoot() {
			return isLauncherRoot;
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

		public boolean isTaskManagerTop() {
			return isTaskManagerTop;
		}

		public int getActivityCount() {
			return activityCount;
		}

		public boolean isRoot() {
			return isRoot;
		}

		public boolean isRunning() {
			return isRunning;
		}

	}

}
