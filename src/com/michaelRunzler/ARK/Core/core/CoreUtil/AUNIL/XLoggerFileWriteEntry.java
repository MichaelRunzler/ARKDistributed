package core.CoreUtil.AUNIL;

import java.io.BufferedWriter;
import java.io.File;

/**
 * Data container for file association and linking data stored by the {@link XLoggerCore} class.
 */
class XLoggerFileWriteEntry
{
    BufferedWriter writer;
    File target;
    boolean writeToMaster;

    XLoggerFileWriteEntry(BufferedWriter br, File target, boolean writeToMaster){
        this.writer = br;
        this.target = target;
        this.writeToMaster = writeToMaster;
    }
}
