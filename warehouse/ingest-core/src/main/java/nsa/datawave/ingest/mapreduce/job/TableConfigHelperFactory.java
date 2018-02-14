package nsa.datawave.ingest.mapreduce.job;

import nsa.datawave.ingest.table.config.TableConfigHelper;
import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Logger;

/**
 * Creates {@link nsa.datawave.ingest.table.config.TableConfigHelper}s with optional Configuration overrides.
 */
public class TableConfigHelperFactory {
    
    /**
     * Creates a TableConfigHelper using the given Hadoop Configuration. Allows an optional Configuration override by using a "prefix".
     *
     * @param table
     *            The table to configure
     * @param conf
     *            The ingest configuration
     * @param log
     *            A logger to use
     * @return TableConfigHelper
     */
    public static TableConfigHelper create(String table, Configuration conf, Logger log) {
        String prop = table + TableConfigHelper.TABLE_CONFIG_CLASS_SUFFIX;
        String className = conf.get(prop);
        TableConfigHelper tableHelper = null;
        
        if (className != null) {
            try {
                Class<? extends TableConfigHelper> tableHelperClass = (Class<? extends TableConfigHelper>) Class.forName(className.trim());
                tableHelper = tableHelperClass.newInstance();
                
                String prefix = conf.get(table + TableConfigHelper.TABLE_CONFIG_PREFIX);
                
                if (prefix != null) {
                    conf = new OverridingConfiguration(prefix, conf);
                }
                
                tableHelper.setup(table, conf, log);
            } catch (Exception e) {
                throw new IllegalArgumentException("Could not create TableConfigHelper: " + className, e);
            }
        }
        
        return tableHelper;
    }
}
