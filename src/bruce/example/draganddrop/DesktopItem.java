package bruce.example.draganddrop;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import android.graphics.drawable.Drawable;
import bruce.common.functional.Func1;
import bruce.common.functional.LambdaUtils;
import bruce.common.functional.PersistentVector;
import bruce.common.utils.CommonUtils;

public final class DesktopItem {
	private static final String PKG_NAME_SLOT = "slot";
	static final Func1<String, DesktopItem> pkgNameSelector = new Func1<String, DesktopItem>() {
		@Override
		public String call(DesktopItem t) { return t.pkgName; }
	};
	public static final DesktopItem SLOT = new DesktopItem(null, null, PKG_NAME_SLOT, null, true);
	final Drawable icon;
	final String label, pkgName;
	final PersistentVector<DesktopItem> folderItems;
	final boolean visible;
	
	public DesktopItem(Drawable drawable, String appName, String packageName) {
		icon = drawable;
		label = appName;
		pkgName = packageName;
		folderItems = null;
		visible = true;
	}
	
	public DesktopItem(String folderName, PersistentVector<DesktopItem> foldedItems) {
		icon = null;
		label = folderName;
		pkgName = CommonUtils.displayArray(LambdaUtils.select(foldedItems, pkgNameSelector).toArray(), ";");
		folderItems = foldedItems;
		visible = true;
	}
	
	private DesktopItem(Drawable ic, String name, String pkg, PersistentVector<DesktopItem> innerItems, boolean isVisible) {
		icon = ic;
		label = name;
		pkgName = pkg;
		folderItems = innerItems;
		visible = isVisible;
	}

	@Override
	public String toString() {
		if (this == SLOT) {
			return PKG_NAME_SLOT;
		} else if (isItem()) {
			return pkgName;
		} else {
			return CommonUtils.buildString(pkgName, ';', pkgName);
		}
	}

	public boolean isItem() {
		return folderItems == null;
	}

	public DesktopItem hide() {
		return new DesktopItem(icon, label, pkgName, folderItems, false);
	}

	public DesktopItem show() {
		return new DesktopItem(icon, label, pkgName, folderItems, true);
	}

	public static DesktopItem fromPkgName(final Map<String, DesktopItem> map, String pkgName) {
		if (PKG_NAME_SLOT.equals(pkgName)) {
			return SLOT;
		} else if (pkgName.indexOf(';') == -1) {
			DesktopItem desktopItem = map.get(pkgName);
			return desktopItem == null ? SLOT : desktopItem;
		} else {
			List<String> pkgNames = Arrays.asList(pkgName.split(";"));
			List<DesktopItem> select = LambdaUtils.select(pkgNames.subList(1, pkgNames.size()), new Func1<DesktopItem, String>() {
				@Override
				public DesktopItem call(String t) { return fromPkgName(map, t); }
			});
			return new DesktopItem(pkgNames.get(0), new PersistentVector<DesktopItem>(LambdaUtils.where(select, new Func1<Boolean, DesktopItem>() {
				@Override
				public Boolean call(DesktopItem t) { return t != DesktopItem.SLOT; }
			})));
		}
	}

	public DesktopItem merge(String defaultFolderName, DesktopItem mergingItem) {
		if (!isItem() && !mergingItem.isItem()) { // folder + folder
			return new DesktopItem(defaultFolderName, folderItems.merge(mergingItem.folderItems));
		} else if (!isItem() && mergingItem.isItem()) { // item + folder | folder + item
			return new DesktopItem(defaultFolderName, folderItems.conj(mergingItem));
		} else if (isItem() && !mergingItem.isItem()) {
			return mergingItem.merge(defaultFolderName, this);
		} else { // item + item
			return new DesktopItem(defaultFolderName, new PersistentVector<DesktopItem>(this, mergingItem));
		}
	}
}
