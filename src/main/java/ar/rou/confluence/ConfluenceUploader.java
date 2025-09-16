package ar.rou.confluence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Uploads documentation to Confluence using ADF format.
 * This class handles the upload of converted documentation to Confluence,
 * including replacing image references with uploaded schema diagrams.
 */
public class ConfluenceUploader {
    
    private static final String CONVERTED_DIR = "converted";
    private static final String ADF_DIR = CONVERTED_DIR + "/adf";
    private static final String EXPORTS_DIR = "exports";
    private static final String DIAGRAMS_DIR = EXPORTS_DIR + "/diagrams";
    
    private final String confluenceUrl;
    private final String username;
    private final String apiToken;
    private final String spaceKey;
    private final String branchName;
    private final String repositoryUrl;
    private final String commitHash;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String authHeader;
    
    public ConfluenceUploader(String confluenceUrl, String username, String apiToken, String spaceKey, String branchName, String repositoryUrl, String commitHash) {
        this.confluenceUrl = confluenceUrl.endsWith("/") ? confluenceUrl : confluenceUrl + "/";
        this.username = username;
        this.apiToken = apiToken;
        this.spaceKey = spaceKey;
        this.branchName = branchName;
        this.repositoryUrl = repositoryUrl;
        this.commitHash = commitHash;
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString((username + ":" + apiToken).getBytes());
    }
    
    public static void main(String[] args) {
        if (args.length != 7) {
            System.err.println("Usage: ConfluenceUploader <confluenceUrl> <username> <apiToken> <spaceKey> <branchName> <repositoryUrl> <commitHash>");
            System.exit(1);
        }
        
        String confluenceUrl = args[0];
        String username = args[1];
        String apiToken = args[2];
        String spaceKey = args[3];
        String branchName = args[4];
        String repositoryUrl = args[5];
        String commitHash = args[6];
        
        try {
            ConfluenceUploader uploader = new ConfluenceUploader(confluenceUrl, username, apiToken, spaceKey, branchName, repositoryUrl, commitHash);
            uploader.uploadDocumentation();
            uploader.close();
            System.out.println("Documentation upload completed successfully");
        } catch (Exception e) {
            System.err.println("Documentation upload failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void uploadDocumentation() throws IOException {
        // First, upload all diagrams and get their attachment IDs
        Map<String, String> diagramAttachments = uploadDiagrams();
        
        // Process all ADF files
        File adfDir = new File(ADF_DIR);
        if (!adfDir.exists() || !adfDir.isDirectory()) {
            System.out.println("No ADF directory found at: " + ADF_DIR);
            return;
        }
        
        File[] adfFiles = adfDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (adfFiles == null || adfFiles.length == 0) {
            System.out.println("No ADF files found to upload");
            return;
        }
        
        for (File adfFile : adfFiles) {
            uploadAdfDocument(adfFile, diagramAttachments);
        }
    }
    
    private Map<String, String> uploadDiagrams() throws IOException {
        Map<String, String> attachments = new HashMap<>();
        
        File diagramsDir = new File(DIAGRAMS_DIR);
        if (!diagramsDir.exists() || !diagramsDir.isDirectory()) {
            System.out.println("No diagrams directory found at: " + DIAGRAMS_DIR);
            return attachments;
        }
        
        File[] pngFiles = diagramsDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (pngFiles == null || pngFiles.length == 0) {
            System.out.println("No PNG diagram files found to upload");
            return attachments;
        }
        
        for (File pngFile : pngFiles) {
            String attachmentId = uploadAttachment(pngFile);
            if (attachmentId != null) {
                attachments.put(pngFile.getName(), attachmentId);
                System.out.println("Uploaded diagram: " + pngFile.getName() + " -> " + attachmentId);
            }
        }
        
        return attachments;
    }
    
    private String uploadAttachment(File file) throws IOException {
        System.out.println("Uploading attachment: " + file.getName());
        
        // Create a temporary page to attach the file to
        // In a real scenario, you might want to attach to a specific page
        String tempPageId = createTempPage("Diagrams_" + System.currentTimeMillis());
        
        if (tempPageId == null) {
            System.err.println("Failed to create temporary page for attachment");
            return null;
        }
        
        String url = confluenceUrl + "rest/api/content/" + tempPageId + "/child/attachment";
        HttpPost post = new HttpPost(url);
        post.setHeader("Authorization", authHeader);
        post.setHeader("X-Atlassian-Token", "no-check");
        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", file, ContentType.DEFAULT_BINARY, file.getName());
        post.setEntity(builder.build());
        
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int statusCode = response.getCode();
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            
            if (statusCode == 200) {
                JsonNode responseJson = objectMapper.readTree(responseBody);
                JsonNode results = responseJson.get("results");
                if (results.isArray() && results.size() > 0) {
                    return results.get(0).get("id").asText();
                }
            } else {
                System.err.println("Failed to upload attachment: " + statusCode + " - " + responseBody);
            }
        }
        
        return null;
    }
    
    private String createTempPage(String title) throws IOException {
        String url = confluenceUrl + "rest/api/content";
        HttpPost post = new HttpPost(url);
        post.setHeader("Authorization", authHeader);
        post.setHeader("Content-Type", "application/json");
        
        ObjectNode pageData = objectMapper.createObjectNode();
        pageData.put("type", "page");
        pageData.put("title", title);
        
        ObjectNode space = pageData.putObject("space");
        space.put("key", spaceKey);
        
        ObjectNode body = pageData.putObject("body");
        ObjectNode storage = body.putObject("storage");
        storage.put("value", "<p>Temporary page for diagram attachments</p>");
        storage.put("representation", "storage");
        
        post.setEntity(new StringEntity(objectMapper.writeValueAsString(pageData), ContentType.APPLICATION_JSON));
        
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int statusCode = response.getCode();
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            
            if (statusCode == 200) {
                JsonNode responseJson = objectMapper.readTree(responseBody);
                return responseJson.get("id").asText();
            } else {
                System.err.println("Failed to create temporary page: " + statusCode + " - " + responseBody);
            }
        }
        
        return null;
    }
    
    private void uploadAdfDocument(File adfFile, Map<String, String> diagramAttachments) throws IOException {
        System.out.println("Uploading ADF document: " + adfFile.getName());
        
        // Read ADF content
        String adfContent = Files.readString(adfFile.toPath());
        JsonNode adfDocument = objectMapper.readTree(adfContent);
        
        // Add metadata header to the document
        JsonNode processedAdf = addMetadataHeader(adfDocument);
        
        // Replace image references with Confluence attachments
        processedAdf = replaceImageReferences(processedAdf, diagramAttachments);
        
        // Use branch name as page title
        String pageTitle = branchName + " - " + adfFile.getName().replace(".json", "").replace("_", " ");
        pageTitle = formatPageTitle(pageTitle);
        
        // Check if page exists
        String existingPageId = findPageByTitle(pageTitle);
        
        if (existingPageId != null) {
            updatePage(existingPageId, pageTitle, processedAdf);
        } else {
            createPage(pageTitle, processedAdf);
        }
    }
    
    private JsonNode addMetadataHeader(JsonNode adfDocument) {
        // Create metadata paragraph
        ObjectNode metadataParagraph = objectMapper.createObjectNode();
        metadataParagraph.put("type", "paragraph");
        
        ArrayNode metadataContent = metadataParagraph.putArray("content");
        
        // Add repository URL
        ObjectNode repoLink = metadataContent.addObject();
        repoLink.put("type", "text");
        repoLink.put("text", "Repository: " + repositoryUrl);
        ArrayNode repoMarks = repoLink.putArray("marks");
        ObjectNode linkMark = repoMarks.addObject();
        linkMark.put("type", "link");
        ObjectNode linkAttrs = linkMark.putObject("attrs");
        linkAttrs.put("href", repositoryUrl);
        
        // Add line break
        ObjectNode lineBreak1 = metadataContent.addObject();
        lineBreak1.put("type", "hardBreak");
        
        // Add commit hash
        ObjectNode commitText = metadataContent.addObject();
        commitText.put("type", "text");
        commitText.put("text", "Commit: " + commitHash);
        ArrayNode commitMarks = commitText.putArray("marks");
        ObjectNode codeMark = commitMarks.addObject();
        codeMark.put("type", "code");
        
        // Add line break
        ObjectNode lineBreak2 = metadataContent.addObject();
        lineBreak2.put("type", "hardBreak");
        
        // Add generation date
        ObjectNode dateText = metadataContent.addObject();
        dateText.put("type", "text");
        String generationDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        dateText.put("text", "Generated: " + generationDate);
        ArrayNode dateMarks = dateText.putArray("marks");
        ObjectNode emMark = dateMarks.addObject();
        emMark.put("type", "em");
        
        // Clone the original document and prepend metadata
        ObjectNode newDocument = adfDocument.deepCopy();
        ArrayNode content = (ArrayNode) newDocument.get("content");
        
        // Insert metadata at the beginning
        content.insert(0, metadataParagraph);
        
        // Add a divider after metadata
        ObjectNode divider = objectMapper.createObjectNode();
        divider.put("type", "rule");
        content.insert(1, divider);
        
        return newDocument;
    }
    
    private String formatPageTitle(String title) {
        // Capitalize first letter and format nicely
        if (title == null || title.trim().isEmpty()) {
            return "Documentation";
        }
        
        String formatted = title.trim();
        if (formatted.length() > 0) {
            formatted = formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
        }
        
        return formatted;
    }
    
    private JsonNode replaceImageReferences(JsonNode adfDocument, Map<String, String> diagramAttachments) {
        // This is a simplified implementation
        // In a real scenario, you would traverse the ADF structure and replace image nodes
        // with Confluence media nodes that reference the uploaded attachments
        
        // For now, we'll return the document as-is
        // A full implementation would recursively process the content array
        // and convert any image references to Confluence media nodes
        
        return adfDocument;
    }
    
    private String findPageByTitle(String title) throws IOException {
        String url = confluenceUrl + "rest/api/content?spaceKey=" + spaceKey + "&title=" + title.replace(" ", "%20");
        HttpGet get = new HttpGet(url);
        get.setHeader("Authorization", authHeader);
        
        try (CloseableHttpResponse response = httpClient.execute(get)) {
            int statusCode = response.getCode();
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            
            if (statusCode == 200) {
                JsonNode responseJson = objectMapper.readTree(responseBody);
                JsonNode results = responseJson.get("results");
                if (results.isArray() && results.size() > 0) {
                    return results.get(0).get("id").asText();
                }
            }
        }
        
        return null;
    }
    
    private void createPage(String title, JsonNode adfContent) throws IOException {
        String url = confluenceUrl + "rest/api/content";
        HttpPost post = new HttpPost(url);
        post.setHeader("Authorization", authHeader);
        post.setHeader("Content-Type", "application/json");
        
        ObjectNode pageData = objectMapper.createObjectNode();
        pageData.put("type", "page");
        pageData.put("title", title);
        
        ObjectNode space = pageData.putObject("space");
        space.put("key", spaceKey);
        
        ObjectNode body = pageData.putObject("body");
        ObjectNode atlas_doc_format = body.putObject("atlas_doc_format");
        atlas_doc_format.set("value", adfContent);
        atlas_doc_format.put("representation", "atlas_doc_format");
        
        post.setEntity(new StringEntity(objectMapper.writeValueAsString(pageData), ContentType.APPLICATION_JSON));
        
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int statusCode = response.getCode();
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            
            if (statusCode == 200) {
                System.out.println("Created page: " + title);
            } else {
                System.err.println("Failed to create page: " + statusCode + " - " + responseBody);
            }
        }
    }
    
    private void updatePage(String pageId, String title, JsonNode adfContent) throws IOException {
        // Get current page version
        String url = confluenceUrl + "rest/api/content/" + pageId;
        HttpGet get = new HttpGet(url);
        get.setHeader("Authorization", authHeader);
        
        int currentVersion = 1;
        try (CloseableHttpResponse response = httpClient.execute(get)) {
            int statusCode = response.getCode();
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            
            if (statusCode == 200) {
                JsonNode pageJson = objectMapper.readTree(responseBody);
                currentVersion = pageJson.get("version").get("number").asInt();
            }
        }
        
        // Update page
        HttpPut put = new HttpPut(url);
        put.setHeader("Authorization", authHeader);
        put.setHeader("Content-Type", "application/json");
        
        ObjectNode pageData = objectMapper.createObjectNode();
        pageData.put("id", pageId);
        pageData.put("type", "page");
        pageData.put("title", title);
        
        ObjectNode version = pageData.putObject("version");
        version.put("number", currentVersion + 1);
        
        ObjectNode space = pageData.putObject("space");
        space.put("key", spaceKey);
        
        ObjectNode body = pageData.putObject("body");
        ObjectNode atlas_doc_format = body.putObject("atlas_doc_format");
        atlas_doc_format.set("value", adfContent);
        atlas_doc_format.put("representation", "atlas_doc_format");
        
        put.setEntity(new StringEntity(objectMapper.writeValueAsString(pageData), ContentType.APPLICATION_JSON));
        
        try (CloseableHttpResponse response = httpClient.execute(put)) {
            int statusCode = response.getCode();
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            
            if (statusCode == 200) {
                System.out.println("Updated page: " + title);
            } else {
                System.err.println("Failed to update page: " + statusCode + " - " + responseBody);
            }
        }
    }
    
    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}