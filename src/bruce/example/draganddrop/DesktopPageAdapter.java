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
import bruce.common.utils.CommonUtils;

public class DesktopPageAdapter extends PagerAdapter {
	private final DesktopActivity activity;
	private final List<View> pageViews = new ArrayList<View>();
	
	public DesktopPageAdapter(DesktopActivity act) {
		activity = act;
		OnTouchListener tl = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return event.getAction() == MotionEvent.ACTION_MOVE;
			}
		};
		List<List<DesktopItem>> pages = CommonUtils.partitionAll(act.pageSize, activity.loadDesktopItems());
		for (int i = 0; i < pages.size(); i++) {
			View desktopView = act.getLayoutInflater().inflate(R.layout.desktop_page, null);
			
			View switchLeftListenerView = desktopView.findViewById(R.id.switch_left_listener);
			switchLeftListenerView.setOnDragListener(act.dragEventListener);
			switchLeftListenerView.setTag(R.id.pos_extra, i - 1);
			
			GridView iconGrid = (GridView) desktopView.findViewById(R.id.app_grid);
			iconGrid.setOnTouchListener(tl); // disable GridView scrolling
			iconGrid.setAdapter(new GridAdapter(act, pages.get(i)));
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

	private GridAdapter getCurrentPageAdapter() {
		return getAdapterByIndex(activity.desktopPager.getCurrentItem());
	}

	private GridAdapter getAdapterByIndex(int currentPageIndex) {
		GridView iconView = (GridView) pageViews.get(currentPageIndex).getTag(R.id.view_holder1);
		return (GridAdapter) iconView.getAdapter();
	}

	GridAdapter startDragAdapter, targetAdapter;
	public void startDrag(int pos) {
		startDragAdapter = getCurrentPageAdapter();
		startDragAdapter.startDrag(pos);
	}

	public void dragTo(View v) {
		targetAdapter = getCurrentPageAdapter();
		if (startDragAdapter == targetAdapter)
			startDragAdapter.dragTo(v);
		else
			startDragAdapter.dragTo(targetAdapter, v);
	}

	public void endDrag() {
		targetAdapter.endDrag();
		if (startDragAdapter != targetAdapter)
			startDragAdapter.endDrag();
		startDragAdapter = targetAdapter = null;
	}

	public void switchPage(int index) {
		Log.d("DragEvent", "switchPage: " + index);
		if (0 <= index && index < pageViews.size()) {
			activity.desktopPager.setCurrentItem(index);
		}
	}
}
