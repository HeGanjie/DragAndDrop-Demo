package bruce.example.draganddrop;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.DisplayMetrics;
import bruce.common.functional.PersistentVector;

public class Utils {
	public static float dp2Px(float dp, Context context){
	    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
	    return dp * (metrics.densityDpi / 160f);
	}

	public static float px2Dp(float px, Context context) {
	    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
	    return px / (metrics.densityDpi / 160f);
	}

	public static <E> PersistentVector<PersistentVector<E>> partitionAll(int n, List<E> ls) {
		List<PersistentVector<E>> resultList = new ArrayList<PersistentVector<E>>();
		int lsLen = ls.size();
		for (int i = 0, end; i < lsLen; i += n) {
			end = i + n;
			resultList.add(new PersistentVector<E>(ls.subList(i, end <= lsLen ? end : lsLen)));
		}
		return new PersistentVector<PersistentVector<E>>(resultList);
	}
}
