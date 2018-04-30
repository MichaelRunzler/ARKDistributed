package X34.UI.JFX.Util;

import X34.Core.X34Image;
import X34.Core.X34Rule;
import X34.Processors.X34RetrievalProcessor;

import java.util.ArrayList;

/**
 * Container-type object.
 */
public class RetrievalResultCache
{
    public String name;
    public ArrayList<X34Image> results;
    public X34Rule sourceRule;

    public RetrievalResultCache(X34Rule rule, ArrayList<X34Image> results) {
        this(rule.query, results, rule);
    }

    public RetrievalResultCache(String name, ArrayList<X34Image> results, X34Rule rule)
    {
        this.name = "" + (results == null ? 0 : results.size()) + ": " + name;
        this.results = results;
        this.sourceRule = rule;
    }

    public RetrievalResultCache(X34RetrievalProcessor repo, String tag, ArrayList<X34Image> results, X34Rule rule)
    {
        this.name = "" + (results == null ? 0 : results.size()) + ": " + tag + "/" + repo.getInformalName();
        this.results = results;
        this.sourceRule = rule;
    }
}
