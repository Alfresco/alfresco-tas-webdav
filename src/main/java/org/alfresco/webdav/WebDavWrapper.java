package org.alfresco.webdav;

import static org.alfresco.utility.Utility.checkObjectIsInitialized;
import static org.alfresco.utility.report.log.Step.STEP;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.util.List;

import org.alfresco.utility.TasProperties;
import org.alfresco.utility.Utility;
import org.alfresco.utility.dsl.DSLContentModelAction;
import org.alfresco.utility.dsl.DSLFile;
import org.alfresco.utility.dsl.DSLFolder;
import org.alfresco.utility.dsl.DSLProtocolWithNetworkDrive;
import org.alfresco.utility.exception.TestConfigurationException;
import org.alfresco.utility.exception.TestStepException;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.webdav.dsl.JmxUtil;
import org.alfresco.webdav.dsl.WebDavAssertion;
import org.alfresco.webdav.dsl.WebDavNetworkDrive;
import org.alfresco.webdav.dsl.WebDavUtil;
import org.alfresco.webdav.exception.MappedDriveException;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.LockMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.MoveMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.client.methods.UnLockMethod;
import org.apache.jackrabbit.webdav.lock.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Scope(value = "prototype")
public class WebDavWrapper extends DSLProtocolWithNetworkDrive<WebDavWrapper> implements DSLContentModelAction<WebDavWrapper>, DSLFolder<WebDavWrapper>,
        DSLFile<WebDavWrapper>
{
    @Autowired
    public TasProperties tasProperties;

    @Autowired
    protected WebDavNetworkDrive webDavNetworkDrive;

    private HttpClient client;
    public static String STEP_PREFIX = "WebDav:";
    public static String RESPONSE_HEADER_CONTENT_TYPE = "Content-Type";
    public static String RESPONSE_HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public int status;
    private String lockToken;
    private boolean overwrite;

    @Override
    public WebDavWrapper authenticateUser(UserModel userModel) throws Exception
    {
        STEP(String.format("%s Connect with %s/%s", STEP_PREFIX, userModel.getUsername(), userModel.getPassword()));
        client = null;
        HostConfiguration config = new HostConfiguration();
        config.setHost(tasProperties.getFullServerUrl());
        HttpConnectionManager manager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        int maxConnections = 20;
        params.setMaxConnectionsPerHost(config, maxConnections);
        manager.setParams(params);
        client = new HttpClient(manager);
        Credentials credentials = new UsernamePasswordCredentials(userModel.getUsername(), userModel.getPassword());
        client.getState().setCredentials(AuthScope.ANY, credentials);
        setTestUser(userModel);
        return this;
    }

    public synchronized HttpClient getHttpClient()
    {
        return client;
    }

    @Override
    public WebDavWrapper disconnect() throws Exception
    {
        client = new HttpClient();
        return this;
    }

    @Override
    public List<FileModel> getFiles() throws Exception
    {
        STEP(String.format("%s Get files from '%s'", WebDavWrapper.STEP_PREFIX, getCurrentSpace()));
        return withWebDavUtil().getFiles();
    }

    @Override
    public String buildPath(String parent, String... paths)
    {
        return Utility.buildPath(parent, paths);
    }

    @Override
    public WebDavWrapper createFolder(FolderModel folderModel) throws Exception
    {
        STEP(String.format("%s Create folder '%s'", STEP_PREFIX, folderModel.getName()));
        String webDavFolder = buildPath(getCurrentSpace(), folderModel.getName());
        if (isActionExecutedOnMappedDrive())
        {
            String currentLocation = getLastResource();
            setLastResource(webDavFolder);
            if (!withWebDavUtil().contentExists())
            {
                setLastResource(currentLocation);
                webDavNetworkDrive.inOSEnvironment().createFolder(buildPath(getLastResourceWithoutPrefix(), folderModel.getName()));
                setLastResource(webDavFolder);
                folderModel.setNodeRef(contentService
                        .getNodeRefByPath(getTestUser().getUsername(), getTestUser().getPassword(), getLastResourceWithoutPrefix()));
                folderModel.setCmisLocation(getLastResourceWithoutPrefix());
                folderModel.setProtocolLocation(webDavFolder);
            }
            else
                throw new FileAlreadyExistsException(webDavFolder);
        }
        else
        {
            String webdavFolderPath = withWebDavUtil().checkForWhiteSpace(webDavFolder);
            checkObjectIsInitialized(webdavFolderPath, "new folder");
            MkColMethod mkdir = new MkColMethod(webdavFolderPath);
            status = getHttpClient().executeMethod(mkdir);
            mkdir.releaseConnection();
            setLastResource(webDavFolder);
            if (HttpStatus.CREATED.value() == status)
            {
                folderModel.setNodeRef(contentService
                        .getNodeRefByPath(getTestUser().getUsername(), getTestUser().getPassword(), getLastResourceWithoutPrefix()));
                folderModel.setCmisLocation(getLastResourceWithoutPrefix());
                folderModel.setProtocolLocation(webDavFolder);
            }
        }
        return this;
    }

    @Override
    public List<FolderModel> getFolders() throws Exception
    {
        STEP(String.format("%s Get folders from '%s'", STEP_PREFIX, getCurrentSpace()));
        return withWebDavUtil().getFolders();
    }

    @Override
    public String getPrefixSpace()
    {
        String prefixSpace = "";

        if (isActionExecutedOnMappedDrive())
        {
            if (SystemUtils.IS_OS_WINDOWS)
                prefixSpace = "M:";
        }
        else
            prefixSpace = String.format("%s/alfresco/webdav", tasProperties.getFullServerUrl());

        return prefixSpace;
    }

    @Override
    protected String getProtocolJMXConfigurationStatus() throws Exception
    {
        return withJMX().getWebDavServerConfigurationStatus();
    }

    @Override
    public WebDavWrapper usingSite(String siteId) throws TestStepException
    {
        try
        {
            STEP(String.format("%s Navigate to site '%s/documentLibrary/'", STEP_PREFIX, siteId));
            checkObjectIsInitialized(siteId, "SiteID");
            if (isActionExecutedOnMappedDrive())
                setCurrentSpace(buildSiteDocumentLibraryPath(siteId));
            else
            {
                String path = buildSiteDocumentLibraryPath(siteId);
                DavMethod pFind = new PropFindMethod(path, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
                status = getHttpClient().executeMethod(pFind);
                pFind.getResponseBodyAsMultiStatus();
                pFind.releaseConnection();
                setCurrentSpace(path);
            }
        }
        catch (DavException e)
        {
            throw new TestStepException("Navigating to site should completed successfully", "Site was not found");
        }
        catch (Exception e)
        {
            throw new TestStepException("Navigating to site should completed successfully", e.getMessage());
        }
        return this;
    }

    @Override
    public WebDavWrapper usingSite(SiteModel siteModel) throws Exception
    {
        return usingSite(siteModel.getId());
    }

    @Override
    public WebDavWrapper usingUserHome(String username) throws Exception
    {
        STEP(String.format("%s Navigate to 'UserHomes/%s/'", STEP_PREFIX, username));
        checkObjectIsInitialized(username, "username");
        setCurrentSpace(buildUserHomePath(username, ""));
        return this;
    }

    @Override
    public WebDavWrapper usingUserHome() throws Exception
    {
        STEP(String.format("%s Navigate to 'UserHomes/'", STEP_PREFIX));
        checkObjectIsInitialized(getTestUser().getUsername(), "username");
        setCurrentSpace(buildUserHomePath(getTestUser().getUsername(), ""));
        return this;
    }

    @Override
    public WebDavWrapper usingRoot() throws Exception
    {
        STEP(String.format("%s Navigate to root ./", STEP_PREFIX));
        setCurrentSpace(getRootPath());
        return this;
    }

    @Override
    public String getRootPath() throws TestConfigurationException
    {
        return String.format("%s", getPrefixSpace());
    }

    @Override
    public String getSitesPath() throws TestConfigurationException
    {
        return String.format("%s/%s", getPrefixSpace(), "Sites");
    }

    @Override
    public String getUserHomesPath() throws TestConfigurationException
    {
        return String.format("%s/%s", getPrefixSpace(), "User Homes");
    }

    @Override
    public String getDataDictionaryPath() throws TestConfigurationException
    {
        return String.format("%s/%s", getPrefixSpace(), "Data Dictionary");
    }

    @Override
    public WebDavWrapper usingResource(ContentModel model) throws Exception
    {
        setLastContentModel(model);
        STEP(String.format("%s Navigate to '%s'", STEP_PREFIX, model.getName()));
        checkObjectIsInitialized(model, "contentName");
        if (model.getCmisLocation().equals(model.getName()))
        {
            setCurrentSpace(buildPath(getCurrentSpace(), model.getName()));
            setLastResource(getCurrentSpace());
        }
        else
            setCurrentSpace(buildPath(getPrefixSpace(), model.getCmisLocation()));
        return this;
    }

    @Override
    public WebDavWrapper rename(String newName) throws Exception
    {        
        String webDavSource = getLastResourceWithoutPrefix();
        File sourceFile = new File(webDavSource);
        String parent = Utility.convertBackslashToSlash(sourceFile.getParent());
        if (parent.equals("/"))
            parent = "";
        STEP(String.format("%s rename '%s' to '%s'", STEP_PREFIX, sourceFile.getName(), newName));
        if (isActionExecutedOnMappedDrive())
            webDavNetworkDrive.inOSEnvironment().renameContent(webDavSource, buildPath(parent, newName));
        else
        {
            String destinationNoSpaces = withWebDavUtil().checkForWhiteSpace(buildPath(getPrefixSpace(), parent, newName));
            webDavSource = withWebDavUtil().checkForWhiteSpace(buildPath(getPrefixSpace(), webDavSource));
            MoveMethod rename = new MoveMethod(webDavSource, destinationNoSpaces, false);
            status = getHttpClient().executeMethod(rename);
            rename.releaseConnection();
        }
        setLastResource(buildPath(getPrefixSpace(), parent, newName));
        getLastContentModel().setCmisLocation(buildPath(parent, newName));
        return this;
    }

    @Override
    public WebDavWrapper update(String content) throws Exception
    {
        String webDavResource = withWebDavUtil().checkForWhiteSpace(getLastResource());
        STEP(String.format("%s Update file '%s' with '%s'", STEP_PREFIX, webDavResource, content));
        checkObjectIsInitialized(webDavResource, "updating resource");
        if (isActionExecutedOnMappedDrive())
        {
            if (withWebDavUtil().contentExists())
                webDavNetworkDrive.inOSEnvironment().updateContent(getLastResourceWithoutPrefix(), new ByteArrayInputStream(content.getBytes()));
            else
                throw new FileNotFoundException();
        }
        else
        {
            if (!withWebDavUtil().contentExists())
                return this;

            PutMethod edit = new PutMethod(webDavResource);
            ByteArrayInputStream byteArray = new ByteArrayInputStream(content.getBytes());
            RequestEntity entity = new InputStreamRequestEntity(byteArray);
            edit.setRequestEntity(entity);
            status = getHttpClient().executeMethod(edit);
            byteArray.close();
            edit.releaseConnection();
        }
        return this;
    }

    @Override
    public WebDavWrapper delete() throws Exception
    {
        String webDavContent = withWebDavUtil().checkForWhiteSpace(getLastResource());
        STEP(String.format("%s Delete '%s'", STEP_PREFIX, webDavContent));
        checkObjectIsInitialized(webDavContent, "delete folder");
        if (isActionExecutedOnMappedDrive())
        {
            webDavNetworkDrive.inOSEnvironment().deleteContent(getLastResourceWithoutPrefix());
            dataContent.waitUntilContentIsDeleted(getLastResourceWithoutPrefix());
        }
        else
        {
            DeleteMethod delete = new DeleteMethod(webDavContent);
            status = getHttpClient().executeMethod(delete);
            delete.releaseConnection();
            try
            {
                DavMethod pFind = new PropFindMethod(webDavContent, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
                getHttpClient().executeMethod(pFind);
                pFind.getResponseBodyAsMultiStatus();
                pFind.releaseConnection();
            }
            catch (DavException ex)
            {
                if (status == HttpStatus.OK.value())
                {
                    dataContent.waitUntilContentIsDeleted(getLastResourceWithoutPrefix());
                }
            }
        }
        return this;
    }

    @Override
    public WebDavWrapper copyTo(ContentModel destinationModel) throws Exception
    {
        if (isActionExecutedOnMappedDrive())
        {
            File file = new File(getLastResource());
            if (file.isDirectory())
                webDavNetworkDrive.inOSEnvironment().copyFolder(getLastResourceWithoutPrefix(), destinationModel.getCmisLocation());
            else
                webDavNetworkDrive.inOSEnvironment().copyFile(getLastResourceWithoutPrefix(), destinationModel.getCmisLocation());
            return this;
        }
        else
            return withWebDavUtil().copyOrMove(destinationModel, true, overwrite);
    }

    /**
     * Upload a local file to webdav location
     * 
     * @param fileToUpload a local file stored on the disk
     * @return
     * @throws Exception
     */
    public WebDavWrapper uploadFile(File fileToUpload) throws Exception
    {
        String webDavResource = withWebDavUtil().checkForWhiteSpace(getLastResource());
        checkObjectIsInitialized(webDavResource, "uploading resource");
        STEP(String.format("%s Upload file '%s' to '%s'", STEP_PREFIX, fileToUpload.getAbsolutePath(), webDavResource));
        
        if (isActionExecutedOnMappedDrive())
        {
            throw new MappedDriveException("Real WebDav upload action CANNOT be executed on a mapped drive.");
        }
        String destinationRelativePath = buildPath(getLastResource(), fileToUpload.getName());
        setLastResource(destinationRelativePath);
        // PUT
        PutMethod uploadAction = new PutMethod(destinationRelativePath);
        status = getHttpClient().executeMethod(uploadAction);
        if (status != HttpStatus.CREATED.value())
        {
            throw new RuntimeException(String.format("Error uploading file %s. WebDAV PUT 1 method failed with code %d.", fileToUpload.getAbsolutePath(), status));
        }
        
        // LOCK
        lock();
        if (status != HttpStatus.OK.value())
        {
            throw new DavException(status, String.format("Error uploading file %s. WebDAV LOCK method failed with code %d.", fileToUpload.getAbsolutePath(), status));
        }

        // PUT
        uploadAction = new PutMethod(destinationRelativePath);
        RequestEntity requestEntity = new InputStreamRequestEntity(new FileInputStream(fileToUpload));
        uploadAction.setRequestEntity(requestEntity);
        uploadAction.setRequestHeader("If", String.format("(<%s>)", lockToken));
        status = getHttpClient().executeMethod(uploadAction);
        if (status != HttpStatus.NO_CONTENT.value())
        {
            throw new DavException(status, String.format("Error uploading file %s. WebDAV PUT 2 method failed with code %d.", fileToUpload.getAbsolutePath(), status));
        }

        // UNLOCK
        unlock();
        if (status != HttpStatus.NO_CONTENT.value())
        {
            throw new DavException(status, String.format("Error uploading file %s. WebDAV UNLOCK method failed with code %d.", fileToUpload.getAbsolutePath(), status));
        }
        return this;
    }

    @Override
    public WebDavWrapper moveTo(ContentModel destinationModel) throws Exception
    {
        if (isActionExecutedOnMappedDrive())
        {
            File file = new File(getLastResource());
            String destination = null;
            if (file.isDirectory())
            {
                destination = buildPath(destinationModel.getCmisLocation(), file.getName());
                webDavNetworkDrive.inOSEnvironment().moveFolder(getLastResourceWithoutPrefix(), destination);
            }
            else
            {
                String fileExtension = getLastResourceWithoutPrefix().substring(getLastResourceWithoutPrefix().lastIndexOf('.') + 1);
                destination = destinationModel.getCmisLocation() + "." + fileExtension;
                webDavNetworkDrive.inOSEnvironment().moveFile(getLastResourceWithoutPrefix(), destination);
            }
            setLastResource(buildPath(getPrefixSpace(), destination));
            return this;
        }
        else
            return withWebDavUtil().copyOrMove(destinationModel, false, overwrite);
    }

    public WebDavWrapper overwriteIfExists()
    {
        overwrite = true;
        return this;
    }

    public WebDavWrapper doNotOverwriteIfExists()
    {
        overwrite = false;
        return this;
    }

    @Override
    public WebDavWrapper createFile(FileModel fileModel) throws Exception
    {
        STEP(String.format("%s Create file '%s'", STEP_PREFIX, fileModel.getName()));
        String webDavFile = buildPath(getCurrentSpace(), fileModel.getName());
        FileInputStream fis = new FileInputStream(withWebDavUtil().setNewFile(fileModel));
        if (isActionExecutedOnMappedDrive())
        {
            String currentLocation = getLastResource();
            setLastResource(webDavFile);
            if (!withWebDavUtil().contentExists())
            {
                setLastResource(currentLocation);
                if (!fileModel.getContent().isEmpty())
                    webDavNetworkDrive.inOSEnvironment().createFile(buildPath(getLastResourceWithoutPrefix(), fileModel.getName()), fis);
                else
                    webDavNetworkDrive.inOSEnvironment().createFile(buildPath(getLastResourceWithoutPrefix(), fileModel.getName()));              
                setLastResource(webDavFile);
                fileModel.setNodeRef(contentService.getNodeRefByPath(getTestUser().getUsername(), getTestUser().getPassword(), getLastResourceWithoutPrefix()));
                fileModel.setCmisLocation(getLastResourceWithoutPrefix());
                fileModel.setProtocolLocation(webDavFile);
            }
           
            else
                throw new FileAlreadyExistsException(webDavFile);
        }
        else
        {
            String webDavFilePath = withWebDavUtil().checkForWhiteSpace(webDavFile);
            checkObjectIsInitialized(webDavFilePath, "create file");
            PutMethod put = new PutMethod(webDavFilePath);
            RequestEntity entity = new InputStreamRequestEntity(fis);
            put.setRequestEntity(entity);
            status = getHttpClient().executeMethod(put);
            put.releaseConnection();
            fis.close();
            setLastResource(webDavFile);
            if (HttpStatus.CREATED.value() == status)
            {
                fileModel.setCmisLocation(getLastResourceWithoutPrefix());
                fileModel.setNodeRef(contentService.getNodeRefByPath(getTestUser().getUsername(), getTestUser().getPassword(), getLastResourceWithoutPrefix()));
                fileModel.setProtocolLocation(webDavFile);
            }
        }
        return this;
    }

    /**
     * Assertion DSL. Call this method to see available assertions available {@link WebDavAssertion}
     */
    @Override
    public WebDavAssertion assertThat()
    {
        return new WebDavAssertion(this);
    }

    /**
     * @return JMX DSL for this wrapper
     */
    public JmxUtil withJMX()
    {
        return new JmxUtil(this, jmxBuilder.getJmxClient());
    }

    public WebDavUtil withWebDavUtil()
    {
        return new WebDavUtil(this);
    }

    /**
     * Download the last file resource in target folder. File is deleted after execution.
     * 
     * @return
     * @throws Exception
     */
    public WebDavWrapper download() throws Exception
    {
    	GetMethod getFile = getLastFileResource();
        status = getFileStatus(getFile);
        File responseFile = new File(System.getProperty("user.dir") + File.separator + "target" + File.separator + withWebDavUtil().getLastResourceName());
        responseFile.deleteOnExit();
        OutputStream outputStream = new FileOutputStream(responseFile);
        if (getFile.getResponseContentLength() > 0)
        {
            InputStream inputStream = getFile.getResponseBodyAsStream();
            byte buf[] = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0)
            {
                outputStream.write(buf, 0, len);
            }
            inputStream.close();
        }
        outputStream.close();
        getFile.releaseConnection();
        return this;
    }
    
    /**
     * Get the response header value for a certain header.
     * @param headerName
     * @return
     * @throws Exception
     */
    public String getResponseHeaderValue(String headerName) throws Exception
    {
    	GetMethod getFile = getLastFileResource();
        status = getFileStatus(getFile);
        if (getFile.getResponseHeader(headerName) != null)
        {
        	String value = getFile.getResponseHeader(headerName).getValue();
        	getFile.releaseConnection();
        	return value;
        }
        else
        {
        	throw new HttpResponseException(status, "Response status is " + status + " but the header is missing");
        }
        
    }
    
    /**
     * Get the last file resource request status from target folder.
     * @param getFile
     * @return
     * @throws Exception
     */
    private int getFileStatus(GetMethod getFile) throws Exception
    {
        return getHttpClient().executeMethod(getFile);
    }
    
    /**
     * Get the last file resource in target folder.
     * 
     * @return
     * @throws Exception
     */
    private GetMethod getLastFileResource() throws Exception
    {
        GetMethod getFile = new GetMethod(withWebDavUtil().checkForWhiteSpace(getLastResource()));
        return getFile;
    }
    
    /**
     * Lock file
     * 
     * @return
     * @throws Exception
     */
    public WebDavWrapper lock() throws Exception
    {
        lockToken = "";
        String lastResource = withWebDavUtil().checkForWhiteSpace(getLastResource());
        STEP(String.format("%s Lock file: %s", STEP_PREFIX, lastResource));
        LockMethod lockMethod = new LockMethod(lastResource, org.apache.jackrabbit.webdav.lock.Scope.EXCLUSIVE, Type.WRITE, getCurrentUser().getUsername(),
                600000l, true);
        status = getHttpClient().executeMethod(lockMethod);
        lockToken = lockMethod.getLockToken();
        return this;
    }

    /**
     * Unlock file
     * 
     * @return
     * @throws Exception
     */
    public WebDavWrapper unlock() throws Exception
    {
        String lastResource = withWebDavUtil().checkForWhiteSpace(getLastResource());
        if (!withWebDavUtil().isLocked())
        {
            lockToken = "";
        }
        STEP(String.format("%s Unlock file: %s", STEP_PREFIX, lastResource));
        UnLockMethod unlock = new UnLockMethod(lastResource, lockToken);
        status = getHttpClient().executeMethod(unlock);
        if (status == HttpStatus.NO_CONTENT.value())
        {
            lockToken = "";
        }
        return this;
    }

    @Override
    public WebDavWrapper usingNetworkDrive() throws Exception
    {
        STEP(String.format("%s map a drive", STEP_PREFIX));
        if (!webDavNetworkDrive.inOSEnvironment().isNetworkDriveMounted())
            webDavNetworkDrive.inOSEnvironment().mount();

        setCurrentSpace(webDavNetworkDrive.inOSEnvironment().getLocalVolumePath());
        setActionExecutedOnMappedDrive(true);
        return this;
    }

    public WebDavWrapper unmountNetworkDrive() throws Exception
    {
        STEP(String.format("WebDAV: unmount drive"));

        if (webDavNetworkDrive.inOSEnvironment().isNetworkDriveMounted())
        {
            webDavNetworkDrive.inOSEnvironment().unount();
        }

        return this;
    }
}
