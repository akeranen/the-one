package input;

import java.util.ArrayList;

import core.Settings;

public class MessagePerNodeEventGenerator extends MessageEventGenerator {

	protected ArrayList<Integer> fromHosts;
	
	public MessagePerNodeEventGenerator(Settings s) {
		super(s);
		fromHosts = new ArrayList<Integer>();
		resetFromHosts(false);
	}
	
	@Override
	protected int drawHostAddress(int[] hostRange) {
		int host = fromHosts.remove(0);
		if (fromHosts.isEmpty()) {
			resetFromHosts(true);
		}
		return host;
	}
	
	
	private void resetFromHosts(boolean advanceTime) {
		fromHosts.clear();
		for (int i=super.hostRange[0]; i <= super.hostRange[1]; ++i) {
			fromHosts.add(i);
		}
		if (advanceTime) {
			this.nextEventsTime += drawNextEventTimeDiff();
		}
	}
	
	@Override
	public ExternalEvent nextEvent() {
		int responseSize = 0; /* zero stands for one way messages */
		int msgSize;
		int interval;
		int from;
		int to;

		/* Get two *different* nodes randomly from the host ranges */
		from = drawHostAddress(this.hostRange);
		to = drawToAddress(hostRange, from);

		msgSize = drawMessageSize();

		/* Create event */
		MessageCreateEvent mce = new MessageCreateEvent(from, to, this.getID(),
				msgSize, responseSize, this.nextEventsTime);

		if (this.msgTime != null && this.nextEventsTime > this.msgTime[1]) {
			/* next event would be later than the end time */
			this.nextEventsTime = Double.MAX_VALUE;
		}

		return mce;
	}


}
