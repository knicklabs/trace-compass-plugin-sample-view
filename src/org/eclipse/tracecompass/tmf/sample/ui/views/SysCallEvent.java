package org.eclipse.tracecompass.tmf.sample.ui.views;

import java.util.Arrays;
import java.util.List;

public class SysCallEvent {
	// The name of the event.
	private String name;
	
	// The thread/cpu id of the event.
	private long tid;
	
	private int numEntry = 0;
	private int numExit = 0;
	
	private long tmpEntryTimestamp = 0;
	
	private long duration = 0;
	
	public static void process(List<SysCallEvent> sysCallEvents, String fullEventName, Long tid, Long timestamp) {	
		// Split the full event name into parts. At least 3 parts are expected.
		String[] nameParts = fullEventName.split("_");
		if (nameParts.length < 3) {
			return;
		}
		
		// The first part of the full event name is the event type. We are only
		// interested in syscall types.
		String eventType = nameParts[0];
		if (!eventType.equals("syscall")) {
			return;
		}
		
		// The second part of the full event name is the event moment. We are only
		// interested in entry and exit moments.
		String eventMoment = nameParts[1];
		if (!eventMoment.equals("entry") && !eventMoment.equals("exit")) {
			return;
		}
		
		// The remaining parts of the full event name can be concatenated into a
		// shortened event name.
		String eventName = String.join("_", Arrays.copyOfRange(nameParts, 2, nameParts.length - 1));
		
		SysCallEvent existingEvent = sysCallEvents.stream()
				.filter(event -> event.getName().equals(eventName) && event.getTid() == tid)
				.findAny()
				.orElse(null);
		
		if (existingEvent == null) {
			SysCallEvent newEvent = new SysCallEvent(eventName, eventMoment, tid, timestamp);
			sysCallEvents.add(newEvent);
		} else {
			existingEvent.increment(eventMoment, timestamp);
		}
	}
	
	public SysCallEvent(String name, String moment, long tid, long timestamp) {
		this.name = name;
		this.tid = tid;
		
		increment(moment, timestamp);
	}
	
	public void increment(String moment, long timestamp) {
		if (moment.equals("entry")) {
			numEntry++;
			tmpEntryTimestamp = timestamp;
		} else if (moment.equals("exit")) {
			numExit++;
			duration = duration + (timestamp - tmpEntryTimestamp);
		}
	}
	
	public String getName() {
		return name;
	}
	
	public long getTid() {
		return tid;
	}
	
	public int getNumCalls() {
		return numEntry;
	}
	
	public long getAverageDuration() {
		if (numExit == 0) return 0;
		
		return duration / numExit;
	}
	
	public String toString() {
		return name + "\t call: " + getNumCalls() + "x \t avg: " + getAverageDuration() + "ns";
	}
}

