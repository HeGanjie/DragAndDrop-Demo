package bruce.example.draganddrop;

import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.widget.Toast;
import bruce.common.functional.PersistentVector;
import bruce.example.draganddrop.state.desktop.Editing;

public class IconDragEventListener implements OnDragListener {
	private DesktopActivity activity;

	public IconDragEventListener(DesktopActivity desktopActivity) { activity = desktopActivity; }

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
			if (tag < 25)
				v.setTag(tag + 1);
			else {
				v.setTag(0);
				if (v.getId() != R.id.switch_left_listener && v.getId() != R.id.switch_right_listener)
					dragTo(v);
				else
					switchPage(v);
			}
			return true;
		case DragEvent.ACTION_DRAG_EXITED:
			v.setTag(0);
			return true;
		case DragEvent.ACTION_DROP:
			if (v.getId() != R.id.switch_left_listener && v.getId() != R.id.switch_right_listener)
				dragTo(v);
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
	
	private DesktopPagerAdapter outCtrl, inCtrl;
	private int outPageIndex = -1, outPos = -1, inPageIndex = -1, inPos = -1;
	private boolean isInDir = false;

	private int getPageIndex(View v) {
		return (Integer) v.getTag(R.id.index_in_group);
	}

	private int getItemIndex(View v) {
		return (Integer) v.getTag(R.id.pos_extra);
	}

	public void startDrag(View v) {
		outCtrl = activity.getPagerAdapter(v);
		outPageIndex = getPageIndex(v);
		outPos = getItemIndex(v);
		inCtrl = null;
		inPageIndex = -1;
		inPos = -1;
		isInDir = false;
		outCtrl.setPageItem(outPageIndex, outPos, DesktopItem.SLOT);
	}

	public void dragTo(View v) {
		DesktopPagerAdapter adapter = activity.getPagerAdapter(v);
		int currentPageIndex = getPageIndex(v);
		int pos = (Integer) v.getTag(R.id.pos_extra);
		if (adapter == outCtrl
				&& currentPageIndex == outPageIndex
				&& pos == outPos) { // same item
			return;
		} else if (adapter == outCtrl
				&& currentPageIndex == inPageIndex
				&& pos == inPos) { // already apply
			return;
		}
		resetToDragStarted();
		inCtrl = adapter;
		inPageIndex = currentPageIndex;
		inPos = pos;
		PersistentVector<DesktopItem> inPageItems = inCtrl.getPageItems(inPageIndex);
		switch (v.getId()) {
		case R.id.slot_view:
			inCtrl.setPageItem(inPageIndex, inPos, DesktopItem.SLOT); // keep dragging view as a SLOT until ending drag
			break;
		case R.id.app_icon:
		case R.id.folder_grid:
			int nearestSlotPos = getNearestSlotPos(inPos, inPageItems);
			if (nearestSlotPos == -1) {
				Toast.makeText(activity, "This page is full", Toast.LENGTH_SHORT).show();
				inCtrl = null;
				inPageIndex = -1;
				inPos = -1;
			} else { // [_ 1 x 3 4] -> [1 x n 3 4] | [0 1 x 3 _] -> [0 1 n x 3]
				inCtrl.setPageItems(inPageIndex, inPageItems.disjAt(nearestSlotPos).conj(inPos, DesktopItem.SLOT));
			}
			break;
		case R.id.trigger_to_folder:
			String defaultFolderName = activity.getString(R.string.grid_item_folder_unnamed);
			DesktopItem folder = inPageItems.get(inPos).merge(defaultFolderName, getDraggingItem());
			inCtrl.setPageItem(inPageIndex, inPos, folder);
			isInDir = true;
			break;
		default:
			break;
		}
	}

	private DesktopItem getDraggingItem() {
		return outCtrl.bakPageItems.get(outPageIndex).get(outPos);
	}

	private void resetToDragStarted() {
		if (inPageIndex != -1) {
			if (outCtrl == inCtrl && inPageIndex == outPageIndex) {
				outCtrl.reloadAndSetPageItem(outPageIndex, outPos, DesktopItem.SLOT);
			} else {
				inCtrl.reloadPage(inPageIndex);
			}
		}
		inCtrl = null;
		inPageIndex = -1;
		inPos = -1;
		isInDir = false;
	}

	public void endDrag() {
		if (inCtrl == null) { // reset to before drag
			outCtrl.reloadPage(outPageIndex);
		} else {
			if (!isInDir) // settle dragging view
				inCtrl.setPageItem(inPageIndex, inPos, getDraggingItem());
			inCtrl.savePage(inPageIndex);
			if (inCtrl != outCtrl || inPageIndex != outPageIndex)
				outCtrl.savePage(outPageIndex);
		}
		inCtrl = null;
		outCtrl = null;
		outPageIndex = -1;
		outPos = -1;
		inPageIndex = -1;
		inPos = -1;
		isInDir = false;
	}

	private int getNearestSlotPos(int offset, PersistentVector<DesktopItem> items) {
		int forthSlotIndex = -1, backSlotIndex = -1;
		for (int i = offset + 1; i < items.size(); i++) {
			if (items.get(i) == DesktopItem.SLOT) {
				forthSlotIndex = i;
				break;
			}
		}
		for (int i = offset - 1; 0 <= i; i--) {
			if (items.get(i) == DesktopItem.SLOT) {
				backSlotIndex = i;
				break;
			}
		}
		if (forthSlotIndex != -1 && backSlotIndex != -1) {
			return forthSlotIndex - offset <= offset - backSlotIndex ? forthSlotIndex : backSlotIndex;
		} else
			return backSlotIndex == -1 ? forthSlotIndex : backSlotIndex;
	}
	
	public void switchPage(View v) {
		activity.getPagerAdapter(v).switchPage((Integer) v.getTag(R.id.pos_extra));
	}
}
