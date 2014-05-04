package bruce.example.draganddrop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public final class GridAdapter extends BaseAdapter {
	private static final int VIEW_TYPE_SLOT = 0;
	private static final int VIEW_TYPE_ITEM = 1;
	private static final int VIEW_TYPE_FOLDER = 2;
	private final LayoutInflater li;
	private final IconDragEventListener dragEventListener;
	private final OnLongClickListener startDragListener;
	
	private List<DesktopItem> itemsBak;
	private List<DesktopItem> items;
	private int draggingItemPosBak = -1;
	private int draggingItemPos = -1;
	
	public GridAdapter(DesktopActivity ctx, List<DesktopItem> displayItems) {
		startDragListener = ctx.startDragListener;
		dragEventListener = ctx.dragEventListener;
		li = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		items = displayItems;
	}

	public GridAdapter(LayoutInflater inflater, List<DesktopItem> folderItems) {
		startDragListener = null;
		dragEventListener = null;
		li = inflater;
		items = folderItems;
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
		convertView.setTag(R.id.pos_extra, pos);
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
			convertView.setTag(R.id.view_holder3, triggerView = convertView.findViewById(R.id.trigger_to_dir));
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
		imageView.setTag(R.id.pos_extra, pos);
		triggerView.setTag(R.id.pos_extra, pos);
		convertView.setVisibility(displayItem.visible ? View.VISIBLE : View.INVISIBLE);
		
		return convertView;
	}

	private View getFolderView(int pos, View convertView) {
		GridView gridView;
		TextView textView;
		View triggerView;
		if (convertView == null) {
			convertView = li.inflate(R.layout.grid_item_dir, null);
			convertView.setTag(R.id.view_holder1, gridView = (GridView) convertView.findViewById(R.id.dir_grid));
			convertView.setTag(R.id.view_holder2, textView = (TextView) convertView.findViewById(R.id.dir_label));
			convertView.setTag(R.id.view_holder3, triggerView = convertView.findViewById(R.id.trigger_to_dir));
			gridView.setOnLongClickListener(startDragListener);
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
		gridView.setTag(R.id.pos_extra, pos);
		triggerView.setTag(R.id.pos_extra, pos);
		convertView.setVisibility(displayItem.visible ? View.VISIBLE : View.INVISIBLE);
		
		return convertView;
	}

	public void startDrag(int pos) {
		itemsBak = Collections.unmodifiableList(items);
		draggingItemPosBak = draggingItemPos = pos;
		items.set(pos, getItem(pos).hide()); // hide dragging item
		notifyDataSetChanged();
	}

	public void endDrag() {
		if (draggingItemPos != -1) // item not in folder
			items.set(draggingItemPos, items.get(draggingItemPos).show());
		itemsBak = null;
		draggingItemPosBak = draggingItemPos = -1;
		notifyDataSetChanged();
	}

	public void dragToSlot(int targetPos) {
		Collections.swap(items, draggingItemPos, targetPos);
		draggingItemPos = targetPos;
		notifyDataSetChanged();
	}
	
	public void dragForMove(int targetPos) {
		DesktopItem draggingItem = items.get(draggingItemPos);
		items.set(draggingItemPos, DesktopItem.SLOT); // move out
		
		DesktopItem targetItem = items.get(targetPos);
		items.set(targetPos, draggingItem); // move in
		draggingItemPos = targetPos;
		
		int nearestSlotPos = getNearestSlotPos(targetPos);
		if (nearestSlotPos < targetPos) { // target item place left
			items.add(targetPos, targetItem);
			items.remove(nearestSlotPos);
		} else { // target item place right
			items.remove(nearestSlotPos);
			items.add(targetPos + 1, targetItem);
		}
		notifyDataSetChanged();
	}

	public void dragForFold(int targetPos) {
		DesktopItem draggingItem = items.get(draggingItemPos);
		items.set(draggingItemPos, DesktopItem.SLOT);
		draggingItemPos = -1;
		
		DesktopItem targetItem = getItem(targetPos);
		String defaultName = li.getContext().getString(R.string.grid_item_folder_unnamed);
		if (targetItem.folderItems == null) {
			items.set(targetPos, new DesktopItem(defaultName, Arrays.asList(targetItem, draggingItem)));
		} else {
			List<DesktopItem> apps = new ArrayList<DesktopItem>(targetItem.folderItems);
			apps.add(draggingItem);
			items.set(targetPos, new DesktopItem(defaultName, apps));
		}
		notifyDataSetChanged();
	}

	public void dragTo(View v) {
		int itemPos = (Integer) v.getTag(R.id.pos_extra);
		if (itemPos == draggingItemPos)
			return; // same item
		items = new ArrayList<DesktopItem>(itemsBak); // recovery first
		draggingItemPos = draggingItemPosBak;
		
		switch (v.getId()) {
		case R.id.slot_view:
			dragToSlot(itemPos);
			break;
		case R.id.app_icon:
		case R.id.dir_grid:
			dragForMove(itemPos);
			break;
		case R.id.trigger_to_dir:
			dragForFold(itemPos);
			break;
		default:
			break;
		}
	}

	private int getNearestSlotPos(int offset) {
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
