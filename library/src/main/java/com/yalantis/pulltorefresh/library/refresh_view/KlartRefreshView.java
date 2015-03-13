package com.yalantis.pulltorefresh.library.refresh_view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.support.annotation.NonNull;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

import com.yalantis.pulltorefresh.library.PullToRefreshView;
import com.yalantis.pulltorefresh.library.R;

import java.util.Random;

public class KlartRefreshView extends BaseRefreshView implements Animatable {

    private static final float SCALE_START_PERCENT = 0.5f;
    private static final int ANIMATION_DURATION = 1000;

    private static final float SIDE_CLOUDS_INITIAL_SCALE = 1.05f;
    private static final float SIDE_CLOUDS_FINAL_SCALE = 1.55f;

    private static final float CENTER_CLOUDS_INITIAL_SCALE = 1.0f;
    private static final float CENTER_CLOUDS_FINAL_SCALE = 1.5f;

    private static final float SUN_FINAL_SCALE = 1.5f;
    private static final float SUN_INITIAL_ROTATE_GROWTH = 1.2f;
    private static final float SUN_FINAL_ROTATE_GROWTH = 1.5f;

    private static final Interpolator ACCELERATE_DECELERATE_INTERPOLATOR = new AccelerateDecelerateInterpolator();
    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();


    // Multiply with this animation interpolator time
//    public static final int LOADING_ANIMATION_COEFFICIENT = 80;
    public static final int LOADING_ANIMATION_COEFFICIENT = 640;
    public static final int CENTER_CLOUDS_LOADING_ANIMATION_COEFFICIENT = 48;
    public static final int SLOW_DOWN_ANIMATION_COEFFICIENT = 6;
    // Amount of lines when is going lading animation
    public static final int Y_SIDE_CLOUDS_SLOW_DOWN_COF = 4;
    public static final int X_SIDE_CLOUDS_SLOW_DOWN_COF = 2;

    private PullToRefreshView mParent;
    private Matrix mMatrix;
    private Matrix mAdditionalMatrix;
    private Animation mAnimation;

    private int mTop;
    private int mScreenWidth;

    private int mSunSize = 100;
    private float mSunLeftOffset;
    private float mSunTopOffset;

    private int mFrontCloudHeightCenter;
    private int mFrontCloudWidthCenter;
    private int mRightCloudsWidthCenter;
    private int mRightCloudsHeightCenter;
    private int mLeftCloudsWidthCenter;
    private int mLeftCloudsHeightCenter;

    private float mPercent = 0.0f;
    private float mRotate = 0.0f;

    private Bitmap mSun;
    private Bitmap mFrontClouds;
    private Bitmap mLeftClouds;
    private Bitmap mRightClouds;

    private boolean isRefreshing = false;
    private float mLoadingAnimationTime;

    private Random mRandom;

    public KlartRefreshView(Context context, PullToRefreshView parent) {
        super(context, parent);
        mParent = parent;
        mMatrix = new Matrix();
        mAdditionalMatrix = new Matrix();
        mRandom = new Random();

        initiateDimens();
        createBitmaps();
        setupAnimations();
    }

    private void initiateDimens() {
        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mTop = -mParent.getTotalDragDistance();

        mSunLeftOffset = 0.3f * (float) mScreenWidth;
        mSunTopOffset = (mParent.getTotalDragDistance() * 0.1f);
    }

    private void createBitmaps() {
        mSun = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.sun);
        mSun = Bitmap.createScaledBitmap(mSun, mSunSize, mSunSize, true);

        mLeftClouds = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.clouds_left);
        mRightClouds = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.clouds_right);
        mFrontClouds = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.clouds_center);

        mFrontCloudWidthCenter = mFrontClouds.getWidth() / 2;
        mFrontCloudHeightCenter = mFrontClouds.getHeight() / 2;

        mRightCloudsWidthCenter = mRightClouds.getWidth() / 2;
        mRightCloudsHeightCenter = mRightClouds.getHeight() / 2;
        mLeftCloudsWidthCenter = mLeftClouds.getWidth() / 2;
        mLeftCloudsHeightCenter = mLeftClouds.getHeight() / 2;
    }

    @Override
    public void setPercent(float percent, boolean invalidate) {
        setPercent(percent);
        if (invalidate) setRotate(percent);
    }

    @Override
    public void offsetTopAndBottom(int offset) {
        mTop += offset;
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final int saveCount = canvas.save();

        // DRAW BACKGROUND.
        canvas.drawColor(getContext().getResources().getColor(R.color.jet_sky_background));
        drawSun(canvas);
        drawSideClouds(canvas);
        drawCenterClouds(canvas);

        canvas.restoreToCount(saveCount);
    }

    private void drawSun(Canvas canvas) {
        Matrix matrix = mMatrix;
        matrix.reset();

        float dragPercent = mPercent;
        if (dragPercent > 1.0f) { // Slow down if pulling over set height
            dragPercent = (dragPercent + 9.0f) / 10;
        }

        float sunRadius = (float) mSunSize / 2.0f;
        float sunRotateGrowth = SUN_INITIAL_ROTATE_GROWTH;

        float offsetX = mSunLeftOffset;
        float offsetY = mSunTopOffset
                + (mParent.getTotalDragDistance() / 2) * (1.0f - dragPercent) // Move the sun up
                - mTop; // Depending on Canvas position

        float scalePercentDelta = dragPercent - SCALE_START_PERCENT;
        if (scalePercentDelta > 0) {
            float scalePercent = scalePercentDelta / (1.0f - SCALE_START_PERCENT);
            float sunScale = 1.0f - (1.0f - SUN_FINAL_SCALE) * scalePercent;
            sunRotateGrowth += (SUN_FINAL_ROTATE_GROWTH - SUN_INITIAL_ROTATE_GROWTH) * scalePercent;

            matrix.preTranslate(offsetX + (sunRadius - sunRadius * sunScale), offsetY * (2.0f - sunScale));
            matrix.preScale(sunScale, sunScale);

            offsetX += sunRadius;
            offsetY = offsetY * (2.0f - sunScale) + sunRadius * sunScale;
        } else {
            matrix.postTranslate(offsetX, offsetY);

            offsetX += sunRadius;
            offsetY += sunRadius;
        }

        matrix.postRotate(
                (isRefreshing ? -360 : 360) * mRotate * (isRefreshing ? 1 : sunRotateGrowth),
                offsetX,
                offsetY);

        canvas.drawBitmap(mSun, matrix, null);
    }

    private void drawSideClouds(Canvas canvas) {
        Matrix matrixLeftClouds = mMatrix;
        Matrix matrixRightClouds = mAdditionalMatrix;
        matrixLeftClouds.reset();
        matrixRightClouds.reset();

        // Drag percent will newer get more then 1 here
        float dragPercent = Math.min(1f, Math.abs(mPercent));

        boolean overdrag = false;

        // But we check here for overdrag
        if (mPercent > 1.0f) {
            overdrag = true;
        }

        float scale;
        float scalePercentDelta = dragPercent - SCALE_START_PERCENT;
        if (scalePercentDelta > 0) {
            float scalePercent = scalePercentDelta / (1.0f - SCALE_START_PERCENT);
            scale = SIDE_CLOUDS_INITIAL_SCALE + (SIDE_CLOUDS_FINAL_SCALE - SIDE_CLOUDS_INITIAL_SCALE) * scalePercent;
        } else {
            scale = SIDE_CLOUDS_INITIAL_SCALE;
        }

        // Current y position of clouds
        float dragYOffset = mParent.getTotalDragDistance() * (1.0f - dragPercent);

        // Position where clouds fully visible on screen and we should drag them with content of listView
        int cloudsVisiblePosition = mParent.getTotalDragDistance() / 2 - mLeftCloudsHeightCenter;

        boolean needMoveCloudsWithContent = false;
        if (dragYOffset < cloudsVisiblePosition) {
            needMoveCloudsWithContent = true;
        }

        float offsetRightX = mScreenWidth - mRightClouds.getWidth();
        float offsetRightY = (needMoveCloudsWithContent
                ? mParent.getTotalDragDistance() * dragPercent - mLeftClouds.getHeight()
                : dragYOffset)
                + (overdrag ? mTop : 0);

        float offsetLeftX = 0;
        float offsetLeftY = (needMoveCloudsWithContent
                ? mParent.getTotalDragDistance() * dragPercent - mLeftClouds.getHeight()
                : dragYOffset)
                + (overdrag ? mTop : 0);

        // Magic with animation on loading process
        if (isRefreshing) {
            if (checkCurrentAnimationPart(AnimationPart.FIRST)) {
                offsetLeftY += getAnimationPartValue(AnimationPart.FIRST) / Y_SIDE_CLOUDS_SLOW_DOWN_COF;
                offsetRightX -= getAnimationPartValue(AnimationPart.FIRST) / X_SIDE_CLOUDS_SLOW_DOWN_COF;
            } else if (checkCurrentAnimationPart(AnimationPart.SECOND)) {
                offsetLeftY += getAnimationPartValue(AnimationPart.SECOND) / Y_SIDE_CLOUDS_SLOW_DOWN_COF;
                offsetRightX -= getAnimationPartValue(AnimationPart.SECOND) / X_SIDE_CLOUDS_SLOW_DOWN_COF;
            } else if (checkCurrentAnimationPart(AnimationPart.THIRD)) {
                offsetLeftY -= getAnimationPartValue(AnimationPart.THIRD) / Y_SIDE_CLOUDS_SLOW_DOWN_COF;
                offsetRightX += getAnimationPartValue(AnimationPart.THIRD) / X_SIDE_CLOUDS_SLOW_DOWN_COF;
            } else if (checkCurrentAnimationPart(AnimationPart.FOURTH)) {
                offsetLeftY -= getAnimationPartValue(AnimationPart.FOURTH) / X_SIDE_CLOUDS_SLOW_DOWN_COF;
                offsetRightX += getAnimationPartValue(AnimationPart.FOURTH) / Y_SIDE_CLOUDS_SLOW_DOWN_COF;
            }
        }

        matrixRightClouds.postScale(scale, scale, mRightCloudsWidthCenter, mRightCloudsHeightCenter);
        matrixRightClouds.postTranslate(offsetRightX, offsetRightY);

        matrixLeftClouds.postScale(scale, scale, mLeftCloudsWidthCenter, mLeftCloudsHeightCenter);
        matrixLeftClouds.postTranslate(offsetLeftX, offsetLeftY);

        canvas.drawBitmap(mLeftClouds, matrixLeftClouds, null);
        canvas.drawBitmap(mRightClouds, matrixRightClouds, null);
    }

    private void drawCenterClouds(Canvas canvas) {
        Matrix matrix = mMatrix;
        matrix.reset();
        float dragPercent = Math.min(1f, Math.abs(mPercent));

        float scale;
        float overdragPercent = 0;
        boolean overdrag = false;

        if (mPercent > 1.0f) {
            overdrag = true;
            // Here we want know about how mach percent of over drag we done
            overdragPercent = Math.abs(1.0f - mPercent);
        }

        float scalePercentDelta = dragPercent - SCALE_START_PERCENT;
        float scalePercent = scalePercentDelta / (1.0f - SCALE_START_PERCENT);
        scale = CENTER_CLOUDS_INITIAL_SCALE + (CENTER_CLOUDS_FINAL_SCALE - CENTER_CLOUDS_INITIAL_SCALE) * scalePercent;

        float parallaxPercent = 0;
        boolean parallax = false;
        // Current y position of clouds
        float dragYOffset = mParent.getTotalDragDistance() * dragPercent;
        // Position when should start parallax scrolling
        int startParallaxHeight = mParent.getTotalDragDistance() - mFrontCloudHeightCenter;

        if (dragYOffset > startParallaxHeight) {
            parallax = true;
            parallaxPercent = dragYOffset - startParallaxHeight;
        }

        float offsetX = (mScreenWidth / 2) - mFrontCloudWidthCenter;
        float offsetY = dragYOffset
                - (parallax ? mFrontCloudHeightCenter + parallaxPercent : mFrontCloudHeightCenter)
                + (overdrag ? mTop : 0);

        float sx = scale + overdragPercent / 4;
        float sy = scale + overdragPercent / 2;

        if (isRefreshing) {
            if (checkCurrentAnimationPart(AnimationPart.FIRST)) {
                sx -= (getAnimationPartValue(AnimationPart.FIRST) / CENTER_CLOUDS_LOADING_ANIMATION_COEFFICIENT) / 8;
            } else if (checkCurrentAnimationPart(AnimationPart.SECOND)) {
                sx -= (getAnimationPartValue(AnimationPart.SECOND) / CENTER_CLOUDS_LOADING_ANIMATION_COEFFICIENT) / 8;
            } else if (checkCurrentAnimationPart(AnimationPart.THIRD)) {
                sx += (getAnimationPartValue(AnimationPart.THIRD) / CENTER_CLOUDS_LOADING_ANIMATION_COEFFICIENT) / 6;
            } else if (checkCurrentAnimationPart(AnimationPart.FOURTH)) {
                sx += (getAnimationPartValue(AnimationPart.FOURTH) / CENTER_CLOUDS_LOADING_ANIMATION_COEFFICIENT) / 6;
            }
            sy = sx;
        }

        matrix.postScale(sx, sy, mFrontCloudWidthCenter, mFrontCloudHeightCenter);
        matrix.postTranslate(offsetX, offsetY);

        canvas.drawBitmap(mFrontClouds, matrix, null);
    }

    /**
     * We need a special value for different part of animation
     *
     * @param part - needed part
     * @return - value for needed part
     */
    private float getAnimationPartValue(AnimationPart part) {
        switch (part) {
            case FIRST: {
                return mLoadingAnimationTime;
            }
            case SECOND: {
                return getAnimationTimePart(AnimationPart.FOURTH) - (mLoadingAnimationTime - getAnimationTimePart(AnimationPart.FOURTH));
            }
            case THIRD: {
                return mLoadingAnimationTime - getAnimationTimePart(AnimationPart.SECOND);
            }
            case FOURTH: {
                return getAnimationTimePart(AnimationPart.THIRD) - (mLoadingAnimationTime - getAnimationTimePart(AnimationPart.FOURTH));
            }
            default:
                return 0;
        }
    }

    /**
     * On drawing we should check current part of animation
     *
     * @param part - needed part of animation
     * @return - return true if current part
     */
    private boolean checkCurrentAnimationPart(AnimationPart part) {
        switch (part) {
            case FIRST: {
                return mLoadingAnimationTime < getAnimationTimePart(AnimationPart.FOURTH);
            }
            case SECOND:
            case THIRD: {
                return mLoadingAnimationTime < getAnimationTimePart(part);
            }
            case FOURTH: {
                return mLoadingAnimationTime > getAnimationTimePart(AnimationPart.THIRD);
            }
            default:
                return false;
        }
    }

    /**
     * Get part of animation duration
     *
     * @param part - needed part of time
     * @return - interval of time
     */
    private int getAnimationTimePart(AnimationPart part) {
        switch (part) {
            case SECOND: {
                return LOADING_ANIMATION_COEFFICIENT / 2;
            }
            case THIRD: {
                return getAnimationTimePart(AnimationPart.FOURTH) * 3;
            }
            case FOURTH: {
                return LOADING_ANIMATION_COEFFICIENT / 4;
            }
            default:
                return 0;
        }
    }

    public void setPercent(float percent) {
        mPercent = percent;
    }

    public void resetOriginals() {
        setPercent(0);
        setRotate(0);
    }

    public void setRotate(float rotate) {
        mRotate = rotate;
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void start() {
        mAnimation.reset();
        isRefreshing = true;
        mParent.startAnimation(mAnimation);
    }

    @Override
    public void stop() {
        mParent.clearAnimation();
        isRefreshing = false;
        resetOriginals();
    }

    private void setupAnimations() {
        mAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, @NonNull Transformation t) {
                setLoadingAnimationTime(interpolatedTime);
                setRotate(interpolatedTime);
            }
        };
        mAnimation.setRepeatCount(Animation.INFINITE);
        mAnimation.setRepeatMode(Animation.REVERSE);
        mAnimation.setInterpolator(ACCELERATE_DECELERATE_INTERPOLATOR);
        mAnimation.setDuration(ANIMATION_DURATION);
    }

    private void setLoadingAnimationTime(float loadingAnimationTime) {
        /**SLOW DOWN ANIMATION IN {@link #SLOW_DOWN_ANIMATION_COEFFICIENT} time */
        mLoadingAnimationTime = LOADING_ANIMATION_COEFFICIENT * (loadingAnimationTime / SLOW_DOWN_ANIMATION_COEFFICIENT);
        invalidateSelf();
    }

}
