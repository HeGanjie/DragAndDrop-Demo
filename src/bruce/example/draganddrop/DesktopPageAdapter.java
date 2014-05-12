package bruce.example.draganddrop;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.view.PagerAdapter;
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

	private int dragOutPageIndex = -1, draggingOutPos = -1, dragInPageIndex = -1, draggingToPos = -1;
	private boolean isToDir = false;
	
	public void startDrag(int pos) {
		dragOutPageIndex = getDesktopPageIndex();
		draggingOutPos = pos;
		dragInPageIndex = -1;
		draggingToPos = -1;
		isToDir = false;
		setPageItems(dragOutPageIndex, pageItems.get(dragOutPageIndex).assoc(draggingOutPos, DesktopItem.SLOT));
	}

	public void dragTo(View v) {
		int currentPageIndex = getDesktopPageIndex();
		int pos = (Integer) v.getTag(R.id.pos_extra);
		if (currentPageIndex == dragOutPageIndex && pos == draggingOutPos) { // same item
			return;
		} else if (currentPageIndex == dragInPageIndex && pos == draggingToPos) { // already apply
			return;
		}
		resetToDragStarted();
		dragInPageIndex = currentPageIndex;
		draggingToPos = pos;
		PersistentVector<DesktopItem> itemsOfTargetPage = getAdapterByIndex(dragInPageIndex).getItems();
		switch (v.getId()) {
		case R.id.slot_view:
			setPageItems(dragInPageIndex, itemsOfTargetPage.assoc(draggingToPos, DesktopItem.SLOT)); // keep dragging view as a SLOT until ending drag
			break;
		case R.id.app_icon:
		case R.id.folder_grid:
			int nearestSlotPos = getNearestSlotPos(draggingToPos, itemsOfTargetPage);
			if (nearestSlotPos == -1) {
				Toast.makeText(activity, "This page is full", Toast.LENGTH_SHORT).show();
				dragInPageIndex = -1;
				draggingToPos = -1;
			} else { // [_ 1 x 3 4] -> [1 x n 3 4] | [0 1 x 3 _] -> [0 1 n x 3]
				setPageItems(dragInPageIndex, itemsOfTargetPage.disjAt(nearestSlotPos).conj(draggingToPos, DesktopItem.SLOT));
			}
			break;
		case R.id.trigger_to_folder:
			String defaultFolderName = activity.getString(R.string.grid_item_folder_unnamed);
			DesktopItem folder = itemsOfTargetPage.get(draggingToPos).merge(defaultFolderName, getDraggingItem());
			setPageItems(dragInPageIndex, itemsOfTargetPage.assoc(draggingToPos, folder));
			isToDir = true;
			break;
		default:
			break;
		}
	}

	private void resetToDragStarted() {
		if (dragInPageIndex != -1) {
			if (dragInPageIndex == dragOutPageIndex) {
				setPageItems(dragOutPageIndex, pageItems.get(dragOutPageIndex).assoc(draggingOutPos, DesktopItem.SLOT));
			} else {
				setPageItems(dragInPageIndex, pageItems.get(dragInPageIndex));
			}
		}
		dragInPageIndex = -1;
		draggingToPos = -1;
		isToDir = false;
	}

	public void endDrag() {
		if (dragInPageIndex == -1) { // reset to before drag
			setPageItems(dragOutPageIndex, pageItems.get(dragOutPageIndex));
		} else if (dragInPageIndex == dragOutPageIndex) { // settle dragging view
			pageItems = isToDir
					? pageItems.assoc(dragOutPageIndex, getAdapterByIndex(dragOutPageIndex).getItems())
					: pageItems.assoc(dragOutPageIndex, getAdapterByIndex(dragOutPageIndex).getItems().assoc(draggingToPos, getDraggingItem()));
			setPageItems(dragInPageIndex, pageItems.get(dragOutPageIndex));
		} else {
			PersistentVector<DesktopItem> newModifiedPageItems = isToDir
					? getAdapterByIndex(dragInPageIndex).getItems()
					:  getAdapterByIndex(dragInPageIndex).getItems().assoc(draggingToPos, getDraggingItem());
			pageItems = pageItems
					.assoc(dragOutPageIndex, getAdapterByIndex(dragOutPageIndex).getItems())
					.assoc(dragInPageIndex, newModifiedPageItems);
			setPageItems(dragInPageIndex, pageItems.get(dragInPageIndex));
		}
		dragOutPageIndex = -1;
		draggingOutPos = -1;
		dragInPageIndex = -1;
		draggingToPos = -1;
		isToDir = false;
	}

	private DesktopItem getDraggingItem() { return pageItems.get(dragOutPageIndex).get(draggingOutPos); }

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
		if (0 <= index && index < pageViews.size()) {
			activity.desktopPager.setCurrentItem(index);
		}
	}
}
