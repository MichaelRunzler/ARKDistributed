package X34.Core;

import java.util.ArrayList;

public class X34Index
{
    public String id;
    public ArrayList<X34Image> entries;

    public X34Index(String id)
    {
        this.id = id;
        this.entries = new ArrayList<>();
    }
}
