package org.alfresco.webdav.dsl;

import org.alfresco.utility.network.Jmx;
import org.alfresco.utility.network.JmxClient;
import org.alfresco.utility.network.JmxJolokiaProxyClient;
import org.alfresco.webdav.WebDavWrapper;

/**
 * DSL for interacting with JMX (using direct JMX call see {@link JmxClient} or {@link JmxJolokiaProxyClient}
 */
public class JmxUtil
{
    @SuppressWarnings("unused")
    private WebDavWrapper webDavProtocol;
    private Jmx jmx;

    public JmxUtil(WebDavWrapper webDavProtocol, Jmx jmx)
    {
        this.webDavProtocol = webDavProtocol;
        this.jmx = jmx;
    }

    public String getWebDavServerConfigurationStatus() throws Exception
    {
        return jmx.readProperty("Alfresco:Type=Configuration,Category=WebDav,id1=default", "Enabled").toString();
    }

    public boolean getSystemUsagesConfigurationStatus() throws Exception
    {
        return Boolean.parseBoolean(jmx.readProperty("Alfresco:Name=GlobalProperties", "system.usages.enabled").toString());
    }
}
