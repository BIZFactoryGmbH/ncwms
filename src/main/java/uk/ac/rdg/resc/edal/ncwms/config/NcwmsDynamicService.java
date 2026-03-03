package uk.ac.rdg.resc.edal.ncwms.config;

import java.util.regex.Pattern;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 * A dynamic dataset object in the ncWMS configuration system: This object links
 * a dynamic location (local or remote) to the ncWMS system. Once a dynamic
 * dataset is added to the configuration ncWMS will attempt to service any
 * incoming WMS request whose URL resolves to the alias of the service plus a
 * matching (via regex) path, using the dynamically-generated dataset as the
 * source of (meta)data.
 *
 * @author Nathan D Potter
 * @author Guy Griffiths
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class NcwmsDynamicService {
    /* Alias for this service */
    @XmlAttribute(name = "alias", required = true)
    private String alias;

    /*
     * Either a URL or a local path representing the entry point for a dynamic
     * service
     */
    @XmlAttribute(name = "servicePath", required = true)
    private String path;

    /*
     * A regular expression used to match requested datasets with datasets on
     * the dynamic service
     */
    @XmlAttribute(name = "datasetIdMatch", required = true)
    private String datasetIdMatch;

    /*
     * We'll use a default data readerunless this is overridden in the config
     * file
     */
    @XmlAttribute(name = "dataReaderClass", required = false)
    private String dataReaderClass = "";

    @XmlAttribute(name = "copyrightStatement", required = false)
    private String copyrightStatement = "";

    @XmlAttribute(name = "moreInfoUrl", required = false)
    private String moreInfo = "";

    /* Set true to disable the dataset without removing it completely */
    @XmlAttribute(name = "disabled", required = false)
    private boolean disabled = false;

    @XmlAttribute(name = "queryable", required = false)
    private boolean queryable;

    @XmlAttribute(name = "downloadable", required = false)
    private boolean downloadable;

    @XmlTransient
    private Pattern idMatchPattern;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getServicePath() {
        return path;
    }

    public void setServicePath(String servicePath) {
        this.path = servicePath.trim();
    }

    /** Maximum length for dataset ID match regex to limit ReDoS attack surface. */
    private static final int MAX_REGEX_LENGTH = 500;

    public String getDatasetIdMatch() {
        return datasetIdMatch;
    }

    public void setDatasetIdMatch(String datasetIdMatch) {
        String trimmed = datasetIdMatch.trim();
        if (trimmed.length() > MAX_REGEX_LENGTH) {
            throw new IllegalArgumentException(
                    "datasetIdMatch regex exceeds maximum length of " + MAX_REGEX_LENGTH + " characters");
        }
        this.datasetIdMatch = trimmed;
        try {
            idMatchPattern = Pattern.compile(trimmed);
        } catch (java.util.regex.PatternSyntaxException e) {
            throw new IllegalArgumentException(
                    "Invalid regex for datasetIdMatch: " + e.getDescription(), e);
        }
    }

    public Pattern getIdMatchPattern() {
        if (idMatchPattern == null && datasetIdMatch != null) {
            // Re-apply the same guards as setDatasetIdMatch() to handle
            // patterns loaded from XML config (JAXB bypasses the setter).
            if (datasetIdMatch.length() > MAX_REGEX_LENGTH) {
                throw new IllegalStateException(
                        "datasetIdMatch regex exceeds maximum length of " + MAX_REGEX_LENGTH + " characters");
            }
            try {
                idMatchPattern = Pattern.compile(datasetIdMatch);
            } catch (java.util.regex.PatternSyntaxException e) {
                throw new IllegalStateException(
                        "Invalid regex for datasetIdMatch: " + e.getDescription(), e);
            }
        }
        return idMatchPattern;
    }

    public String getDataReaderClass() {
        return dataReaderClass;
    }

    public void setDataReaderClass(String dataReaderClass) {
        this.dataReaderClass = dataReaderClass.trim();
    }

    public String getCopyrightStatement() {
        return copyrightStatement;
    }

    public void setCopyrightStatement(String copyrightStatement) {
        this.copyrightStatement = copyrightStatement.trim();
    }

    public String getMoreInfo() {
        return moreInfo;
    }

    public void setMoreInfo(String moreInfo) {
        this.moreInfo = moreInfo.trim();
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isDownloadable() {
        return downloadable;
    }

    public void setDownloadable(boolean downloadable) {
        this.downloadable = downloadable;
    }

    public boolean isQueryable() {
        return queryable;
    }

    public void setQueryable(boolean queryable) {
        this.queryable = queryable;
    }
}
