package org.alfresco.webdav.dsl;


import org.alfresco.utility.TasProperties;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.exception.TestConfigurationException;
import org.alfresco.utility.network.NetworkDrive;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WebDavNetworkDrive
{
    @Autowired
    private DataUser dataUser;

    @Autowired
    private TasProperties tasProperties;

    /**
     * This will return the correct NetworkDrive class based on the current test operating system.
     *
     * @return
     * @throws TestConfigurationException
     */
    public NetworkDrive inOSEnvironment() throws TestConfigurationException
    {
        if(SystemUtils.IS_OS_WINDOWS)
            return winOS();

        throw new TestConfigurationException("NetworkDrive was not configure YET for this Operating System: " + SystemUtils.OS_NAME);
    }

    private NetworkDrive winOS()
    {
        String webDavNetworkServer = String.format("\\\\%s:%s\\alfresco\\webdav", tasProperties.getServer(), tasProperties.getPort());

        return new NetworkDrive(dataUser.getAdminUser(), webDavNetworkServer, "M:")
        {
            @Override
            protected void umountCode() throws Exception
            {
                /*
                 * net use * /d /y
                 */
                runCommand("net use * /d /y");
            }

            @Override
            protected void mountCode() throws Exception
            {
                /*
                 * net use M: \\localhost:8080\alfresco\webdav /user:user password
                 */
                runCommand("net use %s %s /user:%s %s /persistent:no",
                        getLocalVolumePath(),
                        getServerNetworkPath(),
                        getUserForNetworkAccess().getUsername(),
                        getUserForNetworkAccess().getPassword()
                );
            }
        };
    }
}
