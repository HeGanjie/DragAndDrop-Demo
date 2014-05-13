package bruce.example.draganddrop.state.desktop;

import android.view.View;
import android.view.View.DragShadowBuilder;
import bruce.example.draganddrop.DesktopActivity;

public class Editing extends DesktopState {

	public Editing(DesktopActivity demoActivity) {
		super(demoActivity);
		//Log.i("DragEvent", "State: Editing");
	}

	@Override
	public void idle() {
		_activity.state = new Idle(_activity);
	}

	@Override
	public void editing() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void dragging(View v) {
		_activity.state = new Dragging(_activity);
		v.startDrag(null, new DragShadowBuilder(v), null, 0);
		_activity.dragEventListener.startDrag(v);
		//_activity.pagerAdapter.startDrag((Integer) v.getTag(R.id.pos_extra));
	}

}
