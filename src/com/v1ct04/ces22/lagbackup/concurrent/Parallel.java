package com.v1ct04.ces22.lagbackup.concurrent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Parallel {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    private static final int MAX_NUM_THREADS = 4;

    public static  <Type> void forEach(Iterable<Type> iterable, Operation<Type> operation)
            throws Exception {
        List<Future> futures = new ArrayList<>(MAX_NUM_THREADS);
        ParallelForRunnable parallelForRunnable = new ParallelForRunnable<>(iterable, operation);
        for (int i = 0; i < MAX_NUM_THREADS; i++)
            futures.add(EXECUTOR_SERVICE.submit(parallelForRunnable));
        try {
            for (Future future : futures)
                future.get();
        } catch (InterruptedException e) {
            if (parallelForRunnable.mExceptionThrown == null)
                parallelForRunnable.mExceptionThrown = e;
            for (Future future : futures)
                future.get();
            throw parallelForRunnable.mExceptionThrown;
        }
    }

    public interface Operation<Type> {
        public void Do(Type type) throws Exception;
    }

    private static class ParallelForRunnable<Type> implements Runnable {

        private final Thread mOriginalThread;
        private final Iterator<Type> mIterator;
        private final Operation<Type> mOperation;

        private Exception mExceptionThrown = null;

        private ParallelForRunnable(Iterable<Type> iterable, Operation<Type> operation) {
            mOriginalThread = Thread.currentThread();
            mIterator = iterable.iterator();
            mOperation = operation;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Type obj;
                    synchronized (this) {
                        if (!mIterator.hasNext() || mExceptionThrown != null)
                            break;
                        obj = mIterator.next();
                    }
                    mOperation.Do(obj);
                }
            } catch (Exception e) {
                if (mExceptionThrown == null) {
                    mExceptionThrown = e;
                    mOriginalThread.interrupt();
                }
            }
        }
    }
}
