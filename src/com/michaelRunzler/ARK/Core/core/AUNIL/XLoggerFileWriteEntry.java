package core.AUNIL;

import java.io.BufferedWriter;
import java.io.File;

/**
 * Data container for file association and linking data stored by the {@link XLoggerCore} class.
 */
class XLoggerFileWriteEntry
{
    BufferedWriter writer;
    File target;

    XLoggerFileWriteEntry(BufferedWriter br, File target){
        this.writer = br;
        this.target = target;
    }
}
