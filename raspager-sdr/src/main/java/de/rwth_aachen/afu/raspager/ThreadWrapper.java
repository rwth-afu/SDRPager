package de.rwth_aachen.afu.raspager;

final class ThreadWrapper<Job extends Runnable> extends Thread {
	private final Job job;

	public ThreadWrapper(Job job) {
		super(job);
		this.job = job;
	}

	public Job getJob() {
		return job;
	}
}
