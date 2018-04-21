package core.CoreUtil.AUNIL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for keeping track of all registered {@link Callback}s from {@link XLoggerInterpreter}s, and relaying all
 * logged data to them. Only accessible to elements of the AUNIL core logging system.
 */
class XLoggerInputStream
{
    private Map<Callback, String> callbackRegistry;
    private Thread delegateThread;

    XLoggerInputStream(){
        callbackRegistry = new HashMap<>();
    }

    void addCallback(Callback cb, XLoggerInterpreter caller){
        callbackRegistry.put(cb, caller.classID);
    }

    // Only removes the callback if it was mapped to the calling interpreter class ID for security reasons
    void removeCallback(Callback cb, XLoggerInterpreter caller){
        callbackRegistry.remove(cb, caller.classID);
    }

    // IID is the Interpreter Class ID (not unique)
    @SuppressWarnings("unchecked")
    void logEventToStream(String event, String IID, LogEventLevel level, String compiled)
    {
        ArrayList<Callback> main = new ArrayList<>();
        ArrayList<Callback> delegate = new ArrayList<>();

        for(Callback cb : callbackRegistry.keySet()){
            if(cb.doMultiThread) delegate.add(cb);
            else main.add(cb);
        }

        // Call any flagged multithreaded callback objects on the delegate daemon thread
        if(delegate.size() > 0){
            delegateThread = new Thread(() -> {
                for(Callback cb : delegate){
                    cb.call(event, IID, level, compiled);
                }
            });

            delegateThread.setDaemon(true);
            delegateThread.start();
        }

        for(Callback cb : main){
            cb.call(event, IID, level, compiled);
        }
    }
}
