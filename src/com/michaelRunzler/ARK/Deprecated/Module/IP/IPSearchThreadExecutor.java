package com.michaelRunzler.ARK.Deprecated.Module.IP;

import java.util.ArrayList;

/**
 * Provides a convenient method of managing concurrent thread execution.
 * Includes utilities for thread stack management, as well as the ability to restrict the
 * number of concurrent threads executing at any one time.
 */
public class IPSearchThreadExecutor
{
    private ArrayList<IPSearchThread> stack;
    private int maxConcurrentCount;
    private boolean isExecuting;
    private int currentlyRunning;
    private int stackCounter;

    /**
     * Instantiates a new copy of this object with an empty stack counter.
     * @param maxConcurrentCount the maximum concurrent thread execution count for this object
     */
    public IPSearchThreadExecutor(int maxConcurrentCount)
    {
        stack = new ArrayList<>();
        setMaxConcurrentCount(maxConcurrentCount);
        isExecuting = false;
        currentlyRunning = 0;
        stackCounter = 0;
    }

    /**
     * Adds a thread to the end of the stack.
     * @param thread a Runnable containing the code to be run by the concurrency engine
     */
    public void addThreadToStack(Runnable thread)
    {
        if(thread == null) throw new IllegalArgumentException("Provided thread cannot be null!");

        stack.add(new IPSearchThread(thread, new IPSearchThreadNotifier() {
            @Override
            public void notifyThread() {
                // This thread has finished, it should no longer count towards the execution cap.
                currentlyRunning --;

                // Check if the stack should be starting new threads at this time, the stack counter is not at the end
                // of its range, and that it is not already at its execution cap. This should not happen, but it may if
                // another thread finishes during the time that this thread is checking its state. If any of these
                // conditions is failed, flag execution as finished, since no other threads will match these requirements either.
                if(isExecuting && currentlyRunning < maxConcurrentCount && currentlyRunning < stack.size() - 1 || stackCounter < stack.size() - 1){
                    // Start the next thread in the stack, and increment the running and stack access counters.
                    stack.get(stackCounter).startChildThread();
                    currentlyRunning ++;
                    stackCounter ++;
                }else{
                    // Although more threads might still be running, new threads will not be started, and thus stack execution
                    // has ended. Flag as such and return.
                    isExecuting = false;
                }
            }
        }));
    }

    /**
     * Removes a thread from the stack. Calling this method while the stack is executing will not take any action.
     * @param index the index of the thread to remove. If the provided index is not present in the stack, no action will be taken
     */
    public void removeThreadFromStack(int index){
        if(index >= stack.size() || isExecuting()){
            return;
        }
        stack.remove(index);
    }

    /**
     * Removes a thread from the stack. Calling this method while the stack is executing will not take any action.
     * @param thread the Runnable object correlating to the thread to be removed. If the specified object is not present
     *               in the stack, no action will be taken
     */
    public void removeThreadFromStack(Runnable thread){
        if(isExecuting()) return;

        for(int i = 0; i < stack.size(); i++)
        {
            IPSearchThread t = stack.get(i);
            if(t.child == thread){
                stack.remove(i);
                return;
            }
        }
    }

    /**
     * Clears the current stack. Calling this method while the stack is executing will not take any action.
     */
    public void clearStack() {
        if(isExecuting()) return;
        stack.clear();
    }

    /**
     * Adds multiple threads to the stack. Calling this method while the stack is executing will not take any action.
     * @param threads the list of Runnable objects to add to the stack. Any null objects will be ignored.
     */
    public void addMultipleThreadsToStack(Runnable... threads)
    {
        if(isExecuting() || threads == null || threads.length == 0) return;
        for(Runnable r : threads){
            if(r == null) continue;
            addThreadToStack(r);
        }
    }

    /**
     * Stops any further threads in the stack from executing. Any threads that are currently executing will finish and exit
     * normally. This method will not wait for currently executing threads to finish before returning. Calling this method
     * while there are no executing threads will do nothing.
     */
    public void stopStackExecution() {
        isExecuting = false;
        stackCounter = 0;
    }

    /**
     * Starts stack execution. If there are no threads in the stack, or if the stack is currently executing, no action will be taken.
     */
    public void startStackExecution()
    {
        if(isExecuting() || stack.size() == 0) return;

        isExecuting = true;
        // Use the stack size as the limit if the execution cap is bigger than the stack size to prevent array size exceptions.
        int stackStartLimit = maxConcurrentCount >= stack.size() ? stack.size() : maxConcurrentCount;
        // Start threads up to the stack start limit. Threads will start themselves after this point.
        for (int i = 0; i < stackStartLimit; i++){
            stack.get(i).startChildThread();
            currentlyRunning ++;
            stackCounter ++;
        }
    }

    /**
     * This method will block until the stack has finished execution. Concurrent calls to this method from other threads
     * will also block until previous instances of the method have finished execution.
     * @param checkDelay the delay between checking the state of the stack in milliseconds. Values less than 1 will result in an exception.
     */
    public synchronized void waitForStackCompletion(long checkDelay)
    {
        if(checkDelay <= 0) throw new IllegalArgumentException("Delay cannot be less than or equal to 0");
        while(isExecuting()){
            try {
                Thread.sleep(checkDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks to see if the stack is currently running threads or waiting for thread execution to stop.
     * @return whether or not the stack is currently active
     */
    public boolean isExecuting(){
        return isExecuting || currentlyRunning > 0;
    }

    /**
     * Sets the concurrency count for this object. Calling this method while the stack is executing will not take any action.
     * @param max the maximum number of concurrent threads that can be running at any given time from this object. Values
     *            below 1 will remove the restriction on thread execution.
     */
    public void setMaxConcurrentCount(int max) {
        if(isExecuting()) return;
        if(max <= 0) maxConcurrentCount = Integer.MAX_VALUE;
        else maxConcurrentCount = max;
    }

    /**
     * Gets the concurrency count for this object.
     * @return the maximum number of concurrent threads that can be running at any given time from this object
     */
    public int getMaxConcurrentCount() {
        return maxConcurrentCount;
    }
}

/**
 * Encapsulates a Runnable object and its associated Notifier signaling class in object format, and manages the execution
 * path for the encapsulated thread.
 */
class IPSearchThread
{
    Runnable child;
    IPSearchThreadNotifier notifier;

    /**
     * Instantiates a new instance of this encapsulation class.
     * @param child the child Runnable object that will be executed when the startChildThread() method is called
     * @param notifier the notifier object that will have its notifyThread() method called when the child finishes execution
     */
    IPSearchThread(Runnable child, IPSearchThreadNotifier notifier) {
        this.child = child;
        this.notifier = notifier;
    }

    /**
     * Starts the encapsulated thread. Once it has started, the object will wait for it to finish executing, and then call
     * the notifier's signaling method.
     */
    public void startChildThread()
    {
        // Extract the run tasks from the passed Thread, pass them to a new Thread alongside the Notifier task, and start the thread.
        // This allows the user to pass Thread objects to the Executor class instead of custom abstract objects, although
        // it makes execution a bit more complex.
        new Thread(() -> {
            child.run();
            notifier.notifyThread();
        }).start();
    }
}

/**
 * Handles cross-thread notifications for the IPSearch Executor and Thread classes.
 */
abstract class IPSearchThreadNotifier
{
    /**
     * Runs when the current thread has finished executing.
     */
    public abstract void notifyThread();
}
