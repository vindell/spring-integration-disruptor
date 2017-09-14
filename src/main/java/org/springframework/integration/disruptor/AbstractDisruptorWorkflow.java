package org.springframework.integration.disruptor;

import java.util.List;
import java.util.concurrent.Executor;

import org.springframework.context.SmartLifecycle;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.disruptor.config.workflow.translator.MessageEventTranslator;
import org.springframework.util.Assert;

import com.lmax.disruptor.EventProcessor;
import com.lmax.disruptor.EventPublisher;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.RingBuffer;

public abstract class AbstractDisruptorWorkflow<T> extends IntegrationObjectSupport implements SmartLifecycle {

	private volatile boolean running = false;
	private volatile boolean autoStartup = true;
	private volatile int phase = 0;

	private final RingBuffer<T> ringBuffer;
	private final Executor executor;
	private final List<EventProcessor> eventProcessors;
	private final MessageEventTranslator<T> messageEventTranslator;
	private final EventPublisher<T> eventPublisher;

	AbstractDisruptorWorkflow(final RingBuffer<T> ringBuffer, final Executor executor, final List<EventProcessor> eventProcessors,
			final MessageEventTranslator<T> messageEventTranslator) {
		Assert.isTrue(ringBuffer != null, "RingBuffer can not be null");
		Assert.isTrue(executor != null, "Executor can not be null");
		Assert.isTrue(eventProcessors != null, "EventProcessors can not be null");
		Assert.isTrue(messageEventTranslator != null, "MessageEventTranslator can not be null");
		this.ringBuffer = ringBuffer;
		this.executor = executor;
		this.eventProcessors = eventProcessors;
		this.messageEventTranslator = messageEventTranslator;
		this.eventPublisher = new EventPublisher<T>(this.ringBuffer);
	}

	public final void start() {
		this.doStart();
		this.startEventProcessors();
		this.running = true;
	}

	protected void doStart() {
	}

	public final void stop() {
		this.running = false;
		this.stopEventProcessors();
		this.doStop();
	}

	protected void doStop() {
	}

	public final boolean isRunning() {
		return this.running;
	}

	public final int getPhase() {
		return this.phase;
	}

	public final boolean isAutoStartup() {
		return this.autoStartup;
	}

	public final void stop(final Runnable callback) {
		this.stop();
		callback.run();
	}

	public void setPhase(final int phase) {
		this.phase = phase;
	}

	public void setAutoStartup(final boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	private void startEventProcessors() {
		for (final EventProcessor eventProcessor : this.eventProcessors) {
			this.executor.execute(eventProcessor);
		}
	}

	private void stopEventProcessors() {
		for (final EventProcessor eventProcessor : this.eventProcessors) {
			eventProcessor.halt();
		}
	}

	protected final boolean publish(final Message<?> message) {
		try {
			this.eventPublisher.publishEvent(new EventTranslator<T>() {

				public void translateTo(final T event, final long sequence) {
					AbstractDisruptorWorkflow.this.messageEventTranslator.translateTo(message, event);
				}

			});
			return true;
		} catch (final Exception e) {
			this.logger.warn("Can't publish to RingBuffer", e);
			return false;
		}
	}

}
