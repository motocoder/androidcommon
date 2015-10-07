package llc.ufwa.activities;


import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import llc.ufwa.activities.ActivityUtil.ActivityStatus;
import llc.ufwa.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.os.Debug.MemoryInfo;
import android.os.Handler;
import android.os.Looper;

public class ActivityUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(ActivityUtil.class);
    
    private static final Set<String> taskManagers = new HashSet<String>();
    
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
    
    public static ActivityStatus getStatus(final String activityClass, final Context context, final Set<String> launchers) {
        return getStatus(activityClass, context, launchers, true);
    }
    
    public static ActivityStatus getStatus(final String activityClass, final Context context, final Set<String> launchers, final boolean showLogs) {
        
        final StopWatch watch = new StopWatch();
        watch.start();
        
        final ActivityManager activityManager = 
            (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

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
        
        synchronized(taskManagers) {
            managers = new HashSet<String>(taskManagers);
        }
        
        if(tasks.size() > 0 && managers.contains(tasks.get(0).baseActivity.getClassName().toLowerCase(Locale.getDefault()))) {
            isTaskManagerTop = true;
        }
        else {
            isTaskManagerTop = false;
        }
        
        
        if(tasks.get(0).baseActivity.getClassName().endsWith(".Launcher")
           || tasks.get(0).baseActivity.getClassName().endsWith(".home.HomeActivity")) {
            isLauncherRoot = true;
        }
        else {
            
            final String baseClass = tasks.get(0).baseActivity.getClassName();
            
            for(final String launcher : launchers) {
                
                //TODO optimize for O(Log(N))
                if(baseClass.contains(launcher)) {
                    
                    isLauncherRoot = true;
                    break;
                           
                }
                
            }
            
        }
        
        if(tasks.get(0).baseActivity.getClassName().equals(activityClass) || tasks.get(0).topActivity.getClassName().equals(activityClass)) {
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
        
        for(final RunningTaskInfo task : tasks) {
            
            if(
                (task.baseActivity.getClassName().equals(activityClass) || task.topActivity.getClassName().equals(activityClass)) 
                && !foundLastSelf
            ) {
            	
            	
                          
                foundLastSelf = true;
                
                id = task.id;
                
                if(packageName == null) {
                    
                    if(task.baseActivity.getClassName().equals(activityClass)) {
                        packageName = task.baseActivity.getPackageName();
                    }
                    else {
                        packageName = task.topActivity.getPackageName();
                    }
                    
                }
                
//                if(task.numRunning > 0) {
                    
                    if(task.baseActivity.getPackageName().equals(packageName)) {
                        tasksWithBaseOfPackage.add(task.id);
                    }
                    
                    if(task.topActivity.getPackageName().equals(packageName)) {
                        tasksWithTopOfPackage.add(task.id);
                    }
                    
//                }
                
                if(task.baseActivity.getClassName().equals(activityClass)) {
                    isRoot = true;
                }
                
                activityCount = task.numActivities;
                
                if(task.numRunning != 0) {
                    isRunning = true;
                }
                                
                break;
                
            }
            
            if (task.baseActivity.getPackageName().equals("com.android.settings")) {
            	isSettingsOpen = true;
            }
            
            if ((task.baseActivity.getPackageName().equals("com.android.launcher")) ||
            		(task.baseActivity.getPackageName().toLowerCase().equals(("com.google.android.launcher.GEL").toLowerCase()))) {
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

        if (showLogs) {
        	logger.debug("watch time 2: " + watch.getTime() + "ms");
        }
        
        boolean isVisible = false;
        boolean isBackground = false;
        boolean isForeground = false;
        boolean isPerceptible = false;
        boolean isQuickSearch = false;
        boolean isQuickSearchVisible = false;
        boolean isProcessForeground = false;
//        boolean isLauncherProcessForeground = false;
        boolean isSystemUIForeground = false;
        
        final List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        
        boolean foundProcess = false;
        

        if (showLogs) {
        	logger.debug("checking for process name: " + packageName);
        }
        
        for(final RunningAppProcessInfo appProcess : appProcesses) {

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
            
            if (appProcess.processName.equalsIgnoreCase("com.google.android.googlequicksearchbox:search")) {
            	if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
            		isQuickSearch = true;
            	}
            	else if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
            		isQuickSearchVisible = true;
            	}
            }
            
            if (((appProcess.processName.equalsIgnoreCase(packageName))
            		&& (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND))) {
            	isProcessForeground = true;
            }
            
//            if ((appProcess.processName.equalsIgnoreCase("com.android.launcher"))
//            		&& (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND)) {
//            	isLauncherProcessForeground = true;
//            }
            
            if ((appProcess.processName.equalsIgnoreCase("com.android.systemui"))
            		&& (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND)) {
            	isSystemUIForeground = true;
            }
            
            if(appProcess.processName.equals(context.getPackageName()) && !foundProcess) {
                
                foundProcess = true;
                
                if(appProcess.importance == RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                    isBackground = true;
                }
                else if(appProcess.importance == RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    isVisible = true;
                }
                else if(appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    isForeground = true;
                }
                else if(appProcess.importance == RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE) {
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
                
//                for(final String pkg : appProcess.pkgList) {
////                    logger.debug("process:pkg:" + pkg);
//                }
                
                break;
                
            }
            
        }
//        
        if (showLogs) {
	        logger.debug("isRoot: " + isRoot);
	        logger.debug("isForeground: " + isForeground);
	        logger.debug("isBackground: " + isBackground);
	        logger.debug("isVisible: " + isVisible);
	        logger.debug("isRunning: " + isRunning);
	        logger.debug("activityCount: " + activityCount);
	        logger.debug("isQuickSearch: " + isQuickSearch);
	        logger.debug("isQuickSearchVisible: " + isQuickSearchVisible);
	        logger.debug("isLauncherRoot: " + isLauncherRoot);
	        logger.debug("isPerceptible: " + isPerceptible);
	        logger.debug("isTaskManagerTop: " + isTaskManagerTop);
	        logger.debug("isProcessForeground: " + isProcessForeground);
	        logger.debug("isSettingsOpen: " + isSettingsOpen);
	//        logger.debug("isLauncherProcessForeground: " + isLauncherProcessForeground);
	        logger.debug("isStockGoogleLauncher: " + isStockGoogleLauncher);
	        logger.debug("isSystemUIForeground: " + isSystemUIForeground);
        }
        

        if (showLogs) {
        	logger.debug("watch time 3: " + watch.getTime() + "ms");
        }
		
        return
            new ActivityStatus(
                isRoot,
                isForeground,
                isBackground,
                isVisible,
                isRunning,
                activityCount, 
                isTaskManagerTop,
                isLauncherRoot,
                tasksWithBaseOfPackage,
                tasksWithTopOfPackage,
                isTop,
                id,
                isQuickSearch,
                isQuickSearchVisible,
                isPerceptible,
                isProcessForeground,
                isSettingsOpen,
//                isLauncherProcessForeground,
                isStockGoogleLauncher,
                isSystemUIForeground
            );

    }

    public static class ActivityStatus {

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
//        private final boolean isLauncherProcessForeground;
        private final boolean isStockGoogleLauncher;
        private final boolean isSystemUIForeground;

        /**
         * 
         * @param isRoot
         * @param isForeground
         * @param isBackground
         * @param isVisible
         * @param isRunning
         * @param activityCount
         * @param isTaskManagerTop
         * @param tasksWithTopOfPackage 
         * @param tasksWithBaseOfPackage 
         * @param isPerceptible 
         * @param isProcessForeground 
         * @param isSettingsOpen 
         * @param isLauncherProcessForeground 
         * @param isStockGoogleLauncher 
         * @param isSystemUIForeground
         * @param booleanisSystemUIForeground2 
         */
        private ActivityStatus(
            final boolean isRoot,
            final boolean isForeground,
            final boolean isBackground,
            final boolean isVisible,
            final boolean isRunning,
            final int activityCount,
            final boolean isTaskManagerTop,
            final boolean isLauncherTop,
            final Set<Integer> tasksWithBaseOfPackage, 
            final Set<Integer> tasksWithTopOfPackage,
            final boolean isTop,
            final int id,
            final boolean isQuickSearch, 
            final boolean isQuickSearchVisible, 
            final boolean isPerceptible, 
            final boolean isProcessForeground,
            final boolean isSettingsOpen,
//            final boolean isLauncherProcessForeground,
            final boolean isStockGoogleLauncher,
            final boolean isSystemUIForeground
        ) {
            
            this.id = id;
            this.isTop = isTop;
            this.tasksWithBaseOfPackage = new HashSet<Integer>(tasksWithBaseOfPackage);
            this.tasksWithTopOfPackage = new HashSet<Integer>(tasksWithTopOfPackage);
            this.isLaucherTop = isLauncherTop;
            this.isTaskManagerTop = isTaskManagerTop;
            this.isRunning = isRunning;
            this.isForeground = isForeground;
            this.isVisible = isVisible;
            this.isBackground = isBackground;
            this.isRoot = isRoot;
            this.activityCount = activityCount;
            this.isQuickSearch = isQuickSearch;
            this.isPerceptible = isPerceptible;
            this.isProcessForeground = isProcessForeground;
            this.isSettingsOpen = isSettingsOpen;
//            this.isLauncherProcessForeground = isLauncherProcessForeground;
            this.isStockGoogleLauncher = isStockGoogleLauncher;
            this.isSystemUIForeground = isSystemUIForeground;
            this.isQuickSearchVisible = isQuickSearchVisible;
            
        }
        
        public boolean isSystemUIForeground() {
        	return isSystemUIForeground;
        }
        
        public boolean isStockGoogleLauncher() {
        	return isStockGoogleLauncher;
        }
        
//        public boolean isLauncherProcessForeground() {
//        	return isLauncherProcessForeground;
//        }

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

    public static boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
        
    }
    
    public static void runOnMainOrPost(final Runnable runnable, final Handler handler) {
        
        if(isMainThread()) {
            runnable.run();
        }
        else {
            handler.post(runnable);
        }
        
    }
    
    public static String getIPAddress(final boolean useIPv4) throws SocketException {
            
              final List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
              String theIPAddress = null;
              for (final NetworkInterface intf : interfaces) {
                  final List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                  for (final InetAddress addr : addrs) {
                      if (!addr.isLoopbackAddress()) {
                          final String sAddr = addr.getHostAddress().toUpperCase();
                          final boolean isIPv4 = isIPv4Address(sAddr); 
                          if (useIPv4) {
                              if (isIPv4) 
                                  theIPAddress = sAddr;
                          } else {
                              if (!isIPv4) {
                                  final int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                  theIPAddress = delim<0 ? sAddr : sAddr.substring(0, delim);
                              }
                          }
                      }
                  }
              }
          return theIPAddress;
      }

    private static final Pattern IPV4_PATTERN = 
        Pattern.compile("^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");
    
    public static boolean isIPv4Address(final String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }
    
    public static ActivityStatus getStatus(final String activityName, final Context context) {
        
        final Set<String> launchers;
        
        synchronized(homeLaunchers) {
            launchers = new HashSet<String>(homeLaunchers);
        }
        
        return getStatus(activityName, context, launchers);
    }

}
