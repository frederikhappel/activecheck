package org.activecheck.plugin.reporter;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is the actual executor for ActivecheckReporters. It extends a
 * ScheduledThreadPoolExecutor with some logic to pause the execution and to
 * decrease the worker pool size.
 *
 * @since v1.2.0
 */
public class ActivecheckReporterExecutor extends ScheduledThreadPoolExecutor
        implements ActivecheckReporterExecutorMBean {
    public boolean isPaused;
    private final ReentrantLock pauseLock = new ReentrantLock();
    private final Condition unpaused = pauseLock.newCondition();
    private final Latch activeTasksLatch = new Latch();
    private final Semaphore terminations = new Semaphore(0);

    /**
     * A runtime exception used to prematurely terminate threads in this pool.
     */
    private static class ShutdownException extends RuntimeException {
        private static final long serialVersionUID = 2473973552846774645L;

        ShutdownException(String message) {
            super(message);
        }
    }

    /**
     * This uncaught exception handler is used only as threads are entered into
     * their shutdown state.
     */
    private static class ShutdownHandler implements Thread.UncaughtExceptionHandler {
        private final Thread.UncaughtExceptionHandler handler;

        /**
         * Create a new shutdown handler.
         *
         * @param handler
         *            The original handler to deligate non-shutdown exceptions
         *            to.
         */
        ShutdownHandler(Thread.UncaughtExceptionHandler handler) {
            this.handler = handler;
        }

        /**
         * Quietly ignore {@link ShutdownException}.
         * <p>
         * Do nothing if this is a ShutdownException, this is just to prevent
         * logging an uncaught exception which is expected. Otherwise forward it
         * to the thread group handler (which may hand it off to the default
         * uncaught exception handler).
         * </p>
         */
        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            if (!(throwable instanceof ShutdownException)) {
                /**
                 * Use the original exception handler if one is available,
                 * otherwise use the group exception handler.
                 */
                if (handler != null) {
                    handler.uncaughtException(thread, throwable);
                }
            }
        }
    }

    /**
     * Default constructor for a simple fixed threadpool
     */
    public ActivecheckReporterExecutor(final int corePoolSize) {
        super(corePoolSize, (Runnable runnable) -> {
            // Validate.isTrue(runnable instanceof NagiosReporter);
            final Thread t = new Thread(runnable);
            t.setName("NagiosReporter");
            t.setUncaughtExceptionHandler(new ShutdownHandler(t.getUncaughtExceptionHandler()));
            return t;
        });
    }

    @Override
    public void setCorePoolSize(final int size) {
        final int delta = getActiveCount() - size;

        super.setCorePoolSize(size);

        if (delta > 0) {
            terminations.release(delta);
        }
    }

    private class Latch {
        private final Object synchObj = new Object();
        private int count;

        public boolean awaitZero(final long waitMS) throws InterruptedException {
            final long startTime = System.currentTimeMillis();
            synchronized (synchObj) {
                while (count > 0) {
                    if (waitMS != 0) {
                        synchObj.wait(waitMS);
                        final long curTime = System.currentTimeMillis();
                        if ((curTime - startTime) > waitMS) {
                            return count <= 0;
                        }
                    } else
                        synchObj.wait();
                }
                return count <= 0;
            }
        }

        public void countDown() {
            synchronized (synchObj) {
                if (--count <= 0) {
                    // assert count >= 0;
                    synchObj.notifyAll();
                }
            }
        }

        public void countUp() {
            synchronized (synchObj) {
                count++;
            }
        }
    }

    /**
     * Executed before a task is assigned to a thread.
     */
    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        pauseLock.lock();
        try {
            while (isPaused) {
                unpaused.await();
            }
        } catch (InterruptedException ie) {
            t.interrupt();
        } finally {
            pauseLock.unlock();
        }
        activeTasksLatch.countUp();

        if (terminations.tryAcquire()) {
            /**
             * Replace this item in the queue so it may be executed by another
             * thread
             */
            getQueue().add(r);

            /**
             * Throwing a runtime exception is the only way to prematurely cause
             * a worker thread from the TheadPoolExecutor to exit.
             */
            throw new ShutdownException("Terminating thread " + t.getName());
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        try {
            super.afterExecute(r, t);
        } finally {
            activeTasksLatch.countDown();
        }
    }

    @Override
    public boolean isPaused() {
        return isPaused;
    }

    /**
     * Pause the thread pool. Running tasks will continue running, but new tasks
     * will not start until the thread pool is resumed.
     */
    @Override
    public String pause() {
        pauseLock.lock();
        try {
            isPaused = true;
        } finally {
            pauseLock.unlock();
        }
        return isPaused ? "successfully paused" : "failed to pause";
    }

    /**
     * Wait for all active tasks to end.
     */
    public boolean await(final long timeoutMS) {
        // assert isPaused;
        try {
            return activeTasksLatch.awaitZero(timeoutMS);
        } catch (InterruptedException e) {
            // log e, or rethrow maybe
        }
        return false;
    }

    /**
     * Resume the thread pool.
     */
    @Override
    public String resume() {
        pauseLock.lock();
        try {
            isPaused = false;
            unpaused.signalAll();
        } finally {
            pauseLock.unlock();
        }
        return !isPaused ? "successfully resumed" : "failed to resume";
    }
}