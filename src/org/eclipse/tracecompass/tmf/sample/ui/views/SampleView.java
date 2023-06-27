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
	private HashMap<String, Integer> syscallEvents = new HashMap<String, Integer>();
	
	public SampleView() {
		super(VIEW_ID);
	}
	
	@Override
	public void createPartControl(Composite parent) {
		TmfTraceManager traceManager = TmfTraceManager.getInstance();
        ITmfTrace trace = traceManager.getActiveTrace();
        if (trace != null) {
            traceSelected(new TmfTraceSelectedSignal(this, trace));
        }
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
		
		syscallEvents.clear();
		
		currentTrace = signal.getTrace();
		
		TmfEventRequest req = new TmfEventRequest(TmfEvent.class,
                TmfTimeRange.ETERNITY, 0, ITmfEventRequest.ALL_DATA,
                ITmfEventRequest.ExecutionType.BACKGROUND) {
			
			@Override
			public void handleData(ITmfEvent data) {
				// Called for each event
				super.handleData(data);
				
				String name = data.getName();
				if (name.startsWith("syscall_")) {
					// Record event name and call count
					Integer count = syscallEvents.get(name);
					syscallEvents.put(name,  count == null ? 1 : count + 1);
				}	
			}
			
			@Override
			public void handleSuccess() {
				// Request successful, not more data available
				super.handleSuccess();
				
				// Sort descending
				Comparator<Map.Entry<String, Integer>> comparator = new Comparator<Map.Entry<String, Integer>>() {
					@Override
					public int compare(Map.Entry<String, Integer> set1, Map.Entry<String, Integer> set2) {
						Integer value1 = set1.getValue();
						Integer value2 = set2.getValue();
						return value2.compareTo(value1);
					}
				};
				
				List<Map.Entry<String, Integer>> collection = new ArrayList<Map.Entry<String, Integer>>();
				for (Map.Entry<String, Integer> set : syscallEvents.entrySet()) {
					collection.add(set);
				}
				
				Collections.sort(collection, comparator);
				
				// TODO: Display this in a table in the plug-in instead of printing it.
				System.out.println("syscall: frequency");
				System.out.println("------------------");
				
				for (Map.Entry<String, Integer> set : collection) {
					System.out.println(set.getKey() + ": " + set.getValue());
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
		
		ITmfTrace trace = signal.getTrace();
		trace.sendRequest(req);
	}
}
