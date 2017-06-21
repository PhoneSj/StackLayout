package com.phone.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

/**
 * Created by Phone on 2017/6/20. 该控件只能有一个子控件。只处理水平方向滑动
 */

public class SpecialLayout extends ViewGroup {

	private static final int DEFAULT_INIT_OFFSET_X = 0;

	//子控件初始偏移量
	private int initOffsetX = 0;
	private int distanceX = 0;
	private float mDownX;
	private float mDownY;
	private float mLastX;
	private float mLastY;

	//轻微滑动阈值
	private int mTouchSlop;
	private State lastState = State.Close;
	private State curState = State.Close;
	private State futureState = State.None;

	private ValueAnimator animator;

	/**
	 * 子控件拉动的状态
	 */
	enum State {
		Open, Close, Middle, None
	}

	public SpecialLayout(Context context) {
		this(context, null);
	}

	public SpecialLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SpecialLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SpecialLayout);
		initOffsetX = (int) array.getDimension(R.styleable.SpecialLayout_initOffsetX, DEFAULT_INIT_OFFSET_X);
		array.recycle();
		distanceX = initOffsetX;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);

		int maxChildWidth = 0;
		int maxChildHeight = 0;
		int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			measureChild(child, widthMeasureSpec, heightMeasureSpec);
			maxChildWidth = Math.max(child.getMeasuredWidth(), maxChildWidth);
			maxChildHeight = Math.max(child.getMeasuredHeight(), maxChildHeight);
		}
		int width = widthMode == MeasureSpec.EXACTLY ? widthSize : maxChildWidth;
		int height = heightMode == MeasureSpec.EXACTLY ? heightSize : maxChildHeight;
		setMeasuredDimension(width, height);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			int childLeft = distanceX;
			int childTop = (getMeasuredHeight() - child.getMeasuredHeight()) / 2;
			child.layout(childLeft, childTop, childLeft + child.getMeasuredWidth(),
					childTop + child.getMeasuredHeight());
		}
	}

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
				if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > mTouchSlop) {
					return true;
				}
				break;
		}
		return super.onInterceptTouchEvent(event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (animator != null && animator.isRunning()) {
			return true;
		}
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				mLastX = event.getX();
				mLastY = event.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				float dx = event.getX() - mLastX;
				float dy = event.getY() - mLastY;
				curState = State.Middle;
				if (dx != 0) {
					checkDistanceX(dx);
					offsetXForChildren();
					if (onOffsetXFractionListener != null) {
						float rate = Math.abs(distanceX * 1.0f / initOffsetX);
						onOffsetXFractionListener.onOffsetFraction(rate);
					}
				}
				mLastX = event.getX();
				mLastY = event.getY();
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				judgeState();
				onRealese();
				break;
		}
		return true;
	}

	private void judgeState() {
		float rate = lastState == State.Close ? 0.3f : 0.7f;
		if (Math.abs(distanceX - initOffsetX) > rate * Math.abs(initOffsetX)) {
			futureState = State.Open;
		} else {
			futureState = State.Close;
		}
	}

	/**
	 * 手指松开
	 */
	private void onRealese() {
		if (futureState == State.Open) {
			animator = ValueAnimator.ofInt(distanceX, 0);
		} else {
			animator = ValueAnimator.ofInt(distanceX, initOffsetX);
		}
		animator.setInterpolator(new DecelerateInterpolator());
		animator.setDuration(300);
		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				distanceX = (int) valueAnimator.getAnimatedValue();
				offsetXForChildren();
				requestLayout();
				if (onOffsetXFractionListener != null) {
					float rate = Math.abs(distanceX * 1.0f / initOffsetX);
					onOffsetXFractionListener.onOffsetFraction(rate);
				}
			}
		});
		animator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				lastState = futureState;
				curState = futureState;
				futureState = State.None;
				animator = null;
			}
		});
		animator.start();
	}

	/**
	 * 水平方向移动子控件
	 */
	private void offsetXForChildren() {
		int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			child.layout(distanceX, child.getTop(), distanceX + getMeasuredWidth(), child.getBottom());
		}
		requestLayout();
	}

	/**
	 * 矫正子控件x方向偏移量
	 * 
	 * @param dx
	 */
	private void checkDistanceX(float dx) {
		distanceX += dx;
		if (initOffsetX > 0) {
			distanceX = distanceX > initOffsetX ? initOffsetX : distanceX;
			distanceX = distanceX < 0 ? 0 : distanceX;
		} else {
			distanceX = distanceX < initOffsetX ? initOffsetX : distanceX;
			distanceX = distanceX > 0 ? 0 : distanceX;
		}
	}

	/**
	 * 水平滑动比例进度监听
	 */
	public interface OnOffsetXFractionListener {
		void onOffsetFraction(float fraction);
	}

	private OnOffsetXFractionListener onOffsetXFractionListener;

	public void setOnOffsetXFractionListener(OnOffsetXFractionListener listener) {
		this.onOffsetXFractionListener = listener;
	}
}
