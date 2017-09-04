package R34;

import javafx.concurrent.Service;

import java.util.ArrayList;

/**
 * Manages non-concurrent threads for the R34.R34 UI system.
 */
public class R34RetrievalThreadExecutor
{
    private ArrayList<Service> threads;
    private int counter;

    /**
     * Constructs a new executor object.
     */
    public R34RetrievalThreadExecutor() {
        threads = new ArrayList<>();
        counter = 0;
    }

    /**
     * Adds a new thread to the executor stack if the stack is not in the process of executing already.
     * @param svc the Service to add to the stack
     */
    public void registerThreadForExecution(Service svc) {
        if(verifyNoThreadExecution()){
            threads.add(svc);
        }
    }

    /**
     * Executes the next thread in the stack if all threads in the stack have not already been executed.
     * If they have, returns the stack counter to 0 so that the stack will restart on the next call.
     */
    public void executeNextThread()
    {
        if(counter < threads.size()){
            threads.get(counter).restart();
            counter ++;
        }else{
            counter = 0;
        }
    }

    /**
     * Gets the last thread executed by the executor.
     * @return the last Service started by the executor, or null if there are no threads in the stack or the stack counter is at 0
     */
    public synchronized Service getLastExecutedThread() {
        int ctr = counter; //ensures that the value does not change while evaluating expression
        return (threads.size() > 0 && ctr > 0) ? threads.get(ctr - 1) : null;
    }

    /**
     * Cancels the current execution stack and resets the stack counter.
     */
    public void cancelStackExecution()
    {
        if(!verifyNoThreadExecution()){
            threads.get(counter - 1).cancel();
        }

        counter = 0;
    }

    /**
     * Checks if the thread stack is currently in the process of executing.
     * @return true if the stack is currently idle, true if it is currently executing
     */
    public synchronized boolean verifyNoThreadExecution()
    {
        for(Service t : threads){
            if(t.isRunning()){
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the stack pointer is at its zero position. Note that this does not guarantee that all threads
     * have finished executing, but rather that the last thread in the stack has been <i>called</i>, and the
     * pointer has reset to zero. Also returns true if the stack contains no threads.
     * @return true if the stack pointer has reset, false if otherwise
     */
    public synchronized boolean checkThreadStackCounter() {
        return counter == threads.size();
    }

    /**
     * Clears the thread execution stack if the stack is idle.
     */
    public void clearThreadExecutionStack(){
        if(verifyNoThreadExecution()){
            threads.clear();
            counter = 0;
        }
    }

    /**
     * Gets the current position of the stack counter - note that this is not the currently executing thread, but
     * rather the index of the last thread to be <i>called from the stack.</i>
     * @return the current position of the stack counter
     */
    public int getStackCounterPosition()
    {
        return counter;
    }

    /**
     * Gets the current number of threads in the execution stack. Note that this does not give any information
     * about the number of executed threads - merely the total number of threads.
     * @return the current size of the execution stack
     */
    public int getStackSize()
    {
        return threads.size();
    }
}
