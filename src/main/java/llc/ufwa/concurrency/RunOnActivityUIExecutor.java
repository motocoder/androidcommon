package llc.ufwa.concurrency;

import java.util.concurrent.Executor;

import android.app.Activity;

public class RunOnActivityUIExecutor implements Executor {

	private final Activity activity;
	
	public RunOnActivityUIExecutor(Activity activity) {
		this.activity = activity;
	}
	@Override
	public void execute(Runnable command) {
		activity.runOnUiThread(command);		
	}

}
