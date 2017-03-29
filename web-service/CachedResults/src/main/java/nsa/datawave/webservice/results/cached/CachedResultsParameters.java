package nsa.datawave.webservice.results.cached;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import nsa.datawave.validation.ParameterValidator;
import nsa.datawave.webservice.query.QueryParameters;

import com.google.common.base.Preconditions;

public class CachedResultsParameters implements ParameterValidator {
    
    public static final String QUERY_ID = "queryId";
    public static final String ALIAS = "alias";
    public static final String VIEW = "view";
    public static final String FIELDS = "fields";
    public static final String CONDITIONS = "conditions";
    public static final String GROUPING = "grouping";
    public static final String ORDER = "order";
    public static final String FIXED_FIELDS_IN_EVENT = "fixedFields";
    
    private static final List<String> KNOWN_PARAMS = Arrays.asList(new String[] {QUERY_ID, ALIAS, VIEW, FIELDS, CONDITIONS, GROUPING, ORDER,
            FIXED_FIELDS_IN_EVENT, QueryParameters.QUERY_PAGESIZE});
    
    private String queryId = null;
    private String alias = null;
    private String view = null;
    private String fields = null;
    private String conditions = null;
    private String grouping = null;
    private String order = null;
    private String fixedFields = null;
    private int pagesize = 10;
    
    @Override
    public void validate(MultivaluedMap<String,String> parameters) throws IllegalArgumentException {
        for (String param : KNOWN_PARAMS) {
            List<String> values = parameters.get(param);
            if (null == values) {
                continue;
            }
            if (values.isEmpty() || values.size() > 1) {
                throw new IllegalArgumentException("Known parameter " + param + " only accepts one value");
            }
            if (QUERY_ID.equals(param)) {
                this.queryId = values.get(0);
            } else if (ALIAS.equals(param)) {
                this.alias = values.get(0);
            } else if (VIEW.equals(param)) {
                this.view = values.get(0);
            } else if (FIELDS.equals(param)) {
                this.fields = values.get(0);
            } else if (CONDITIONS.equals(param)) {
                this.conditions = values.get(0);
            } else if (GROUPING.equals(param)) {
                this.grouping = values.get(0);
            } else if (ORDER.equals(param)) {
                this.order = values.get(0);
            } else if (FIXED_FIELDS_IN_EVENT.equals(param)) {
                this.fixedFields = values.get(0);
            } else if (QueryParameters.QUERY_PAGESIZE.equals(param)) {
                this.pagesize = Integer.parseInt(values.get(0));
            } else {
                throw new IllegalArgumentException("Unknown condition.");
            }
        }
        Preconditions.checkNotNull(this.queryId, "Query id string cannot be null");
        Preconditions.checkNotNull(this.view, "View cannot be null");
        Preconditions.checkNotNull(this.fields, "Fields cannot be null");
    }
    
    public String getQueryId() {
        return queryId;
    }
    
    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }
    
    public String getAlias() {
        return alias;
    }
    
    public void setAlias(String alias) {
        this.alias = alias;
    }
    
    public String getView() {
        return view;
    }
    
    public void setView(String view) {
        this.view = view;
    }
    
    public String getFields() {
        return fields;
    }
    
    public void setFields(String fields) {
        this.fields = fields;
    }
    
    public String getConditions() {
        return conditions;
    }
    
    public void setConditions(String conditions) {
        this.conditions = conditions;
    }
    
    public String getGrouping() {
        return grouping;
    }
    
    public void setGrouping(String grouping) {
        this.grouping = grouping;
    }
    
    public String getOrder() {
        return order;
    }
    
    public void setOrder(String order) {
        this.order = order;
    }
    
    public String getFixedFields() {
        return fixedFields;
    }
    
    public void setFixedFields(String fixedFields) {
        this.fixedFields = fixedFields;
    }
    
    public int getPagesize() {
        return pagesize;
    }
    
    public void setPagesize(int pagesize) {
        this.pagesize = pagesize;
    }
    
    public void clear() {
        this.queryId = null;
        this.alias = null;
        this.view = null;
        this.fields = null;
        this.conditions = null;
        this.grouping = null;
        this.order = null;
        this.fixedFields = null;
        this.pagesize = 10;
    }
}