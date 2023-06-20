package org.eclipse.tracecompass.tmf.sample.ui.views;

import java.sql.Date;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.Range;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTimestampFormatUpdateSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampFormat;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

public class SampleView extends TmfView {
	private static final String SERIES_NAME = "Series";
	private static final String Y_AXIS_TITLE = "Signal";
	private static final String X_AXIS_TITLE = "Time";
	private static final String FIELD = "value";
	private static final String VIEW_ID = "org.eclipse.tracecompass.tmf.sample.ui.view";
	private Chart chart;
	private ITmfTrace currentTrace;
	
	public SampleView() {
		super(VIEW_ID);
	}
	
	@Override
	public void createPartControl(Composite parent) {
		chart = new Chart(parent, SWT.BORDER);
		chart.getTitle().setVisible(false);
		chart.getAxisSet().getXAxis(0).getTitle().setText(X_AXIS_TITLE);
		chart.getAxisSet().getYAxis(0).getTitle().setText(Y_AXIS_TITLE);
		chart.getSeriesSet().createSeries(SeriesType.LINE, SERIES_NAME);
		chart.getLegend().setVisible(false);
		chart.getAxisSet().getXAxis(0).getTick().setFormat(new TmfChartTimeStampFormat());
		
		// The tutorial said to call getActiveTrace on this instance but it was wrong.
		// https://rtist.hcldoc.com/help/index.jsp?topic=%2Forg.eclipse.tracecompass.doc.dev%2Fdoc%2FView-Tutorial.html
		// The example here said to use the Trace Manager to get the active trace.
		// http://www.java2s.com/example/java-src/pkg/com/keba/tracecompass/jitter/ui/systemjitterview-7f1ca.html
		TmfTraceManager traceManager = TmfTraceManager.getInstance();
        ITmfTrace trace = traceManager.getActiveTrace();
        if (trace != null) {
            traceSelected(new TmfTraceSelectedSignal(this, trace));
        }
	}
	
	public class TmfChartTimeStampFormat extends SimpleDateFormat {
		private static final long serialVersionUID = 1L;
		public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
            long time = date.getTime();
            toAppendTo.append(TmfTimestampFormat.getDefaulTimeFormat().format(time));
            return toAppendTo;
        }
	}
	
	@TmfSignalHandler
	public void timestampFormatUpdated(TmfTimestampFormatUpdateSignal signal) {
		// Called when the time stamp preference is changed
		chart.getAxisSet().getXAxis(0).getTick().setFormat(new TmfChartTimeStampFormat());
		chart.redraw();
	}
	
	@Override
	public void setFocus() {
		chart.setFocus();
	}
	
	@TmfSignalHandler
	public void traceSelected(final TmfTraceSelectedSignal signal) {
		// Don't populate the view again if we're already showing this trace
		if (currentTrace == signal.getTrace()) {
			return;
		}
		
		currentTrace = signal.getTrace();
		
		TmfEventRequest req = new TmfEventRequest(TmfEvent.class,
                TmfTimeRange.ETERNITY, 0, ITmfEventRequest.ALL_DATA,
                ITmfEventRequest.ExecutionType.BACKGROUND) {
			
			ArrayList<Double> xValues = new ArrayList<Double>();
			ArrayList<Double> yValues = new ArrayList<Double>();
			private double maxY = -Double.MAX_VALUE;
			private double minY = Double.MAX_VALUE;
			private double maxX = -Double.MAX_VALUE;
			private double minX = Double.MAX_VALUE;
			
			@Override
			public void handleData(ITmfEvent data) {
				// Called for each event
				super.handleData(data);
				ITmfEventField field = data.getContent().getField(FIELD);
				if (field != null) {
					Double yValue = (Double) field.getValue();
					minY = Math.min(minY,  yValue);
					maxY = Math.max(maxY,  yValue);
					yValues.add(yValue);
					
					double xValue = (double) data.getTimestamp().getValue();
					minX = Math.min(minX,  xValue);
					maxX = Math.max(maxX,  xValue);
					xValues.add(xValue);
				}
			}
			
			@Override
			public void handleSuccess() {
				// Request successful, not more data available
				super.handleSuccess();
				
				final double x[] = toArray(xValues);
				final double y[] = toArray(yValues);
				
				// This part needs to run on the UI thread since it updates the chart
				// SWT control.
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						chart.getSeriesSet().getSeries()[0].setXSeries(x);
						chart.getSeriesSet().getSeries()[0].setYSeries(y);
						
						// Set the new range
						if (!xValues.isEmpty() && !yValues.isEmpty()) {
							chart.getAxisSet().getXAxis(0).setRange(new Range(0, x[x.length - 1]));
							chart.getAxisSet().getYAxis(0).setRange(new Range(minY, maxY));
						} else {
							chart.getAxisSet().getXAxis(0).setRange(new Range(0, 1));
							chart.getAxisSet().getYAxis(0).setRange(new Range(0, 1));
						}
						chart.getAxisSet().adjustRange();
						
						chart.redraw();
					}
				});
			}
			
			@Override
			public void handleFailure() {
				// Request failed, not more data available
				super.handleFailure();
			}
			
			private double[] toArray(List<Double> list) {
				double[] d = new double[list.size()];
				for (int i = 0; i < list.size(); ++i) {
					d[i] = list.get(i);
				}
				return d;
			}
		};
		
		ITmfTrace trace = signal.getTrace();
		trace.sendRequest(req);
	}
}
