package bruce.example.draganddrop;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.Toast;
import bruce.common.functional.PersistentVector;

public class DesktopPageAdapter extends PagerAdapter {
	private final DesktopActivity activity;
	private final List<View> pageViews = new ArrayList<View>();
	private PersistentVector<PersistentVector<DesktopItem>> pageItems;
	
	public DesktopPageAdapter(DesktopActivity act) {
		activity = act;
		OnTouchListener tl = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return event.getAction() == MotionEvent.ACTION_MOVE;
			}
		};
		pageItems = Utils.partitionAll(act.pageSize, activity.loadDesktopItems());
		for (int i = 0; i < pageItems.size(); i++) {
			View desktopView = act.getLayoutInflater().inflate(R.layout.desktop_page, null);
			
			View switchLeftListenerView = desktopView.findViewById(R.id.switch_left_listener);
			switchLeftListenerView.setOnDragListener(act.dragEventListener);
			switchLeftListenerView.setTag(R.id.pos_extra, i - 1);
			
			GridView iconGrid = (GridView) desktopView.findViewById(R.id.app_grid);
			iconGrid.setOnTouchListener(tl); // disable GridView scrolling
			iconGrid.setAdapter(new GridAdapter(act, pageItems.get(i)));
			desktopView.setTag(R.id.view_holder1, iconGrid);
			
			View switchRightListenerView = desktopView.findViewById(R.id.switch_right_listener);
			switchRightListenerView.setOnDragListener(act.dragEventListener);
			switchRightListenerView.setTag(R.id.pos_extra, i + 1);
			
			pageViews.add(desktopView);
		}
	}

	@Override
	public void destroyItem(ViewGroup container, int pos, Object object) {
		container.removeView(pageViews.get(pos));
	}

	@Override
	public Object instantiateItem(ViewGroup container, int pos) {
		View page = pageViews.get(pos);
		container.addView(page, 0);
		return page;
	}

	@Override
	public int getCount() { return pageViews.size(); }

	@Override
	public boolean isViewFromObject(View arg0, Object arg1) { return arg0 == arg1; }

	private int getDesktopPageIndex() { return activity.desktopPager.getCurrentItem(); }

	private GridAdapter getAdapterByIndex(int currentPageIndex) {
		GridView iconView = (GridView) pageViews.get(currentPageIndex).getTag(R.id.view_holder1);
		return (GridAdapter) iconView.getAdapter();
	}

	private int startDragAtPageIndex = -1, draggingItemPos = -1, modifiedPageIndex = -1, draggingToPos = -1;
	
	public void startDrag(int pos) {
		startDragAtPageIndex = getDesktopPageIndex();
		draggingItemPos = pos;
		modifiedPageIndex = -1;
		draggingToPos = -1;
		setPageItems(startDragAtPageIndex, pageItems.get(startDragAtPageIndex).assoc(draggingItemPos, DesktopItem.SLOT));
	}

	public void dragTo(View v) {
		resetToDragStarted();
		int draggingToPageIndex = getDesktopPageIndex();
		int pos = (Integer) v.getTag(R.id.pos_extra);
		if (draggingToPageIndex == startDragAtPageIndex && pos == draggingItemPos) { // same item
			return;
		}
		PersistentVector<DesktopItem> itemsOfTargetPage = getAdapterByIndex(draggingToPageIndex).getItems();
		switch (v.getId()) {
		case R.id.slot_view:
			setPageItems(draggingToPageIndex, itemsOfTargetPage.assoc(pos, DesktopItem.SLOT)); // keep dragging view as a SLOT until ending drag
			modifiedPageIndex = draggingToPageIndex;
			draggingToPos = pos;
			break;
		case R.id.app_icon:
		case R.id.folder_grid:
			int nearestSlotPos = getNearestSlotPos(pos, itemsOfTargetPage);
			if (nearestSlotPos == -1)
				Toast.makeText(activity, "This page is full", Toast.LENGTH_SHORT).show();
			else {
				setPageItems(draggingToPageIndex, itemsOfTargetPage.disjAt(nearestSlotPos).conj(pos, DesktopItem.SLOT));
				modifiedPageIndex = draggingToPageIndex;
			draggingToPos = pos;
			}
			break;
		case R.id.trigger_to_folder:
			String defaultFolderName = activity.getString(R.string.grid_item_folder_unnamed);
			setPageItems(draggingToPageIndex, itemsOfTargetPage.assoc(pos, itemsOfTargetPage.get(pos).merge(defaultFolderName, getDraggingItem())));
			modifiedPageIndex = draggingToPageIndex;
			draggingToPos = pos;
			break;
		default:
			break;
		}
	}

	private void resetToDragStarted() {
		if (modifiedPageIndex == -1) {
			return;
		} else if (modifiedPageIndex == startDragAtPageIndex) {
			setPageItems(startDragAtPageIndex, pageItems.get(startDragAtPageIndex).assoc(draggingItemPos, DesktopItem.SLOT));
			modifiedPageIndex = -1;
		} else {
			setPageItems(modifiedPageIndex, pageItems.get(modifiedPageIndex));
			modifiedPageIndex = -1;
		}
	}

	public void endDrag() {
		if (modifiedPageIndex == -1) { // reset to before drag
			setPageItems(startDragAtPageIndex, pageItems.get(startDragAtPageIndex));
		} else if (modifiedPageIndex == startDragAtPageIndex) { // settle dragging view
			PersistentVector<DesktopItem> updated = getAdapterByIndex(modifiedPageIndex).getItems().assoc(draggingToPos, getDraggingItem());
			setPageItems(modifiedPageIndex, updated);
			pageItems = pageItems.assoc(startDragAtPageIndex, updated);
		} else {
			PersistentVector<DesktopItem> updated = getAdapterByIndex(modifiedPageIndex).getItems().assoc(draggingToPos, getDraggingItem());
			setPageItems(modifiedPageIndex, updated);
			pageItems = pageItems
					.assoc(startDragAtPageIndex, getAdapterByIndex(startDragAtPageIndex).getItems())
					.assoc(modifiedPageIndex, updated);
		}
		startDragAtPageIndex = -1;
		draggingItemPos = -1;
		modifiedPageIndex = -1;
		draggingToPos = -1;
	}

	private DesktopItem getDraggingItem() { return pageItems.get(startDragAtPageIndex).get(draggingItemPos); }

	private void setPageItems(int pageIndex, PersistentVector<DesktopItem> items) {
		getAdapterByIndex(pageIndex).updateItems(items);
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
	
	public void switchPage(int index) {
		Log.d("DragEvent", "switchPage: " + index);
		if (0 <= index && index < pageViews.size()) {
			activity.desktopPager.setCurrentItem(index);
		}
	}
}
