package org.springframework.integration.disruptor.config.workflow;

import java.util.concurrent.Executor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;

import com.lmax.disruptor.ClaimStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;

abstract class AbstractDisruptorWorkflowFactoryBean<T> implements SmartLifecycle, BeanFactoryAware, InitializingBean {

	protected BeanFactory beanFactory;

	public void setBeanFactory(final BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	protected String executorName;

	public void setExecutorName(final String executorName) {
		this.executorName = executorName;
	}

	protected Class<T> eventType;

	public void setEventType(final Class<T> eventType) {
		this.eventType = eventType;
	}

	protected String eventFactoryName;

	public void setEventFactoryName(final String eventFactoryName) {
		this.eventFactoryName = eventFactoryName;
	}

	protected HandlerGroupDefinition handlerGroupDefinition;

	public void setHandlerGroupDefinition(final HandlerGroupDefinition handlerGroupDefinition) {
		this.handlerGroupDefinition = handlerGroupDefinition;
	}

	protected WaitStrategy waitStrategy;

	public void setWaitStrategy(final WaitStrategy waitStrategy) {
		this.waitStrategy = waitStrategy;
	}

	protected ClaimStrategy claimStrategy;

	public void setClaimStrategy(final ClaimStrategy claimStrategy) {
		this.claimStrategy = claimStrategy;
	}

	private RingBufferFactory<T> ringBufferFactory;
	private ExecutorFactory executorFactory;

	public final void afterPropertiesSet() throws Exception {
		this.ringBufferFactory = this.createRingBufferFactory();
		this.executorFactory = this.createExecutorFactory();
	}

	protected RingBuffer<T> createRingBuffer() {
		return this.ringBufferFactory.createRingBuffer();
	}

	private RingBufferFactory<T> createRingBufferFactory() {
		final RingBufferFactory<T> ringBufferFactory = new RingBufferFactory<T>();
		ringBufferFactory.setBeanFactory(this.beanFactory);
		ringBufferFactory.setEventFactoryName(this.eventFactoryName);
		ringBufferFactory.setEventType(this.eventType);
		ringBufferFactory.setHandlerGroupDefinition(this.handlerGroupDefinition);
		ringBufferFactory.setWaitStrategy(this.waitStrategy);
		ringBufferFactory.setClaimStrategy(this.claimStrategy);
		initialize(ringBufferFactory);
		return ringBufferFactory;
	}

	protected Executor createExecutor() {
		return this.executorFactory.createExecutorService();
	}

	private ExecutorFactory createExecutorFactory() {
		final ExecutorFactory executorFactory = new ExecutorFactory();
		executorFactory.setBeanFactory(this.beanFactory);
		executorFactory.setExecutorName(this.executorName);
		initialize(executorFactory);
		return executorFactory;
	}

	protected static void initialize(final Object object) {
		try {
			if (object instanceof InitializingBean) {
				((InitializingBean) object).afterPropertiesSet();
			}
		} catch (final Exception e) {
			throw new BeanCreationException("Exception while initializing: " + object, e);
		}
	}

	protected abstract SmartLifecycle getInstance();

	public final int getPhase() {
		return this.getInstance() != null ? this.getInstance().getPhase() : 0;
	}

	public final boolean isRunning() {
		return this.getInstance() != null ? this.getInstance().isRunning() : false;
	}

	public final void start() {
		if (this.getInstance() == null) {
			this.createInstance();
		}
		if (!this.isRunning()) {
			this.getInstance().start();
		}
	}

	protected abstract void createInstance();

	public final void stop() {
		if ((this.getInstance() != null) && this.isRunning()) {
			this.getInstance().stop();
		}
	}

	public final boolean isAutoStartup() {
		return this.getInstance() != null ? this.getInstance().isAutoStartup() : true;
	}

	public final void stop(final Runnable callback) {
		if ((this.getInstance() != null) && this.isRunning()) {
			this.getInstance().stop(callback);
		}
	}

}
