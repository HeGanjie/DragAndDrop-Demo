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
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import bruce.common.functional.Func1;
import bruce.common.functional.LambdaUtils;
import bruce.common.utils.CommonUtils;
import bruce.example.draganddrop.state.desktop.DesktopState;
import bruce.example.draganddrop.state.desktop.Idle;

public class DesktopActivity extends Activity {
	private static final String APP_SEQUENCES = "appSequences";
	public int pageSize;
	public DesktopState state;
	private ViewPager desktopPager;
	public DesktopPageAdapter pagerAdapter;
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
		state = new Idle(this);
		dragEventListener = new IconDragEventListener(this);
		desktopPager = new ViewPager(this);
		setContentView(desktopPager);
		desktopPager.post(new Runnable() {
			@Override
			public void run() {
				pageSize = getPageSize();
				if (pageSize <= 0)
					pageSize = 20;
				Log.d("DemoDebug", "PageSize:" + pageSize);
				desktopPager.setAdapter(pagerAdapter = new DesktopPageAdapter(DesktopActivity.this));
			}
		});
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
			while (items.size() % pageSize != 0) {
				items.add(DesktopItem.SLOT);
			}
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

	public int getCurrentPageIndex() {
		return desktopPager.getCurrentItem();
	}
}
