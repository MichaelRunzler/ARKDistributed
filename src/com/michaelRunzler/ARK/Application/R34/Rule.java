import java.io.File;
import java.io.Serializable;

/**
 * Container class for R34 automated retrieval rule definitions.
 */
public class Rule implements Serializable
{
    public int repo;
    public String tag;
    public boolean push;
    public String name;
    public File dest;

    public Rule(String name, int repo, String tag, boolean push, File dest){
        this.repo = repo;
        this.tag = tag;
        this.push = push;
        this.name = name;
        this.dest = dest;
    }
}
