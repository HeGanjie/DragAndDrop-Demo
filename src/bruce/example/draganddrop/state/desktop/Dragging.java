package bruce.example.draganddrop.state.desktop;

import android.view.View;
import bruce.example.draganddrop.DesktopActivity;

public class Dragging extends DesktopState {

	public Dragging(DesktopActivity demoActivity) {
		super(demoActivity);
		//Log.i("DragEvent", "State: Dragging");
	}

	@Override
	public void idle() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void editing() {
		_activity.state = new Editing(_activity);
		_activity.pagerAdapter.endDrag();
	}

	@Override
	public void dragging(View v) {
		throw new UnsupportedOperationException();
	}

}
