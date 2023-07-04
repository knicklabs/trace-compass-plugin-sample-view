package org.eclipse.tracecompass.tmf.sample.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

public class SampleView extends TmfView {
	private static final String VIEW_ID = "org.eclipse.tracecompass.tmf.sample.ui.view";
	private ITmfTrace currentTrace;
	//private HashMap<String, Integer> syscallEvents = new HashMap<String, Integer>();
	
	private List<SysCallEvent> sysCallEvents = new ArrayList<SysCallEvent>();
	
	public SampleView() {
		super(VIEW_ID);
	}
	
	@Override
	public void createPartControl(Composite parent) {
		//TmfTraceManager traceManager = TmfTraceManager.getInstance();
        //ITmfTrace trace = traceManager.getActiveTrace();
        
        //if (trace != null) {
        //    traceSelected(new TmfTraceSelectedSignal(this, trace));
        //}
	}
	
	@Override
	public void setFocus() {
		// do nothing yet...
	}
	
	@TmfSignalHandler
	public void traceSelected(final TmfTraceSelectedSignal signal) {
		// Don't populate the view again if we're already showing this trace
		if (currentTrace == signal.getTrace()) {
			return;
		}
		
		sysCallEvents.clear();
		
		currentTrace = signal.getTrace();
		
		TmfEventRequest req = new TmfEventRequest(TmfEvent.class,
                TmfTimeRange.ETERNITY, 0, ITmfEventRequest.ALL_DATA,
                ITmfEventRequest.ExecutionType.BACKGROUND) {
			
			@Override
			public void handleData(ITmfEvent data) {
				// Called for each event
				super.handleData(data);
				
				String name = data.getName();
				Long tid = data.getContent().getFieldValue(Long.class, "context.cpu_id");
				Long timestamp = data.getTimestamp().toNanos();
				
				SysCallEvent.process(sysCallEvents, name, tid, timestamp);
			}
			
			@Override
			public void handleSuccess() {
				// Request successful, not more data available
				super.handleSuccess();
								
				for (SysCallEvent sysCallEvent : sysCallEvents) {
					System.out.println(sysCallEvent.toString());
				}
				
				// Update UI in the UI thread.
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						// TODO: Draw a table with the data that was printed out to the console.
					}
				});
			}
			
			@Override
			public void handleFailure() {
				// Request failed, not more data available
				super.handleFailure();
			}
		};
		
		currentTrace.sendRequest(req);
	}
}
