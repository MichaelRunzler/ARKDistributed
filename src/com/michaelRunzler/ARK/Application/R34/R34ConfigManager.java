import system.ARKLogHandler;

import java.io.*;
import java.util.ArrayList;

/**
 * Manages config load and save operations for the R34UI class.
 */
public class R34ConfigManager
{
    //instance variables
    private File configG;
    private File configM;
    private File configA;
    private ARKLogHandler log;
    private boolean errorFlag = false;

    //general config
    private int opmode = 0;
    private File outputDir = new File(System.getProperty("user.home") + "\\Desktop");
    private boolean doMD5 = false;
    private boolean doFileLog = true;
    private boolean doLogTrim = true;
    private boolean doSkipDLPrompt;
    private boolean doMT = false;
    private File logFile = new File(System.getProperty("user.home") + "\\.R34Logs", "log.vcsl");

    private boolean doPreview = true;

    //manual config
    private int repoM = 0;
    private String tagM = "";
    private boolean pushM = true;

    //auto config
    private ArrayList<Rule> ruleList = new ArrayList<>();

    /**
     * Constructs a new instance of this Manager class.
     * @param configG the General config file
     * @param configM the Manual mode config file
     * @param configA the Auto mode config file
     * @param log an object implementing ARKLogHandler to be used for system event logging
     */
    public R34ConfigManager(File configG, File configM, File configA, ARKLogHandler log)
    {
        this.configG = configG;
        this.configM = configM;
        this.configA = configA;
        this.log = log;
    }

    /**
     * Reads all config options from log files. If an option cannot be loaded, it will be left at default.
     * @return true if all option loads succeeded, false if some failed
     * @throws IOException if there is a problem locating or reading one or more config files
     */
    public boolean readConfig() throws IOException
    {
        if(!configG.exists()){
            configG.getParentFile().mkdirs();
            configG.createNewFile();
            log.logEvent("Created new global config file!");
        }else{
            ObjectInputStream isG = new ObjectInputStream(new FileInputStream(configG));

            log.logEvent("Loading global config...");
            opmode = readConfigOption(opmode, isG);
            outputDir = readConfigOption(outputDir, isG);
            doMD5 = readConfigOption(doMD5, isG);
            doFileLog = readConfigOption(doFileLog, isG);
            doLogTrim = readConfigOption(doLogTrim, isG);
            doMT = readConfigOption(doMT, isG);
            doSkipDLPrompt = readConfigOption(doSkipDLPrompt, isG);
            logFile = readConfigOption(logFile, isG);
            doPreview = readConfigOption(doPreview, isG);

            isG.close();
        }

        if(!configM.exists()) {
            configM.getParentFile().mkdirs();
            configM.createNewFile();
            log.logEvent("Created new manual mode config file!");
        }else{
            ObjectInputStream isM = new ObjectInputStream(new FileInputStream(configM));

            log.logEvent("Loading manual mode config...");
            repoM = readConfigOption(repoM, isM);
            tagM = readConfigOption(tagM, isM);
            pushM = readConfigOption(pushM, isM);

            isM.close();
        }

        if(!configA.exists()) {
            configA.getParentFile().mkdirs();
            configA.createNewFile();
            log.logEvent("Created new auto mode config file!");
        }else {
            //byte[] bufA = ARKArrayUtil.charToByteArray(RetrievalTools.loadDataFromFile(configA).replace("com.michaelRunzler.ARK.retrieval.R34.Rule", "com.michaelRunzler.ARK.application.retrieval.R34.Rule").toCharArray());

            log.logEvent("Loading auto mode config...");
            ruleList = readLegacyAutoConfig(configA);
        }

        if(errorFlag){
            errorFlag = false;
            return true;
        }else{
            return false;
        }
    }

    /**
     * Clears any existing config files, then writes all currently stored config settings to them.
     * @return true if all config stores succeeded, false if some failed
     * @throws IOException if there is a problem deleting existing files or creating new ones
     */
    public boolean writeConfig() throws IOException
    {
        if(configG.exists())
            configG.delete();

        if(configM.exists())
            configM.delete();

        if(configA.exists())
            configA.delete();

        configG.createNewFile();
        configM.createNewFile();
        configA.createNewFile();

        ObjectOutputStream osG = new ObjectOutputStream(new FileOutputStream(configG));
        ObjectOutputStream osM = new ObjectOutputStream(new FileOutputStream(configM));
        ObjectOutputStream osA = new ObjectOutputStream(new FileOutputStream(configA));

        writeConfigOption(opmode, osG);
        writeConfigOption(outputDir, osG);
        writeConfigOption(doMD5, osG);
        writeConfigOption(doFileLog, osG);
        writeConfigOption(doLogTrim, osG);
        writeConfigOption(doMT, osG);
        writeConfigOption(doSkipDLPrompt, osG);
        writeConfigOption(logFile, osG);
        writeConfigOption(doPreview, osG);

        writeConfigOption(repoM, osM);
        writeConfigOption(tagM, osM);
        writeConfigOption(pushM, osM);

        writeConfigOption(ruleList, osA);

        osG.flush();
        osG.close();
        osM.flush();
        osM.close();
        osA.flush();
        osA.close();

        if(errorFlag){
            errorFlag = false;
            return true;
        }else{
            return false;
        }
    }

    /**
     * Reads a config option from the specified input stream.
     * @param value the value to read to
     * @param is the ObjectInputStream (usually containerizing a FileInputStream) to read from
     * @param <T> the type of object to read to. Primitive data types should be encapsulated in their object type
     * @return the config value from the file if the read was successful, the original value if not.
     */
    private <T> T readConfigOption(Object value, ObjectInputStream is)
    {
        T returnValue = (T) value;

        try{
            if(value instanceof Boolean){
                returnValue = (T)(Boolean)is.readBoolean();
            }else if(value instanceof Integer){
                returnValue = (T)(Integer)is.readInt();
            }else{
                returnValue = (T) is.readObject();
            }
        } catch (ClassNotFoundException | ClassCastException e){
            e.printStackTrace();
            log.logEvent("Config load encountered a class cast problem!\nSome options may be set to defaults.");
            errorFlag = true;
        } catch (IOException e){
            e.printStackTrace();
            log.logEvent("IO exception while loading from config file!\nSome options may be set to defaults.");
            errorFlag = true;
        }
        return returnValue;
    }

    /**
     * LEGACY METHOD: Loads an older auto-mode config file and converts it to usable form.
     * @return a usable Rule list
     */
    private ArrayList<Rule> readLegacyAutoConfig(File src){
        ArrayList<Rule> returnValue = new ArrayList<>();
        ObjectInputStream is;

        try {
            is = new ObjectInputStream(new FileInputStream(src));
            returnValue = readConfigOption(returnValue, is);
        } catch (IOException e1) {
            errorFlag = true;
            return null;
        }
        log.logEvent(returnValue.size() == 0 ? "Failed! Auto-mode config reset to defaults." : "Success! Loaded auto-mode config.");
        errorFlag = true;
        return returnValue;
    }

    /**
     * Writes a config option to the specified output stream.
     * @param value the value to write to the stream
     * @param os the ObjectOutputStream (usually containerizing a FileOutputStream) to write to
     */
    private void writeConfigOption(Object value, ObjectOutputStream os)
    {
        try {
            if(value instanceof Boolean) {
                os.writeBoolean((Boolean) value);
            }else if (value instanceof Integer) {
                os.writeInt((Integer) value);
            }else if (value instanceof Serializable) {
                    os.writeObject(value);
            }else {
                log.logEvent("Attempted to write non-serializable\nobject to config! Please report\nthis to the developer.");
                errorFlag = true;
            }
        }catch (IOException e){
            e.printStackTrace();
            log.logEvent("Config writer encountered an IO error!");
            errorFlag = true;
        }
    }

    //here comes the auto-generated code... I'm NOT javadoccing this.

    public int getOpmode() {
        return opmode;
    }

    public void setOpmode(int opmode) {
        this.opmode = opmode;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public boolean isDoMD5() {
        return doMD5;
    }

    public void setDoMD5(boolean doMD5) {
        this.doMD5 = doMD5;
    }

    public boolean isDoFileLog() {
        return doFileLog;
    }

    public void setDoFileLog(boolean doFileLog) {
        this.doFileLog = doFileLog;
    }

    public boolean isDoLogTrim() {
        return doLogTrim;
    }

    public void setDoLogTrim(boolean doLogTrim) {
        this.doLogTrim = doLogTrim;
    }

    public boolean isDoMT() {
        return doMT;
    }

    public void setDoMT(boolean doMT) {
        this.doMT = doMT;
    }

    public File getLogFile() {
        return logFile;
    }

    public void setLogFile(File logFile) {
        this.logFile = logFile;
    }

    public boolean isDoPreview() {
        return doPreview;
    }

    public void setDoPreview(boolean doPreview) {
        this.doPreview = doPreview;
    }

    public int getRepoM() {
        return repoM;
    }

    public void setRepoM(int repoM) {
        this.repoM = repoM;
    }

    public String getTagM() {
        return tagM;
    }

    public void setTagM(String tagM) {
        this.tagM = tagM;
    }

    public boolean isPushM() {
        return pushM;
    }

    public void setPushM(boolean pushM) {
        this.pushM = pushM;
    }

    public ArrayList<Rule> getRuleList() {
        return ruleList;
    }

    public void setRuleList(ArrayList<Rule> ruleList) {
        this.ruleList = ruleList;
    }

    public boolean isDoSkipDLPrompt() {
        return doSkipDLPrompt;
    }

    public void setDoSkipDLPrompt(boolean doSkipDLPrompt) {
        this.doSkipDLPrompt = doSkipDLPrompt;
    }
}
