package org.alfresco.webdav.dsl;

import static org.alfresco.utility.report.log.Step.STEP;

import java.io.File;
import java.util.List;

import org.alfresco.utility.dsl.DSLAssertion;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.webdav.WebDavWrapper;
import org.testng.Assert;

public class WebDavAssertion extends DSLAssertion<WebDavWrapper>
{
    public WebDavAssertion(WebDavWrapper webDavProtocol)
    {
        super(webDavProtocol);
    }

    public WebDavWrapper webDavWrapper()
    {
        return getProtocol();
    }

    /**
     * Verify if folder children exist in parent folder
     * 
     * @param fileModel children files
     * @return
     * @throws Exception
     */
    public WebDavWrapper hasFolders(FolderModel... folderModel) throws Exception
    {
        String currentSpace = webDavWrapper().getCurrentSpace();
        List<FolderModel> folders = webDavWrapper().getFolders();
        for (FolderModel folder : folderModel)
        {
            STEP(String.format("%s Verify that folder %s is in %s", WebDavWrapper.STEP_PREFIX, folder.getName(), currentSpace));
            Assert.assertTrue(webDavWrapper().withWebDavUtil().isFolderInList(folder, folders),
                    String.format("Folder %s is in %s", folder.getName(), currentSpace));
        }
        return webDavWrapper();
    }

    /**
     * Verify if file children exist in parent folder
     * 
     * @param fileModel children files
     * @return
     * @throws Exception
     */
    public WebDavWrapper hasFiles(FileModel... fileModel) throws Exception
    {
        String currentSpace = webDavWrapper().getCurrentSpace();
        List<FileModel> files = webDavWrapper().getFiles();
        for (FileModel file : fileModel)
        {
            STEP(String.format("%s Verify that file %s is in %s", WebDavWrapper.STEP_PREFIX, file.getName(), currentSpace));
            Assert.assertTrue(webDavWrapper().withWebDavUtil().isFileInList(file, files), String.format("File %s is in %s", file.getName(), currentSpace));
        }
        return webDavWrapper();
    }

    /**
     * Verify the children(files and folders) from a parent folder
     * 
     * @param contentModel children
     * @return
     * @throws Exception
     */
    public WebDavWrapper hasChildren(ContentModel... contentModel) throws Exception
    {
        String currentSpace = webDavWrapper().getCurrentSpace();
        List<ContentModel> contents = webDavWrapper().withWebDavUtil().getChildren();
        for (ContentModel content : contentModel)
        {
            STEP(String.format("%s Verify that file %s is in %s", WebDavWrapper.STEP_PREFIX, content.getName(), currentSpace));
            Assert.assertTrue(webDavWrapper().withWebDavUtil().isContentInList(content, contents),
                    String.format("Content %s is in %s", content.getName(), currentSpace));
        }
        return webDavWrapper();
    }

    /**
     * Verify the status for a specific webdav action
     * 
     * @param status
     * @return
     */
    public WebDavWrapper hasStatus(int status)
    {
        Assert.assertEquals(webDavWrapper().withWebDavUtil().getStatus(), status, String.format("Verify status"));
        return webDavWrapper();
    }

    /**
     * Verify the content from a file
     * 
     * @param content String content to verify
     * @return
     * @throws Exception
     */
    public WebDavWrapper contentIs(String content) throws Exception
    {
        STEP(String.format("%s Verify that content '%s' is the expected one", WebDavWrapper.STEP_PREFIX, content));
        Assert.assertEquals(webDavWrapper().withWebDavUtil().getContent(), content, "File content is the expected one");
        return webDavWrapper();
    }

    /**
     * Verify that content exits in webdav
     * 
     * @return
     * @throws Exception
     */
    public WebDavWrapper existsInWebdav() throws Exception
    {
        String contentName = new File(webDavWrapper().getLastResource()).getName();
        STEP(String.format("%s Verify that content '%s' exists in webdav", WebDavWrapper.STEP_PREFIX, contentName));
        Assert.assertTrue(webDavWrapper().withWebDavUtil().contentExists(), String.format("Content %s exists in webdav", contentName));
        return webDavWrapper();
    }

    /**
     * Verify that content does not exit in webdav
     * 
     * @return
     * @throws Exception
     */
    public WebDavWrapper doesNotExistInWebdav() throws Exception
    {
        String contentName = new File(webDavWrapper().getLastResource()).getName();
        STEP(String.format("%s Verify that content '%s' does not exist in webdav", WebDavWrapper.STEP_PREFIX, contentName));
        Assert.assertFalse(webDavWrapper().withWebDavUtil().contentExists(), String.format("Content %s does not exist in webdav", contentName));
        return webDavWrapper();
    }

    /**
     * Verify if file is downloaded in project root.
     * 
     * @return
     */
    public WebDavWrapper isDownloaded()
    {
        String contentName = new File(webDavWrapper().getLastResource()).getName();
        STEP(String.format("%s Verify that %s is downloaded", WebDavWrapper.STEP_PREFIX, contentName));
        File downloadedFile = new File(System.getProperty("user.dir") + File.separator + "target" 
                + File.separator + contentName);
        Assert.assertTrue(downloadedFile.exists(), String.format("%s is downloaded", contentName));
        return webDavWrapper();
    }
    
    /**
     * Verify if a file is locked
     * @return
     * @throws Exception
     */
    public WebDavWrapper isLocked() throws Exception
    {
        String contentName = new File(webDavWrapper().getLastResource()).getName();
        STEP(String.format("%s Verify that '%s' is locked", WebDavWrapper.STEP_PREFIX, contentName));
        Assert.assertTrue(webDavWrapper().withWebDavUtil().isLocked(), String.format("Content %s is locked", contentName));
        return webDavWrapper();
    }
    
    /**
     * Verify if a file is not locked
     * @return
     * @throws Exception
     */
    public WebDavWrapper isUnlocked() throws Exception
    {
        String contentName = new File(webDavWrapper().getLastResource()).getName();
        STEP(String.format("%s Verify that '%s' is locked", WebDavWrapper.STEP_PREFIX, contentName));
        Assert.assertFalse(webDavWrapper().withWebDavUtil().isLocked(), String.format("Content %s is locked", contentName));
        return webDavWrapper();
    }
    
    /**
     * Verify if response header value is as expected
     * Example:
     * hasResponseHeaderValue(WebDavWrapper.RESPONSE_HEADER_CONTENT_TYPE, "text/plain");
     * 
     * @param headerName Name of the response header
     * @param headerValue Value of the response header to be checked
     * @return
     * @throws Exception
     */
    public WebDavWrapper hasResponseHeaderValue(String headerName, String headerValue) throws Exception
    {
		STEP(String.format("%s Verify that the header value for '%s' is correct", WebDavWrapper.STEP_PREFIX, headerName));
		Assert.assertTrue(webDavWrapper().getResponseHeaderValue(headerName).contains(headerValue),
				String.format("The value of header [%s] is not the expected one. Received: %s, Expected, %s",
						headerName, webDavWrapper().getResponseHeaderValue(headerName), headerValue));
		return webDavWrapper();
    }
}
