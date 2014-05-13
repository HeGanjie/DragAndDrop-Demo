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
	
	private DesktopPageAdapter draggingOutCtrl, draggingInCtrl;
	private int draggingOutPageIndex = -1, draggingOutPos = -1, dragInPageIndex = -1, draggingToPos = -1;
	private boolean isToDir = false;

	private int getPageIndexByItemView(View v) {
		return (Integer) v.getTag(R.id.index_in_group);
	}

	private int getItemIndex(View v) {
		return (Integer) v.getTag(R.id.pos_extra);
	}

	public void startDrag(View v) {
		draggingOutCtrl = activity.getPagerAdapterByItemView(v);
		draggingOutPageIndex = getPageIndexByItemView(v);
		draggingOutPos = getItemIndex(v);
		draggingInCtrl = null;
		dragInPageIndex = -1;
		draggingToPos = -1;
		isToDir = false;
		draggingOutCtrl.setPageItems(draggingOutPageIndex, draggingOutPos, DesktopItem.SLOT);
	}

	public void dragTo(View v) {
		DesktopPageAdapter adapter = activity.getPagerAdapterByItemView(v);
		int currentPageIndex = getPageIndexByItemView(v);
		int pos = (Integer) v.getTag(R.id.pos_extra);
		if (adapter == draggingOutCtrl
				&& currentPageIndex == draggingOutPageIndex
				&& pos == draggingOutPos) { // same item
			return;
		} else if (adapter == draggingOutCtrl
				&& currentPageIndex == dragInPageIndex
				&& pos == draggingToPos) { // already apply
			return;
		}
		resetToDragStarted();
		draggingInCtrl = adapter;
		dragInPageIndex = currentPageIndex;
		draggingToPos = pos;
		PersistentVector<DesktopItem> itemsOfTargetPage = draggingInCtrl.getGridAdapterByIndex(dragInPageIndex).getItems();
		switch (v.getId()) {
		case R.id.slot_view:
			draggingInCtrl.setPageItems(dragInPageIndex, itemsOfTargetPage.assoc(draggingToPos, DesktopItem.SLOT)); // keep dragging view as a SLOT until ending drag
			break;
		case R.id.app_icon:
		case R.id.folder_grid:
			int nearestSlotPos = getNearestSlotPos(draggingToPos, itemsOfTargetPage);
			if (nearestSlotPos == -1) {
				Toast.makeText(activity, "This page is full", Toast.LENGTH_SHORT).show();
				dragInPageIndex = -1;
				draggingToPos = -1;
			} else { // [_ 1 x 3 4] -> [1 x n 3 4] | [0 1 x 3 _] -> [0 1 n x 3]
				draggingInCtrl.setPageItems(dragInPageIndex, itemsOfTargetPage.disjAt(nearestSlotPos).conj(draggingToPos, DesktopItem.SLOT));
			}
			break;
		case R.id.trigger_to_folder:
			String defaultFolderName = activity.getString(R.string.grid_item_folder_unnamed);
			DesktopItem folder = itemsOfTargetPage.get(draggingToPos).merge(defaultFolderName, getDraggingItem());
			draggingInCtrl.setPageItems(dragInPageIndex, itemsOfTargetPage.assoc(draggingToPos, folder));
			isToDir = true;
			break;
		default:
			break;
		}
	}

	private DesktopItem getDraggingItem() {
		return draggingOutCtrl.getDesktopItem(draggingOutPageIndex, draggingOutPos);
	}

	private void resetToDragStarted() {
		if (dragInPageIndex != -1) {
			if (dragInPageIndex == draggingOutPageIndex) {
				draggingOutCtrl.setPageItems(draggingOutPageIndex, draggingOutPos, DesktopItem.SLOT);
			} else {
				draggingInCtrl.setPageItems(dragInPageIndex, draggingInCtrl.pageItems.get(dragInPageIndex));
			}
		}
		dragInPageIndex = -1;
		draggingToPos = -1;
		isToDir = false;
	}

	public void endDrag() {
		if (draggingInCtrl == null) { // reset to before drag
			draggingOutCtrl.setPageItems(draggingOutPageIndex, draggingOutCtrl.pageItems.get(draggingOutPageIndex));
		} else if (draggingInCtrl == draggingOutCtrl && dragInPageIndex == draggingOutPageIndex) { // settle dragging view
			PersistentVector<PersistentVector<DesktopItem>> originalPageItems = draggingInCtrl.pageItems;
			PersistentVector<DesktopItem> modifiedItems = draggingInCtrl.getGridAdapterByIndex(dragInPageIndex).getItems();
			draggingInCtrl.pageItems = isToDir
					? originalPageItems.assoc(dragInPageIndex, modifiedItems)
					: originalPageItems.assoc(dragInPageIndex, modifiedItems.assoc(draggingToPos, getDraggingItem()));
			draggingInCtrl.reload(dragInPageIndex);
		} else if (draggingInCtrl == draggingOutCtrl && dragInPageIndex != draggingOutPageIndex) {
			PersistentVector<DesktopItem> newModifiedPageItems = isToDir
					? draggingInCtrl.getGridAdapterByIndex(dragInPageIndex).getItems()
					: draggingInCtrl.getGridAdapterByIndex(dragInPageIndex).getItems().assoc(draggingToPos, getDraggingItem());
			draggingInCtrl.pageItems = draggingInCtrl.pageItems
					.assoc(draggingOutPageIndex, draggingOutCtrl.getGridAdapterByIndex(draggingOutPageIndex).getItems())
					.assoc(dragInPageIndex, newModifiedPageItems);
			draggingInCtrl.reload(dragInPageIndex);
		} else {
			
		}
		draggingInCtrl = null;
		draggingOutCtrl = null;
		draggingOutPageIndex = -1;
		draggingOutPos = -1;
		dragInPageIndex = -1;
		draggingToPos = -1;
		isToDir = false;
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
		activity.getPagerAdapterByItemView(v).switchPage((Integer) v.getTag(R.id.pos_extra));
	}
}
