package bruce.example.draganddrop;

import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.widget.TextView;
import bruce.example.draganddrop.state.desktop.Editing;

public class IconDragEventListener implements OnDragListener {
	private DesktopActivity activity;

	public IconDragEventListener(DesktopActivity desktopActivity) {
		activity = desktopActivity;
	}

	@Override
	public boolean onDrag(View v, DragEvent e) {
		switch (e.getAction()) {
		case DragEvent.ACTION_DRAG_STARTED:
			return true;
		case DragEvent.ACTION_DRAG_ENTERED:
			v.setTag(0);
			return true;
		case DragEvent.ACTION_DRAG_LOCATION:
			int tag = (Integer) v.getTag();
			if (tag < 25) {
				v.setTag(tag + 1);
			} else {
				v.setTag(0);
				activity.pagerAdapter.dragTo(v);
			}
			return true;
		case DragEvent.ACTION_DRAG_EXITED:
			v.setTag(0);
			return true;
		case DragEvent.ACTION_DROP:
			activity.pagerAdapter.dragTo(v);
			return true;
		case DragEvent.ACTION_DRAG_ENDED:
			if (!(activity.state instanceof Editing)) {
				activity.state.editing();
			}
			return true;
		default:
			Log.e("DragEvent", "Unknown action type received by OnDragListener.");
			return false;
		}
	}

	private String trace(View view) {
		View parent = (View) view.getParent().getParent();
		return ((TextView) parent.getTag(R.id.view_holder2)).getText().toString();
	}
}
