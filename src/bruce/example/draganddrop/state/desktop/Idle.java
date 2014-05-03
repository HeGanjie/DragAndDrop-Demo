package bruce.example.draganddrop.state.desktop;

import android.view.View;
import bruce.example.draganddrop.DesktopActivity;

public class Idle extends DesktopState {

	public Idle(DesktopActivity demoActivity) {
		super(demoActivity);
		//Log.i("DragEvent", "State: Idle");
	}

	@Override
	public void idle() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void editing() {
		// TODO Display Uninstall Button, 
		_activity.state = new Editing(_activity);
	}

	@Override
	public void dragging(View v) {
		throw new UnsupportedOperationException();
	}

}
