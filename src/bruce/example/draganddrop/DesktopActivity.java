package bruce.example.draganddrop;

import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import bruce.common.functional.Func1;
import bruce.common.functional.LambdaUtils;
import bruce.common.functional.PersistentVector;
import bruce.common.utils.CommonUtils;
import bruce.example.draganddrop.state.desktop.DesktopState;
import bruce.example.draganddrop.state.desktop.Idle;

public class DesktopActivity extends Activity {
	private static final String APP_SEQUENCES = "appSequences";
	public final int pageSize = 16;
	public DesktopState state;
	public ViewPager mainPager, shortcutPager;
	public IconDragEventListener dragEventListener;
	public final OnLongClickListener startDragListener = new OnLongClickListener() {
		@Override
		public boolean onLongClick(View iconView) {
			if (state instanceof Idle)
				state.editing();
			state.dragging(iconView);
			return true;
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.desktop_layout);
		state = new Idle(this);
		dragEventListener = new IconDragEventListener(this);
		mainPager = (ViewPager) findViewById(R.id.main_pager);
		mainPager.setAdapter(new DesktopPagerAdapter(DesktopActivity.this, splitItems(pageSize, loadDesktopItems()), "main"));
		
		shortcutPager = (ViewPager) findViewById(R.id.shortcut_pager);
		shortcutPager.setAdapter(new DesktopPagerAdapter(DesktopActivity.this, splitItems(5, makeSlots(6)), "shortcut"));
	}
	
	private List<DesktopItem> makeSlots(int count) {
		return LambdaUtils.select(CommonUtils.range(0, count), new Func1<DesktopItem, Integer>() {
			@Override
			public DesktopItem call(Integer arg0) { return DesktopItem.SLOT; }
		});
	}

	private PersistentVector<PersistentVector<DesktopItem>> splitItems(int pageSize, List<DesktopItem> items) {
		while (items.size() % pageSize != 0) {
			items.add(DesktopItem.SLOT);
		}
		return Utils.partitionAll(pageSize, items);
	}

	private int getPageSize() {
		float itemWidth = Utils.dp2Px(60, this);
		float itemHeight = Utils.dp2Px(95, this);
		float horizontalSpacing = Utils.dp2Px(30, this);
		float verticalSpacing = Utils.dp2Px(20, this);
		View decorView = getWindow().getDecorView();
		int activityWidth = decorView.getWidth();
		int activityHeight = decorView.getHeight();

		int columnCount = (int) Math.floor(activityWidth / (itemWidth + horizontalSpacing));
		int rowCount = (int) Math.floor(activityHeight / (itemHeight + verticalSpacing));
		return columnCount * rowCount;
	}

	public List<DesktopItem> loadDesktopItems() {
		String appSequences = getPreferences(MODE_PRIVATE).getString(APP_SEQUENCES, "");
		final PackageManager pm = getPackageManager();
		List<DesktopItem> items = LambdaUtils.select(getAppInfos(), new Func1<DesktopItem, ResolveInfo>() {
			@Override
			public DesktopItem call(ResolveInfo info) {
				return new DesktopItem(info.loadIcon(pm), info.loadLabel(pm).toString(), info.activityInfo.packageName);
			}
		});
		if (CommonUtils.isStringNullOrWriteSpace(appSequences)) {
			return items;
		} else {
			final Map<String, DesktopItem> map = LambdaUtils.toMap(items, DesktopItem.pkgNameSelector);
			return LambdaUtils.select(CommonUtils.parseList(appSequences), new Func1<DesktopItem, String>() {
				@Override
				public DesktopItem call(String pkgName) { return DesktopItem.fromPkgName(map, pkgName); }
			});
		}
	}

	public void saveDesktopItems(List<DesktopItem> allItems) {
		Editor edit = getPreferences(MODE_PRIVATE).edit();
		edit.putString(APP_SEQUENCES, LambdaUtils.select(allItems, DesktopItem.pkgNameSelector).toString());
		edit.commit();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && !(state instanceof Idle)) {
			state.idle();
			return true;
        }
		return super.onKeyDown(keyCode, event);
	}

	private List<ResolveInfo> getAppInfos() {
		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER);
		return getPackageManager().queryIntentActivities(mainIntent, 0);
	}

	public ViewPager getViewPagerByGroupName(String groupName) {
		if ("main".equals(groupName)) {
			return mainPager;
		} else if ("shortcut".equals(groupName)) {
			return shortcutPager;
		}
		throw new IllegalStateException();
	}
	
	public DesktopPagerAdapter getPagerAdapter(View v) {
		return (DesktopPagerAdapter) getViewPagerByGroupName((String) v.getTag(R.id.group_name)).getAdapter();
	}

	public int getViewPagerLayout(String groupName) {
		if ("main".equals(groupName)) {
			return R.layout.desktop_page;
		} else if ("shortcut".equals(groupName)) {
			return R.layout.shortcut_page;
		}
		throw new IllegalStateException();
	}
}
