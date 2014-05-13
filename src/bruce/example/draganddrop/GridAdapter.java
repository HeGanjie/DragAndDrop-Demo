package bruce.example.draganddrop;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import bruce.common.functional.PersistentVector;

public final class GridAdapter extends BaseAdapter {
	private static final int VIEW_TYPE_SLOT = 0;
	private static final int VIEW_TYPE_ITEM = 1;
	private static final int VIEW_TYPE_FOLDER = 2;
	private final LayoutInflater li;
	private final IconDragEventListener dragEventListener;
	private final OnLongClickListener startDragListener;
	private final String groupName;
	private final int indexInGroup;
	
	private PersistentVector<DesktopItem> items;
	
	public PersistentVector<DesktopItem> getItems() { return items; }
	
	public void updateItems(PersistentVector<DesktopItem> newItems) {
		items = newItems;
		notifyDataSetChanged();
	}
	
	public GridAdapter(DesktopActivity ctx, PersistentVector<DesktopItem> persistentVector, String groupName, int indexInGroup) {
		startDragListener = ctx.startDragListener;
		dragEventListener = ctx.dragEventListener;
		li = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		items = persistentVector;
		this.groupName = groupName;
		this.indexInGroup = indexInGroup;
	}

	public GridAdapter(LayoutInflater inflater, PersistentVector<DesktopItem> folderItems) {
		startDragListener = null;
		dragEventListener = null;
		li = inflater;
		items = folderItems;
		this.groupName = null;
		this.indexInGroup = -1;
	}
	
	private void setTags(View dragEventListenerView, int pos) {
		dragEventListenerView.setTag(R.id.pos_extra, pos);
		if (indexInGroup != -1) {
			dragEventListenerView.setTag(R.id.group_name, groupName);
			dragEventListenerView.setTag(R.id.index_in_group, indexInGroup);
		}
	}

	@Override
	public View getView(int pos, View convertView, ViewGroup parent) {
		int itemViewType = getItemViewType(pos);
		switch (itemViewType) {
		case VIEW_TYPE_SLOT:
			return getSlotView(pos, convertView);
		case VIEW_TYPE_ITEM:
			return startDragListener == null
				? getTinyAppItemView(pos, convertView)
				: getAppItemView(pos, convertView);
		case VIEW_TYPE_FOLDER:
			return getFolderView(pos, convertView);
		default:
			throw new UnsupportedOperationException();
		}
	}

	private View getSlotView(int pos, View convertView) {
		if (convertView == null) {
			convertView = li.inflate(R.layout.grid_item_slot, null);
			convertView.setOnDragListener(dragEventListener);
		}
		setTags(convertView, pos);
		convertView.setVisibility(getItem(pos).visible ? View.VISIBLE : View.INVISIBLE);
		return convertView;
	}

	private View getTinyAppItemView(int pos, View convertView) {
		ImageView imageView = (ImageView) convertView;
		if (imageView == null) {
			imageView = (ImageView) li.inflate(R.layout.grid_item_tiny_app, null);
		}
	
		DesktopItem displayItem = getItem(pos);
		imageView.setImageDrawable(displayItem.icon);
		return imageView;
	}

	private View getAppItemView(int pos, View convertView) {
		ImageView imageView;
		TextView textView;
		View triggerView;
		if (convertView == null) {
			convertView = li.inflate(R.layout.grid_item_app, null);
			convertView.setTag(R.id.view_holder1, imageView = (ImageView) convertView.findViewById(R.id.app_icon));
			convertView.setTag(R.id.view_holder2, textView = (TextView) convertView.findViewById(R.id.app_label));
			convertView.setTag(R.id.view_holder3, triggerView = convertView.findViewById(R.id.trigger_to_folder));
			imageView.setOnLongClickListener(startDragListener);
			imageView.setOnDragListener(dragEventListener);
			triggerView.setOnDragListener(dragEventListener);
		} else {
			imageView = (ImageView) convertView.getTag(R.id.view_holder1);
			textView = (TextView) convertView.getTag(R.id.view_holder2);
			triggerView = (View) convertView.getTag(R.id.view_holder3);
		}
	
		DesktopItem displayItem = getItem(pos);
		imageView.setImageDrawable(displayItem.icon);
		textView.setText(displayItem.label);
		setTags(imageView, pos);
		setTags(triggerView, pos);
		convertView.setVisibility(displayItem.visible ? View.VISIBLE : View.INVISIBLE);
		
		return convertView;
	}

	private View getFolderView(int pos, View convertView) {
		final GridView gridView;
		TextView textView;
		View triggerView;
		if (convertView == null) {
			convertView = li.inflate(R.layout.grid_item_dir, null);
			convertView.setTag(R.id.view_holder1, gridView = (GridView) convertView.findViewById(R.id.folder_grid));
			convertView.setTag(R.id.view_holder2, textView = (TextView) convertView.findViewById(R.id.folder_label));
			convertView.setTag(R.id.view_holder3, triggerView = convertView.findViewById(R.id.trigger_to_folder));
			gridView.setOnLongClickListener(startDragListener);
			convertView.findViewById(R.id.folder_clickable).setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) { return gridView.performLongClick(); }
			});
			gridView.setOnDragListener(dragEventListener);
			triggerView.setOnDragListener(dragEventListener);
		} else {
			gridView = (GridView) convertView.getTag(R.id.view_holder1);
			textView = (TextView) convertView.getTag(R.id.view_holder2);
			triggerView = (View) convertView.getTag(R.id.view_holder3);
		}

		DesktopItem displayItem = getItem(pos);
		gridView.setAdapter(new GridAdapter(li, displayItem.folderItems));
		textView.setText(displayItem.label);
		setTags(gridView, pos);
		setTags(triggerView, pos);
		convertView.setVisibility(displayItem.visible ? View.VISIBLE : View.INVISIBLE);
		
		return convertView;
	}

	@Override
	public int getItemViewType(int pos) {
		DesktopItem item = getItem(pos);
		return item == DesktopItem.SLOT
				? VIEW_TYPE_SLOT
				: item.folderItems == null
					? VIEW_TYPE_ITEM
					: VIEW_TYPE_FOLDER;
	}

	@Override
	public int getCount() { return items.size(); }

	@Override
	public DesktopItem getItem(int pos) { return items.get(pos); }

	@Override
	public long getItemId(int pos) { return pos; }

	@Override
	public int getViewTypeCount() { return 3; }

	@Override
	public boolean areAllItemsEnabled() { return false; }

	@Override
	public boolean isEnabled(int pos) {
		// items in folder and slot are disabled
		return startDragListener != null && getItem(pos) != DesktopItem.SLOT;
	}
}
