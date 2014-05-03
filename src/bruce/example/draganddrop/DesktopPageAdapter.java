package bruce.example.draganddrop;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import bruce.common.utils.CommonUtils;

public class DesktopPageAdapter extends PagerAdapter {
	private final DesktopActivity activity;
	private final List<GridView> pages = new ArrayList<GridView>();
	
	public DesktopPageAdapter(DesktopActivity act) {
		activity = act;
		for (List<DesktopItem> items : CommonUtils.partitionAll(DesktopActivity.PAGE_SIZE, activity.loadDesktopItems())) {
			GridView inflate = (GridView) act.getLayoutInflater().inflate(R.layout.desktop_page, null);
			inflate.setAdapter(new GridAdapter(act, items));
			pages.add(inflate);
		}
	}

	@Override
	public void destroyItem(ViewGroup container, int pos, Object object) {
		container.removeView(pages.get(pos));
	}

	@Override
	public Object instantiateItem(ViewGroup container, int pos) {
		View page = pages.get(pos);
		container.addView(page, 0);
		return page;
	}

	@Override
	public int getCount() { return pages.size(); }

	@Override
	public boolean isViewFromObject(View arg0, Object arg1) { return arg0 == arg1; }

	private GridAdapter getCurrentPageAdapter() {
		return (GridAdapter) pages.get(activity.getCurrentPageIndex()).getAdapter();
	}

	public void startDrag(int pos) {
		getCurrentPageAdapter().startDrag(pos);
	}

	public void dragTo(View v) {
		getCurrentPageAdapter().dragTo(v);
	}

	public void endDrag() {
		getCurrentPageAdapter().endDrag();
	}
}
