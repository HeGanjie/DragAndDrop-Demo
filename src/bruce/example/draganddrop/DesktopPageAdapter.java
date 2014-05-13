package bruce.example.draganddrop;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.view.PagerAdapter;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.GridView;
import bruce.common.functional.PersistentVector;

public class DesktopPageAdapter extends PagerAdapter {
	private final DesktopActivity activity;
	private final List<View> pageViews = new ArrayList<View>();
	public PersistentVector<PersistentVector<DesktopItem>> pageItems;
	private final String groupName;
	
	public DesktopPageAdapter(DesktopActivity act, int pageSize, List<DesktopItem> allItems, String groupName) {
		activity = act;
		this.groupName = groupName;
		OnTouchListener tl = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return event.getAction() == MotionEvent.ACTION_MOVE;
			}
		};
		pageItems = Utils.partitionAll(pageSize, allItems);
		for (int i = 0; i < pageItems.size(); i++) {
			View desktopView = act.getLayoutInflater().inflate(R.layout.desktop_page, null);
			
			View switchLeftListenerView = desktopView.findViewById(R.id.switch_left_listener);
			switchLeftListenerView.setOnDragListener(act.dragEventListener);
			switchLeftListenerView.setTag(R.id.pos_extra, i - 1);
			switchLeftListenerView.setTag(R.id.group_name, groupName);
			
			GridView iconGrid = (GridView) desktopView.findViewById(R.id.app_grid);
			iconGrid.setOnTouchListener(tl); // disable GridView scrolling
			iconGrid.setAdapter(new GridAdapter(act, pageItems.get(i), groupName, i));
			desktopView.setTag(R.id.view_holder1, iconGrid);
			
			View switchRightListenerView = desktopView.findViewById(R.id.switch_right_listener);
			switchRightListenerView.setOnDragListener(act.dragEventListener);
			switchRightListenerView.setTag(R.id.pos_extra, i + 1);
			switchRightListenerView.setTag(R.id.group_name, groupName);
			
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

	public GridAdapter getGridAdapterByIndex(int currentPageIndex) {
		GridView iconView = (GridView) pageViews.get(currentPageIndex).getTag(R.id.view_holder1);
		return (GridAdapter) iconView.getAdapter();
	}

	public DesktopItem getDesktopItem(int pageIndex, int itemPos) {
		return pageItems.get(pageIndex).get(itemPos);
	}

	public void setPageItems(int pageIndex, int itemPos, DesktopItem newItem) {
		setPageItems(pageIndex, pageItems.get(pageIndex).assoc(itemPos, newItem));
	}
	
	public void setPageItems(int pageIndex, PersistentVector<DesktopItem> items) {
		getGridAdapterByIndex(pageIndex).updateItems(items);
	}
	
	public void switchPage(int index) {
		if (0 <= index && index < pageViews.size()) {
			activity.getViewPagerByGroupName(groupName).setCurrentItem(index);
		}
	}

	public void reload(int pageIndex) {
		setPageItems(pageIndex, pageItems.get(pageIndex));
	}
}
