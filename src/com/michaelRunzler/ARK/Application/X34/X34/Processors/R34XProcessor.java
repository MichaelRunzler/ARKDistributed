package X34.Processors;

import X34.Core.X34Image;
import X34.Core.X34Index;
import X34.Core.X34Schema;

import javax.xml.bind.ValidationException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Pulls image data from <a href="http://rule34.xxx/">Rule 34</a>.
 */
public class R34XProcessor extends X34RetrievalProcessor
{
    private String ID = "R34X";
    private String INF = "Rule 34";

    private ArrayList<X34Image> newImages;

    public R34XProcessor()
    {
        newImages = new ArrayList<>();
    }

    @Override
    public ArrayList<X34Image> process(X34Index index, X34Schema schema) throws IOException, ValidationException
    {
        if(!schema.validate()) throw new ValidationException("Schema failed to pass validation");



        return null;
    }

    @Override
    public ArrayList<X34Image> getNewImageList() {
        return newImages;
    }

    @Override
    public boolean validateIndex(X34Index index) throws IOException, ValidationException {
        return false;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String getInformalName() {
        return INF;
    }
}
