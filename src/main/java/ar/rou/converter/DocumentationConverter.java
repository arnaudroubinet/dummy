package ar.rou.converter;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Converts HTML documentation to Markdown format.
 * This class processes HTML files exported directly from Structurizr
 * and converts them to Markdown for further processing to ADF format.
 */
public class DocumentationConverter {
    
    private static final String EXPORTS_DIR = "exports";
    private static final String DOCS_DIR = EXPORTS_DIR + "/documentation";
    private static final String HTML_DIR = DOCS_DIR + "/html";
    private static final String CONVERTED_DIR = "converted";
    private static final String MARKDOWN_DIR = CONVERTED_DIR + "/markdown";
    
    public static void main(String[] args) {
        try {
            DocumentationConverter converter = new DocumentationConverter();
            converter.convertDocumentation();
            
            // Also convert Markdown to ADF
            MarkdownToAdfConverter adfConverter = new MarkdownToAdfConverter();
            adfConverter.convertMarkdownToAdf();
            
            System.out.println("Documentation conversion completed successfully");
        } catch (Exception e) {
            System.err.println("Documentation conversion failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void convertDocumentation() throws IOException {
        // Create output directories
        createDirectories();
        
        // Process HTML files directly from Structurizr export
        File htmlDir = new File(HTML_DIR);
        if (!htmlDir.exists() || !htmlDir.isDirectory()) {
            System.out.println("No HTML directory found at: " + HTML_DIR);
            return;
        }
        
        File[] htmlFiles = htmlDir.listFiles((dir, name) -> name.endsWith(".html"));
        if (htmlFiles == null || htmlFiles.length == 0) {
            System.out.println("No HTML files found to convert");
            return;
        }
        
        // Process each HTML file
        for (File htmlFile : htmlFiles) {
            convertHtmlToMarkdown(htmlFile);
        }
    }
    
    private void createDirectories() throws IOException {
        Files.createDirectories(Paths.get(MARKDOWN_DIR));
    }
    
    private void convertHtmlToMarkdown(File htmlFile) throws IOException {
        System.out.println("Converting HTML to Markdown: " + htmlFile.getName());
        
        // Read HTML content directly from Structurizr export
        String htmlContent = Files.readString(htmlFile.toPath());
        
        // Convert HTML to Markdown using Flexmark
        String markdown = convertHtmlToMarkdownWithFlexmark(htmlContent);
        
        // Write markdown file
        String outputFilename = htmlFile.getName().replace(".html", ".md");
        Path markdownPath = Paths.get(MARKDOWN_DIR, outputFilename);
        
        try (FileWriter writer = new FileWriter(markdownPath.toFile())) {
            writer.write(markdown);
        }
        
        System.out.println("Converted to: " + outputFilename + " (" + markdown.length() + " characters)");
    }
    
    /**
     * Convert HTML to Markdown using Flexmark HTML to Markdown converter.
     * This provides better and more reliable conversion than manual regex replacements.
     */
    private String convertHtmlToMarkdownWithFlexmark(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }
        
        try {
            // Use Flexmark's HTML to Markdown converter
            FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder().build();
            String markdown = converter.convert(html);
            
            // If Flexmark produces empty result, use fallback
            if (markdown == null || markdown.trim().isEmpty()) {
                System.out.println("Flexmark conversion resulted in empty output, using fallback");
                return convertHtmlToMarkdownSimple(html);
            }
            
            // Clean up the markdown output
            markdown = cleanMarkdownOutput(markdown);
            
            return markdown;
        } catch (Exception e) {
            System.err.println("Error converting HTML to Markdown with Flexmark: " + e.getMessage());
            // Fallback to simple conversion if Flexmark fails
            return convertHtmlToMarkdownSimple(html);
        }
    }
    
    /**
     * Clean up the markdown output from Flexmark conversion
     */
    private String cleanMarkdownOutput(String markdown) {
        if (markdown == null) {
            return "";
        }
        
        // Remove extra empty lines
        markdown = markdown.replaceAll("\\n{3,}", "\n\n");
        
        // Trim leading and trailing whitespace
        markdown = markdown.trim();
        
        return markdown;
    }
    
    /**
     * Simple HTML to Markdown converter as fallback.
     * This is kept as backup in case Flexmark conversion fails.
     */
    private String convertHtmlToMarkdownSimple(String html) {
        if (html == null) {
            return "";
        }
        
        // Remove HTML document structure
        html = html.replaceAll("(?s).*<body[^>]*>", "");
        html = html.replaceAll("(?s)</body>.*", "");
        
        // Convert headers
        html = html.replaceAll("<h1[^>]*>(.*?)</h1>", "# $1");
        html = html.replaceAll("<h2[^>]*>(.*?)</h2>", "## $1");
        html = html.replaceAll("<h3[^>]*>(.*?)</h3>", "### $1");
        html = html.replaceAll("<h4[^>]*>(.*?)</h4>", "#### $1");
        html = html.replaceAll("<h5[^>]*>(.*?)</h5>", "##### $1");
        html = html.replaceAll("<h6[^>]*>(.*?)</h6>", "###### $1");
        
        // Convert paragraphs
        html = html.replaceAll("<p[^>]*>(.*?)</p>", "$1\n\n");
        
        // Convert emphasis
        html = html.replaceAll("<strong[^>]*>(.*?)</strong>", "**$1**");
        html = html.replaceAll("<b[^>]*>(.*?)</b>", "**$1**");
        html = html.replaceAll("<em[^>]*>(.*?)</em>", "*$1*");
        html = html.replaceAll("<i[^>]*>(.*?)</i>", "*$1*");
        
        // Convert links
        html = html.replaceAll("<a[^>]*href=[\"'](.*?)[\"'][^>]*>(.*?)</a>", "[$2]($1)");
        
        // Convert code blocks
        html = html.replaceAll("<pre[^>]*><code[^>]*>(.*?)</code></pre>", "```\n$1\n```");
        html = html.replaceAll("<code[^>]*>(.*?)</code>", "`$1`");
        
        // Convert lists
        html = html.replaceAll("<ul[^>]*>", "");
        html = html.replaceAll("</ul>", "");
        html = html.replaceAll("<ol[^>]*>", "");
        html = html.replaceAll("</ol>", "");
        html = html.replaceAll("<li[^>]*>(.*?)</li>", "- $1");
        
        // Convert line breaks
        html = html.replaceAll("<br[^>]*>", "\n");
        
        // Remove remaining HTML tags
        html = html.replaceAll("<[^>]+>", "");
        
        // Clean up whitespace
        html = html.replaceAll("(?m)^\\s+", "");
        html = html.replaceAll("\\n{3,}", "\n\n");
        html = html.trim();
        
        return html;
    }
}