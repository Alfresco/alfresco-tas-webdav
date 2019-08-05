package org.alfresco.webdav.dsl;

import static org.alfresco.utility.Utility.checkObjectIsInitialized;
import static org.alfresco.utility.report.log.Step.STEP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.webdav.WebDavWrapper;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.CopyMethod;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.MoveMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.util.EncodeUtil;

public class WebDavUtil
{
    private WebDavWrapper webDavWrapper;

    public WebDavUtil(WebDavWrapper webDavWrapper)
    {
        this.webDavWrapper = webDavWrapper;
    }

    public File setNewFile(FileModel fileModel) throws Exception
    {
        File newFile = new File(fileModel.getName());
        newFile.createNewFile();
        if (!StringUtils.isEmpty(fileModel.getContent()))
        {
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(newFile), Charset.forName("UTF-8").newEncoder());
            writer.write(fileModel.getContent());
            writer.close();
        }
        newFile.deleteOnExit();
        return newFile;
    }

    protected boolean isFolderInList(FolderModel folderModel, List<FolderModel> folders)
    {
        for (FolderModel folder : folders)
        {
            if (folderModel.getName().equals(folder.getName()))
            {
                return true;
            }
        }
        return false;
    }

    protected boolean isFileInList(FileModel fileModel, List<FileModel> files)
    {
        for (FileModel file : files)
        {
            if (fileModel.getName().equals(file.getName()))
            {
                return true;
            }
        }
        return false;
    }

    protected boolean isContentInList(ContentModel contentModel, List<ContentModel> contents)
    {
        for (ContentModel content : contents)
        {
            if (content.getName().equals(content.getName()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a list of folders from current location
     * 
     * @return List<FolderModel>
     * @throws Exception
     */
    public List<FolderModel> getFolders() throws Exception
    {
        List<FolderModel> children = new ArrayList<>();
        if (webDavWrapper.isActionExecutedOnMappedDrive())
        {
            try (Stream<Path> paths = Files.walk(Paths.get(webDavWrapper.getLastResource())))
            {
                paths.forEach(filePath -> {
                    if (!Files.isRegularFile(filePath))
                    {
                        FolderModel content = new FolderModel(new File(filePath.toString()).getName());
                        content.setCmisLocation(filePath.toString());
                        children.add(content);
                    }
                });
            }
        }
        else
        {
            String currentFolder;
            String path = webDavWrapper.getCurrentSpace().replace(webDavWrapper.tasProperties.getFullServerUrl(), "");
            MultiStatusResponse[] responses = getChildrenResponse();
            for (MultiStatusResponse response : responses)
            {
                currentFolder = EncodeUtil.unescape(response.getHref());
                if (!(currentFolder.equals(path) || currentFolder.equals(path + "/")))
                {
                    if (isFolder(response))
                    {
                        FolderModel content = new FolderModel(new File(currentFolder).getName());
                        content.setCmisLocation(currentFolder.replace("/alfresco/webdav", ""));
                        children.add(content);
                    }
                }
            }
        }
        return children;
    }

    /**
     * Get a list of files from current location
     * 
     * @return List<FileModel>
     * @throws Exception
     */
    public List<FileModel> getFiles() throws Exception
    {
        List<FileModel> children = new ArrayList<>();
        if (webDavWrapper.isActionExecutedOnMappedDrive())
        {
            try (Stream<Path> paths = Files.walk(Paths.get(webDavWrapper.getLastResource())))
            {
                paths.forEach(filePath -> {
                    if (Files.isRegularFile(filePath))
                    {
                        FileModel content = new FileModel(new File(filePath.toString()).getName());
                        content.setCmisLocation(filePath.toString());
                        children.add(content);
                    }
                });
            }
        }
        else
        {
            String currentContent;
            String path = webDavWrapper.getCurrentSpace().replace(webDavWrapper.tasProperties.getFullServerUrl(), "");
            MultiStatusResponse[] responses = getChildrenResponse();
            for (MultiStatusResponse response : responses)
            {
                currentContent = EncodeUtil.unescape(response.getHref());
                if (!(currentContent.equals(path) || currentContent.equals(path + "/")))
                {
                    if (!isFolder(response))
                    {
                        FileModel content = new FileModel(new File(currentContent).getName());
                        content.setCmisLocation(currentContent.replace("/alfresco/webdav", ""));
                        children.add(content);
                    }
                }
            }
        }
        return children;
    }

    /**
     * Get a list of contents (file and folders) from current location
     * 
     * @return List<FolderModel>
     * @throws Exception
     */
    public List<ContentModel> getChildren() throws Exception
    {
        List<ContentModel> children = new ArrayList<>();
        if (webDavWrapper.isActionExecutedOnMappedDrive())
        {
            try (Stream<Path> paths = Files.walk(Paths.get(webDavWrapper.getLastResource())))
            {
                paths.forEach(filePath -> {
                    ContentModel content = new ContentModel(new File(filePath.toString()).getName());
                    content.setCmisLocation(filePath.toString());
                    children.add(content);
                });
            }
        }
        else
        {
            String currentContent;
            String path = webDavWrapper.getCurrentSpace().replace(webDavWrapper.tasProperties.getFullServerUrl(), "");
            MultiStatusResponse[] responses = getChildrenResponse();
            for (MultiStatusResponse response : responses)
            {
                currentContent = EncodeUtil.unescape(response.getHref());
                if (!(currentContent.equals(path) || currentContent.equals(path + "/")))
                {
                    ContentModel content = new ContentModel(new File(currentContent).getName());
                    content.setCmisLocation(currentContent.replace("/alfresco/webdav", ""));
                    children.add(content);
                }
            }
        }
        return children;
    }

    private MultiStatusResponse[] getChildrenResponse() throws Exception
    {
        DavMethod pFind = new PropFindMethod(webDavWrapper.getCurrentSpace(), DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
        webDavWrapper.getHttpClient().executeMethod(pFind);
        MultiStatus multiStatus = pFind.getResponseBodyAsMultiStatus();
        MultiStatusResponse[] responses = multiStatus.getResponses();
        pFind.releaseConnection();
        return responses;
    }

    private boolean isFolder(MultiStatusResponse response)
    {
        DavPropertySet properties = response.getProperties(200);
        boolean isfolder = false;
        String resourcetype = null;
        DavProperty<?> resourceType = properties.get("resourcetype");
        if ((resourceType != null) && (resourceType.getValue() != null))
        {
            resourcetype = resourceType.getValue().toString();
        }
        if ((resourcetype != null) && (resourcetype.indexOf("collection") != -1))
        {
            isfolder = true;
        }
        return isfolder;
    }

    public String checkForWhiteSpace(String content)
    {
        if (content.contains(" "))
        {
            content = content.replace(" ", "%20");
        }
        return content;
    }

    protected synchronized int getStatus()
    {
        return webDavWrapper.status;
    }

    public WebDavWrapper copyOrMove(ContentModel destinationModel, boolean copy, boolean overwrite) throws Exception
    {
        String webDavSource = webDavWrapper.getLastResource();
        String webDavSourceName = new File(webDavSource).getName();
        String destinationPath = destinationModel.getCmisLocation();
        destinationPath = webDavWrapper.buildPath(webDavWrapper.getPrefixSpace(), destinationModel.getCmisLocation());
        checkObjectIsInitialized(webDavSource, "source");
        checkObjectIsInitialized(destinationPath, "destination");
        String destination = webDavWrapper.buildPath(destinationPath, webDavSourceName);
        String webDavDestination = webDavWrapper.withWebDavUtil().checkForWhiteSpace(destination);
        webDavSource = webDavWrapper.withWebDavUtil().checkForWhiteSpace(webDavSource);
        DavMethod action;
        if (copy)
        {
            STEP(String.format("%s Copy '%s' to '%s'", WebDavWrapper.STEP_PREFIX, webDavSourceName, destinationPath));
            action = new CopyMethod(webDavSource, webDavDestination, overwrite);
        }
        else
        {
            STEP(String.format("%s Move '%s' to '%s'", WebDavWrapper.STEP_PREFIX, webDavSourceName, destinationPath));
            action = new MoveMethod(webDavSource, webDavDestination, overwrite);
        }
        webDavWrapper.status = webDavWrapper.getHttpClient().executeMethod(action);
        action.releaseConnection();
        webDavWrapper.setLastResource(destination);
        return webDavWrapper;
    }
    
    /**
     * Get the content from a file
     * 
     * @return
     * @throws Exception
     */
    protected String getContent() throws Exception
    {
        StringBuilder content = new StringBuilder();
        String inputLine;
        FileInputStream fileInputStream = null;
        InputStream inputStream;
        GetMethod getMethod = null;
        if (webDavWrapper.isActionExecutedOnMappedDrive())
        {
            fileInputStream = new FileInputStream(webDavWrapper.getLastResource());
            inputStream = fileInputStream;
        }
        else
        {
            getMethod = new GetMethod(checkForWhiteSpace(webDavWrapper.getLastResource()));
            webDavWrapper.getHttpClient().executeMethod(getMethod);
            inputStream = getMethod.getResponseBodyAsStream();
        }
        InputStreamReader inputReader = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(inputReader);
        while ((inputLine = reader.readLine()) != null)
            content.append(inputLine);
        reader.close();
        inputStream.close();
        inputReader.close();
        if (webDavWrapper.isActionExecutedOnMappedDrive())
            fileInputStream.close();
        else
            getMethod.releaseConnection();
        return content.toString();
    }

    /**
     * Verify if content (file or folder) exists
     * 
     * @return
     * @throws Exception
     */
    public boolean contentExists() throws Exception
    {
        if (webDavWrapper.isActionExecutedOnMappedDrive())
            return Files.exists(Paths.get(webDavWrapper.getLastResource()));
        else
        {
            GetMethod pFind = new GetMethod(checkForWhiteSpace(webDavWrapper.getLastResource()));
            webDavWrapper.status = webDavWrapper.getHttpClient().executeMethod(pFind);
            pFind.releaseConnection();
            return webDavWrapper.status == HttpStatus.SC_OK;
        }
    }

    public String getLastResourceName()
    {
        return new File(webDavWrapper.getLastResource()).getName();
    }

    public boolean isLocked() throws Exception
    {
        DavMethod pFind = new PropFindMethod(checkForWhiteSpace(webDavWrapper.getLastResource()), DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
        webDavWrapper.status = webDavWrapper.getHttpClient().executeMethod(pFind);
        MultiStatusResponse[] responses;
        try
        {
            MultiStatus multiStatus = pFind.getResponseBodyAsMultiStatus();
            responses = multiStatus.getResponses();
        }
        catch (DavException e)
        {
            return false;
        }
        pFind.releaseConnection();
        DavProperty<?> pLockDiscovery = responses[0].getProperties(200).get(DavConstants.PROPERTY_LOCKDISCOVERY);
        return pLockDiscovery != null;
    }
}
