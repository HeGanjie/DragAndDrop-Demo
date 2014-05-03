package bruce.example.draganddrop.state.desktop;

import android.view.View;
import bruce.example.draganddrop.DesktopActivity;

public abstract class DesktopState {
	protected final DesktopActivity _activity;
	
	public DesktopState(DesktopActivity demoActivity) {
		_activity = demoActivity;
	}
	
	public abstract void idle();
	public abstract void editing();
	public abstract void dragging(View v);

}
