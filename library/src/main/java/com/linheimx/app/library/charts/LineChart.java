package com.linheimx.app.library.charts;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewParent;

import com.linheimx.app.library.R;
import com.linheimx.app.library.model.HightLight;
import com.linheimx.app.library.data.Lines;
import com.linheimx.app.library.manager.MappingManager;
import com.linheimx.app.library.model.XAxis;
import com.linheimx.app.library.model.YAxis;
import com.linheimx.app.library.render.GodRender;
import com.linheimx.app.library.render.HighLightRender;
import com.linheimx.app.library.render.LineRender;
import com.linheimx.app.library.render.NoDataRender;
import com.linheimx.app.library.render.XAxisRender;
import com.linheimx.app.library.render.YAxisRender;
import com.linheimx.app.library.touch.GodTouchListener;
import com.linheimx.app.library.touch.TouchListener;
import com.linheimx.app.library.utils.LogUtil;
import com.linheimx.app.library.utils.RectD;
import com.linheimx.app.library.utils.SingleD_XY;
import com.linheimx.app.library.utils.Utils;

/**
 * Created by Administrator on 2016/11/13.
 */

public class LineChart extends Chart {

    MappingManager _MappingManager;

    Lines _lines;

    ////////////////////////// function //////////////////////////
    boolean isHighLightEnabled = true;
    boolean isTouchEnabled = true;
    ChartMode _ChartMode;

    ///////////////////////////////// parts ////////////////////////////////
    XAxis _XAxis;
    YAxis _YAxis;
    HightLight _HightLight;

    //////////////////////////////////  render  /////////////////////////////
    NoDataRender _NoDataRender;
    XAxisRender _XAxisRender;
    YAxisRender _YAxisRender;
    LineRender _LineRender;
    HighLightRender _HighLightRender;
    GodRender _GodRender;


    ////////////////////////////// touch  /////////////////////////////
    TouchListener _TouchListener;
    GodTouchListener _GodTouchListener;

    //////////////////////////// 区域 ///////////////////////////
    RectF _MainPlotRect;// 主要的绘图区域

    float _paddingLeft = 20;
    float _paddingRight = 5;
    float _paddingTop = 15;
    float _paddingBottom = 15;


    RectF _GodRect;//


    public LineChart(Context context) {
        super(context);
    }

    public LineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LineChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(Context context, AttributeSet attributeSet) {
        super.init(context, attributeSet);

        TypedArray typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.LineChart);
        boolean isGodMode = typedArray.getBoolean(R.styleable.LineChart_god_mode, false);
        _ChartMode = isGodMode ? ChartMode.God : ChartMode.Normal;
        typedArray.recycle();

        // init v
        _MainPlotRect = new RectF();
        _MappingManager = new MappingManager(_MainPlotRect);
        _GodRect = new RectF();

        // models
        _XAxis = new XAxis();
        _YAxis = new YAxis();
        _HightLight = new HightLight();

        // render
        _NoDataRender = new NoDataRender(_MainPlotRect, _MappingManager);
        _XAxisRender = new XAxisRender(_MainPlotRect, _MappingManager, _XAxis);
        _YAxisRender = new YAxisRender(_MainPlotRect, _MappingManager, _YAxis);
        _LineRender = new LineRender(_MainPlotRect, _MappingManager, _lines, this);
        _HighLightRender = new HighLightRender(_MainPlotRect, _MappingManager, _lines, _HightLight);
        _GodRender = new GodRender(_MainPlotRect, _MappingManager, _GodRect);

        // touch listener
        _TouchListener = new TouchListener(this);
        _GodTouchListener = new GodTouchListener(this);
    }


    @Override
    public void computeScroll() {
        super.computeScroll();

        if (_ChartMode == ChartMode.Normal) {
            _TouchListener.computeScroll();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (_lines == null) {
            return false;
        }
        if (!isTouchEnabled) {
            return false;
        }

        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }

        if (_ChartMode == ChartMode.Normal) {
            return _TouchListener.onTouch(this, event);
        } else if (_ChartMode == ChartMode.God) {
            return _GodTouchListener.onTouch(this, event);
        }

        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. render no data
        if (_lines == null || _lines.getLines() == null || _lines.getLines().size() == 0) {
            _NoDataRender.render(canvas);
            return;
        }

        // 计算轴线上的数值
        _XAxis.calValues(getVisiableMinX(), getVisiableMaxX());
        _YAxis.calValues(getVisiableMinY(), getVisiableMaxY());

        // render grid line
        _XAxisRender.renderGridline(canvas);
        _YAxisRender.renderGridline(canvas);

        // render line
        _LineRender.render(canvas);
        // render high light
        _HighLightRender.render(canvas);


        // render god
        if (_ChartMode == ChartMode.God) {
            _GodRender.render(canvas);
        }

        // render warn line
        _XAxisRender.renderWarnLine(canvas);
        _YAxisRender.renderWarnLine(canvas);

        // render Axis
        _XAxisRender.renderAxisLine(canvas);
        _YAxisRender.renderAxisLine(canvas);

        // render labels
        _XAxisRender.renderLabels(canvas);
        _YAxisRender.renderLabels(canvas);

        // render unit
        _XAxisRender.renderUnit(canvas);
        _YAxisRender.renderUnit(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        notifyDataChanged();
    }


    /**
     * 通知数据改变
     */
    public void notifyDataChanged() {

        limitMainPlotArea();

        if (_lines == null) {
            return;
        }
        _lines.calMinMax();

        prepareMap();

        // 2. onDataChanged
        _LineRender.onDataChanged(_lines);
        _HighLightRender.onDataChanged(_lines);
    }

    /**
     * 限制 主绘图区域的边界
     */
    private void limitMainPlotArea() {

        _MainPlotRect.setEmpty();
        _MainPlotRect.right += _MainPlotRect.left + getWidth();
        _MainPlotRect.bottom += _MainPlotRect.top + getHeight();

        // 0. padding
        offsetPadding();
        // 1. 计算label,unit的宽高
        offsetArea();

        if (_ChartMode == ChartMode.God) {
            _GodRect.set(_MainPlotRect);
            _GodRect.right = _GodRect.right / 3;
        }
    }

    private void offsetPadding() {

        _MainPlotRect.left += Utils.dp2px(_paddingLeft);
        _MainPlotRect.top += Utils.dp2px(_paddingTop);
        _MainPlotRect.right -= Utils.dp2px(_paddingRight);
        _MainPlotRect.bottom -= Utils.dp2px(_paddingBottom);
    }

    private void offsetArea() {
        _MainPlotRect.bottom -= _XAxis.offsetBottom();
        _MainPlotRect.left += _XAxis.offsetLeft();
    }


    /**
     * 准备映射关系
     */
    private void prepareMap() {

        double xMin = _lines.getmXMin();
        double xMax = _lines.getmXMax();
        double yMin = _lines.getmYMin();
        double yMax = _lines.getmYMax();

        _MappingManager.prepareRelation(xMin, xMax, yMin, yMax);
    }

    public float get_paddingLeft() {
        return _paddingLeft;
    }

    public void set_paddingLeft(float _paddingLeft) {
        this._paddingLeft = _paddingLeft;
    }

    public float get_paddingRight() {
        return _paddingRight;
    }

    public void set_paddingRight(float _paddingRight) {
        this._paddingRight = _paddingRight;
    }

    public float get_paddingTop() {
        return _paddingTop;
    }

    public void set_paddingTop(float _paddingTop) {
        this._paddingTop = _paddingTop;
    }

    public float get_paddingBottom() {
        return _paddingBottom;
    }

    public void set_paddingBottom(float _paddingBottom) {
        this._paddingBottom = _paddingBottom;
    }

    public double getVisiableMinX() {
        float px = _MainPlotRect.left;
        SingleD_XY xy = _MappingManager.getValueByPx(px, 0);
        return xy.getX();
    }

    public double getVisiableMaxX() {
        float px = _MainPlotRect.right;
        SingleD_XY xy = _MappingManager.getValueByPx(px, 0);
        return xy.getX();
    }

    public double getVisiableMinY() {
        float py = _MainPlotRect.bottom;
        SingleD_XY xy = _MappingManager.getValueByPx(0, py);
        return xy.getY();
    }

    public double getVisiableMaxY() {
        float py = _MainPlotRect.top;
        SingleD_XY xy = _MappingManager.getValueByPx(0, py);
        return xy.getY();
    }

    public RectD get_currentViewPort() {
        return _MappingManager.get_currentViewPort();
    }

    public void set_currentViewPort(RectD currentViewPort) {
        _MappingManager.set_currentViewPort(currentViewPort);
    }

    public void setLines(Lines lines) {
        _lines = lines;

        notifyDataChanged();
        postInvalidate();
    }

    public Lines getlines() {
        return _lines;
    }

    public MappingManager get_MappingManager() {
        return _MappingManager;
    }


    ////////////////////////////////  便捷的方法  //////////////////////////////////

    public void highLight_PixXY(float px, float py) {
        SingleD_XY xy = _MappingManager.getValueByPx(px, py);
        highLight_ValueXY(xy.getX(), xy.getY());
    }

    public void highLight_ValueXY(double x, double y) {
        _HighLightRender.highLight_ValueXY(x, y);
        invalidate();
    }

    public void highLightLeft() {
        _HighLightRender.highLightLeft();
        invalidate();
    }

    public void highLightRight() {
        _HighLightRender.highLightRight();
        invalidate();
    }

    public HightLight get_HightLight() {
        return _HightLight;
    }

    public void set_HightLight(HightLight _HightLight) {
        this._HightLight = _HightLight;
    }

    public XAxis get_XAxis() {
        return _XAxis;
    }

    public YAxis get_YAxis() {
        return _YAxis;
    }

    public RectF get_GodRect() {
        return _GodRect;
    }

    public void set_GodRect(RectF _GodRect) {
        this._GodRect = _GodRect;
    }

    public RectF get_MainPlotRect() {
        return _MainPlotRect;
    }

    public void set_MainPlotRect(RectF _MainPlotRect) {
        this._MainPlotRect = _MainPlotRect;
    }

    public ChartMode get_ChartMode() {
        return _ChartMode;
    }

    public LineChart set_ChartMode(ChartMode _ChartMode) {
        this._ChartMode = _ChartMode;
        return this;
    }

    LineChart _ob_linechart;

    public void registObserver(LineChart lineChartOb) {
        this._ob_linechart = lineChartOb;

        notifyDataChanged_FromOb(lineChartOb);
    }

    public void notifyDataChanged_FromOb(LineChart lineChartOb) {

        // x,y轴上的单位
        XAxis xAxis = lineChartOb.get_XAxis();
        this.get_XAxis().set_unit(xAxis.get_unit());

        YAxis yAxis = lineChartOb.get_YAxis();
        this.get_YAxis().set_unit(yAxis.get_unit());

        this.setLines(lineChartOb.getlines());
    }

    public void notifyOB_ViewportChanged(RectD _currentViewPort) {
        _ob_linechart.set_CurrentViewPort(_currentViewPort);
        _ob_linechart.invalidate();
    }

    public LineChart set_CurrentViewPort(RectD _currentViewPort) {
        _MappingManager.set_currentViewPort(_currentViewPort);
        return this;
    }


    public enum ChartMode {
        Normal, God
    }


}
