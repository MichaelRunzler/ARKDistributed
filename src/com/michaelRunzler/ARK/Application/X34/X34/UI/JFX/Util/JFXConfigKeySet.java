package X34.UI.JFX.Util;

import X34.UI.JFX.Managers.X34UIRuleManager;
import X34.UI.JFX.X34UI;

/**
 * Contains all config keys used in the JFX UI. These keys should be used instead of string declarations or local variables
 * to address config settings when possible, to ensure class cross-compatibility and uniform config setting distribution.
 */
public class JFXConfigKeySet
{
    @Owner(X34UI.class)
    public static final String KEY_WINDOW_MODE = "window_mode";

    @Owner(X34UI.class)
    public static final String KEY_RULE_LIST = "rules";

    @Owner(X34UI.class)
    public static final String KEY_OUTPUT_DIR = "master_output_dir";

    @Owner(X34UIRuleManager.class)
    public static final String KEY_RULEDIR_MAP = "rule_dir_map";

    @Owner(X34UI.class)
    public static final String KEY_CONFIG_FILE = "config_file";

    @Owner(X34UIRuleManager.class)
    public static final String KEY_PROCESSOR_DIR = "processor_dir";

    @Owner(X34UIRuleManager.class)
    public static final String KEY_PROCESSOR_LIST = "external_processors";

    @Owner(X34UI.class)
    public static final String KEY_INDEX_DIR = "index_dir";

    @Owner(X34UI.class)
    public static final String KEY_LOGGING_DIR = "master_log_dir";

    @Owner(X34UI.class)
    public static final String KEY_OVERWRITE_EXISTING = "overwrite_existing_images";

    @Owner(JFXConfigKeySet.class)
    public static final long MAJOR_CONFIG_VERSION = 1000L;

    @Owner(X34UI.class)
    public static final long PROGRAM_VERSION = 3450;
}


