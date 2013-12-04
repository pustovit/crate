package org.cratedb.lucene.index.memory;

import org.apache.lucene.index.memory.ReusableMemoryIndex;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Simple {@link org.apache.lucene.index.memory.MemoryIndex} Pool that reuses MemoryIndex instance across threads and allows each of the
 * MemoryIndex instance to reuse its internal memory based on a user configured real-time value.
 */
public class MemoryIndexPool {
    private volatile BlockingQueue<ReusableMemoryIndex> memoryIndexQueue;

    // used to track the in-flight memoryIdx instances so we don't overallocate
    private int poolMaxSize = 10;
    private int poolCurrentSize;
    private volatile long bytesPerMemoryIndex;
    private ByteSizeValue maxMemorySize = new ByteSizeValue(1, ByteSizeUnit.MB); // only accessed in sync block
    private volatile TimeValue timeout = new TimeValue(100);

    public MemoryIndexPool() {
        this(10, new ByteSizeValue(1, ByteSizeUnit.MB), new TimeValue(100));
    }

    public MemoryIndexPool(int poolMaxSize, ByteSizeValue maxMemorySize, TimeValue timeout) {
        this.poolMaxSize = poolMaxSize;
        this.maxMemorySize = maxMemorySize;
        this.timeout = timeout;
        memoryIndexQueue = new ArrayBlockingQueue<ReusableMemoryIndex>(poolMaxSize);
        bytesPerMemoryIndex = maxMemorySize.bytes() / poolMaxSize;
    }

    public ReusableMemoryIndex acquire() {
        final BlockingQueue<ReusableMemoryIndex> queue = memoryIndexQueue;
        final ReusableMemoryIndex poll = queue.poll();
        return poll == null ? waitOrCreate(queue) : poll;
    }

    private ReusableMemoryIndex waitOrCreate(BlockingQueue<ReusableMemoryIndex> queue) {
        synchronized (this) {
            if (poolCurrentSize < poolMaxSize) {
                poolCurrentSize++;
                return new ReusableMemoryIndex(false, bytesPerMemoryIndex);

            }
        }
        ReusableMemoryIndex poll = null;
        try {
            final TimeValue timeout = this.timeout; // only read the volatile var once
            poll = queue.poll(timeout.getMillis(), TimeUnit.MILLISECONDS); // delay this by 100ms by default
        } catch (InterruptedException ie) {
            // don't swallow the interrupt
            Thread.currentThread().interrupt();
        }
        return poll == null ? new ReusableMemoryIndex(false, bytesPerMemoryIndex) : poll;
    }

    public void release(ReusableMemoryIndex index) {
        assert index != null : "can't release null reference";
        if (bytesPerMemoryIndex == index.getMaxReuseBytes()) {
            index.reset();
            // only put is back into the queue if the size fits - prune old settings on the fly
            memoryIndexQueue.offer(index);
        }
    }

}