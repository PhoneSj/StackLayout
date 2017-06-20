package com.phone.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ListAdapter;

/**
 * Created by Phone on 2017/6/19.
 */

public class StackLayout extends ViewGroup {

	private static final int INVALID_POSITION = -1;
	private static final int DEFAULT_PILE_ITEM_OFFSET = 30;
	private static final int DEFAULT_PILE_ITEM_SUM = 3;
	private final static long LONG_CLICK_LIMIT = 500;

	private int pileItemOffset = DEFAULT_PILE_ITEM_OFFSET;
	private int pileItemSum = DEFAULT_PILE_ITEM_SUM;

	//速度跟踪器
	private VelocityTracker velocityTracker;
	//轻微滑动阈值
	private int mTouchSlop;
	//最近一次down事件触发的时间
	private long mLastDownTime;
	//是否触发了长按响应
	private boolean isLongClick = false;
	//是否触发了点击事件
	private boolean isClick = false;
	//判定的滑动方向
	private Direction mDirection = Direction.NONE;
	//	//当前x方向的偏移总量
	//	private float mDistanceX;
	//	//手指松开后的动画
	//	private ValueAnimator mItemAnimator;

	//	//当前的滑动状态
	//	private PileLayout.State mState = PileLayout.State.CLOSE;
	//	//上一次的滑动状态
	//	private PileLayout.State oldState = PileLayout.State.CLOSE;
	//	//将要进入的状态
	//	private PileLayout.State newState = PileLayout.State.CLOSE;

	private int distance = 0;

	private ListAdapter adapter;

	private DataSetObserver dataSetObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
			//TODO
		}
	};

	/**
	 * 当前判定的滑动方向
	 */
	enum Direction {
		HORIZONTAL, VERTICAL, NONE
	}
	//
	//	/**
	//	 * 当前侧滑的状态
	//	 */
	//	enum State {
	//		OPEN, CLOSE, MIDDLE
	//	}

	public StackLayout(Context context) {
		this(context, null);
	}

	public StackLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public StackLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.StackLayout);
		pileItemOffset = (int) array.getDimension(R.styleable.StackLayout_pileItemOffset, DEFAULT_PILE_ITEM_OFFSET);
		pileItemSum = array.getInt(R.styleable.StackLayout_pileItemSum, DEFAULT_PILE_ITEM_SUM);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);

		int maxChildWidth = 0;
		int totalHeight = 0;
		int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			measureChild(child, widthMeasureSpec, heightMeasureSpec);
			maxChildWidth = Math.max(child.getMeasuredWidth(), maxChildWidth);
			totalHeight += child.getMeasuredHeight();
		}
		totalHeight += pileItemOffset * pileItemSum * 2;
		int width = widthMode == MeasureSpec.EXACTLY ? widthSize : maxChildWidth;
		int height = heightMode == MeasureSpec.EXACTLY ? heightSize : totalHeight;
		setMeasuredDimension(width, height);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int count = getChildCount();
		int totalHeight = pileItemOffset * pileItemSum;
		//查找第一个完全显示的item
		int firstShowAllIndex = 0;
		int firstShowAllTop = pileItemOffset * pileItemSum;
		for (int i = 0; i < count; i++) {
			if (totalHeight + distance >= pileItemOffset * pileItemSum) {
				firstShowAllIndex = i;
				firstShowAllTop = totalHeight + distance;
				break;
			}
			totalHeight += getChildAt(i).getMeasuredHeight();
		}
		//查找最后一个完全显示的item
		totalHeight = pileItemOffset * pileItemSum;
		int lastShowAllIndex = count - 1;
		int lastShowAllBottom = getMeasuredHeight() - pileItemOffset * pileItemSum;
		for (int i = 0; i < count; i++) {
			totalHeight += getChildAt(i).getMeasuredHeight();
			if (totalHeight + distance < getMeasuredHeight() - pileItemOffset * pileItemSum) {
				if (i + 1 < count && totalHeight + distance
						+ getChildAt(i + 1).getMeasuredHeight() > getMeasuredHeight() - pileItemOffset * pileItemSum) {
					lastShowAllIndex = i;
					lastShowAllBottom = totalHeight + distance;
					break;
				}
			}
		}
		//对中间部分item布局
		int childLeft = 0;
		int childTop = firstShowAllTop;
		for (int i = firstShowAllIndex; i <= lastShowAllIndex; i++) {
			final View child = getChildAt(i);
			child.layout(childLeft, childTop, childLeft + child.getMeasuredWidth(),
					childTop + child.getMeasuredHeight());
			childTop += child.getMeasuredHeight();
			child.setZ(0);
		}

		if (firstShowAllIndex > 0) {
			layoutTopPile(firstShowAllIndex, firstShowAllTop);
		}

		if (lastShowAllIndex < count - 1) {
			layoutBottomPile(lastShowAllIndex, lastShowAllBottom);
		}

	}

	private void layoutTopPile(int firstShowAllIndex, int firstShowAllTop) {
		int topPileDistance = pileItemOffset * pileItemSum;
		int itemHeight = getChildAt(firstShowAllIndex).getMeasuredHeight();
		int childLeft = 0;
		int childTop = pileItemOffset * pileItemSum;
		float alpha = 1.0f;
		for (int i = firstShowAllIndex - 1, z = -1, k = 1; i >= 0; i--, z--, k++) {
			final View child = getChildAt(i);
			float fraction = Math.abs(firstShowAllTop - topPileDistance - itemHeight * k) * 1.0f
					/ (itemHeight * pileItemSum);
			childTop = (int) (pileItemOffset * pileItemSum * (1 - fraction));
			alpha = 1 - fraction;
			child.layout(childLeft, childTop, childLeft + child.getMeasuredWidth(),
					childTop + child.getMeasuredHeight());
			child.setAlpha(alpha);
			child.setZ(z);
		}
	}

	private void layoutBottomPile(int lastShowAllIndex, int lastShowAllBottom) {
		int count = getChildCount();
		int itemHeight = getChildAt(lastShowAllIndex).getMeasuredHeight();
		int childLeft = 0;
		int childBottom = getMeasuredHeight() - pileItemOffset * pileItemSum;
		float alpha = 1.0f;
		for (int i = lastShowAllIndex + 1, z = -1, k = 1; i < count; i++, z--, k++) {
			final View child = getChildAt(i);
			float fraction = (lastShowAllBottom - (getMeasuredHeight() - pileItemOffset * pileItemSum) + itemHeight * k)
					* 1.0f / (itemHeight * pileItemSum);
			childBottom = getMeasuredHeight() - (int) ((pileItemOffset * pileItemSum) * (1 - fraction));
			alpha = 1 - fraction;
			child.layout(childLeft, childBottom - child.getMeasuredHeight(), childLeft + child.getMeasuredWidth(),
					childBottom);
			child.setAlpha(alpha);
			child.setZ(z);
		}
	}

	public void setAdapter(@NonNull ListAdapter adapter) {
		if (adapter == null) {
			throw new IllegalArgumentException("The adapter cannot be null.");
		}
		if (this.adapter != null) {
			this.adapter.unregisterDataSetObserver(dataSetObserver);
		}
		this.adapter = adapter;
		this.adapter.registerDataSetObserver(dataSetObserver);
		attachChildViews();
		requestLayout();
	}

	private void attachChildViews() {
		removeAllViews();
		int count = adapter.getCount();
		for (int i = 0; i < count; i++) {
			final View child = adapter.getView(i, null, this);
			addView(child);
		}
	}

	private Paint testPaint;

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		testPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		testPaint.setColor(0x66ffaaaa);
		canvas.drawRect(0, 0, getMeasuredWidth(), pileItemOffset * pileItemSum, testPaint);
		canvas.drawRect(0, getMeasuredHeight() - pileItemOffset * pileItemSum, getMeasuredWidth(), getMeasuredHeight(),
				testPaint);
	}

	//down事件的x、y坐标
	private float mDownX;
	private float mDownY;
	//上次事件的x、y坐标
	private float mLastX;
	private float mLastY;

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				mDownX = event.getX();
				mDownY = event.getY();
				mLastX = event.getX();
				mLastY = event.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				float dx = event.getX() - mDownX;
				float dy = event.getY() - mDownY;
				//水平、竖直都需要拦截
				if (Math.abs(dx) > mTouchSlop || Math.abs(dy) > mTouchSlop) {
					return true;
				}
				break;
			case MotionEvent.ACTION_UP:
				break;
		}
		return super.onInterceptTouchEvent(event);
	}

	int velocity = 0;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				//取消fling动画执行
				if (flingAnimator != null) {
					flingAnimator.cancel();
				}
				if (velocityTracker == null) {
					velocityTracker = VelocityTracker.obtain();
				} else {
					velocityTracker.clear();
				}
				//滑动方向重置放在up事件时，无法决定重置、响应长按事件的执行先后
				mDirection = Direction.NONE;
				mDownX = event.getX();
				mDownY = event.getY();
				mLastX = event.getX();
				mLastY = event.getY();
				mLastDownTime = System.currentTimeMillis();
				postDelayed(new Runnable() {
					@Override
					public void run() {
						if (mDirection == Direction.NONE) {
							//没有触发点击事件，才能触发长按事件
							if (!isClick && mOnItemLongClickListener != null) {
								int position = judgePosition();
								mOnItemLongClickListener.onItemLongClick(position);
								isLongClick = true;
							}
						}
					}
				}, LONG_CLICK_LIMIT);
				isClick = false;
				break;
			case MotionEvent.ACTION_MOVE:
				float dx = event.getX() - mDownX;
				float dy = event.getY() - mDownY;
				if (mDirection == Direction.NONE && (Math.abs(dx) > mTouchSlop || Math.abs(dy) > mTouchSlop)) {
					//					if (Math.abs(dy) > Math.abs(dx)) {
					mDirection = Direction.VERTICAL;
					//					} else {
					//						mDirection = Direction.NONE;
					//					}
				}

				if (mDirection == Direction.VERTICAL) {
					//响应竖直方向滑动，这里记录总偏移值
					distance += event.getY() - mLastY;
					distance = checkDistance(distance);
					requestLayout();
				}

				mLastX = event.getX();
				mLastY = event.getY();

				invalidate();

				velocityTracker.addMovement(event);
				velocityTracker.computeCurrentVelocity(1000);
				velocity = (int) velocityTracker.getYVelocity();
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				if (mDirection == Direction.NONE) {
					//没有触发长按事件才能触发点击事件
					if (!isLongClick && mOnItemClickListener != null) {
						int position = judgePosition();
						mOnItemClickListener.onItemclick(position);
						isClick = true;
					}
					isLongClick = false;
				} else if (mDirection == Direction.VERTICAL) {
					onFling(velocity);
					velocity = 0;
				}
				mLastX = 0;
				mLastY = 0;
				mLastDownTime = 0;
				//				if (velocityTracker != null) {
				//					velocityTracker.recycle();
				//				}
				break;
		}
		return true;
	}

	private ValueAnimator flingAnimator;

	private void onFling(int velocity) {
		Log.w("phoneTest", "velocity:" + velocity);
		int newDistance = checkDistance(distance + velocity);
		int duration = Math.abs(newDistance - distance);
		flingAnimator = ValueAnimator.ofInt(distance, newDistance);
		flingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				distance = (int) valueAnimator.getAnimatedValue();
				requestLayout();
			}
		});
		flingAnimator.setInterpolator(new DecelerateInterpolator());
		flingAnimator.setDuration(duration);
		flingAnimator.start();
	}

	/**
	 * 矫正竖直方向偏移量
	 * 
	 */
	private int checkDistance(int value) {
		int itemHeight = getChildAt(0).getMeasuredHeight();
		int count = getChildCount();
		int upThroshold = 0;
		int downThroshold = getMeasuredHeight() - pileItemOffset * pileItemSum * 2 - itemHeight * count;
		value = value > upThroshold ? upThroshold : value;
		value = value < downThroshold ? downThroshold : value;
		return value;
	}

	/**
	 * 判断点击的是哪个子View
	 *
	 * @return
	 */
	private int judgePosition() {
		int count = getChildCount();
		View[] sortChildren = new View[count];
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			sortChildren[i] = child;
		}
		//按Z坐标排序，值大的在前面
		View temp;
		for (int i = 0; i < count; i++) {
			for (int j = i; j < count - 1; j++) {
				if (sortChildren[j].getZ() < sortChildren[j + 1].getZ()) {
					temp = sortChildren[j];
					sortChildren[j] = sortChildren[j + 1];
					sortChildren[j + 1] = temp;
				}

			}

		}
		//找出接收事件的子控件
		View targetView = null;
		for (int i = 0; i < count; i++) {
			final View child = sortChildren[i];
			RectF rectF = new RectF(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
			if (rectF.contains(mDownX, mDownY)) {
				targetView = child;
				break;
			}
		}
		//判定接收事件的子控件是第几个
		if (targetView == null) {
			return INVALID_POSITION;
		}
		int position = INVALID_POSITION;
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child == targetView) {
				position = i;
				break;
			}
		}
		return position;
	}

	private OnItemClickListener mOnItemClickListener;
	private OnItemLongClickListener mOnItemLongClickListener;

	public void setOnItemClickListener(OnItemClickListener listener) {
		this.mOnItemClickListener = listener;
	}

	public void setOnItemLongClickListener(OnItemLongClickListener listener) {
		this.mOnItemLongClickListener = listener;
	}

	public interface OnItemClickListener {
		void onItemclick(int position);
	}

	public interface OnItemLongClickListener {
		void onItemLongClick(int position);
	}
}
