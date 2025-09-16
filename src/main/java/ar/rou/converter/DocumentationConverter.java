package ar.rou.converter;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts AsciiDoc documentation to Markdown format.
 * This class processes AsciiDoc files exported from Structurizr
 * and converts them to Markdown for further processing.
 */
public class DocumentationConverter {
    
    private static final String EXPORTS_DIR = "exports";
    private static final String DOCS_DIR = EXPORTS_DIR + "/documentation";
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
        
        // Process all AsciiDoc files
        File docsDir = new File(DOCS_DIR);
        if (!docsDir.exists() || !docsDir.isDirectory()) {
            System.out.println("No documentation directory found at: " + DOCS_DIR);
            return;
        }
        
        File[] adocFiles = docsDir.listFiles((dir, name) -> name.endsWith(".adoc"));
        if (adocFiles == null || adocFiles.length == 0) {
            System.out.println("No AsciiDoc files found to convert");
            return;
        }
        
        // Initialize AsciiDoctor
        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            for (File adocFile : adocFiles) {
                convertAsciiDocToMarkdown(asciidoctor, adocFile);
            }
        }
    }
    
    private void createDirectories() throws IOException {
        Files.createDirectories(Paths.get(MARKDOWN_DIR));
    }
    
    private void convertAsciiDocToMarkdown(Asciidoctor asciidoctor, File adocFile) throws IOException {
        System.out.println("Converting: " + adocFile.getName());
        
        // Configure options for AsciiDoc to HTML conversion
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("showtitle", true);
        attributes.put("source-highlighter", "none"); // Disable syntax highlighting for cleaner HTML
        attributes.put("sectanchors", false);
        attributes.put("sectlinks", false);
        
        Options options = Options.builder()
                .backend("html5")
                .safe(SafeMode.UNSAFE)
                .attributes(attributes)
                .build();
        
        // Convert AsciiDoc to HTML using AsciiDoctor
        String html;
        try {
            html = asciidoctor.convertFile(adocFile, options);
            if (html != null && !html.trim().isEmpty()) {
                System.out.println("Successfully converted AsciiDoc to HTML for " + adocFile.getName());
            } else {
                throw new RuntimeException("AsciiDoctor returned empty HTML");
            }
        } catch (Exception e) {
            System.err.println("Error converting AsciiDoc to HTML: " + e.getMessage());
            // Fallback to reading file directly and using simple conversion
            String asciidocContent = Files.readString(adocFile.toPath());
            html = convertAsciiDocToHtmlFallback(asciidocContent);
        }
        
        // Convert HTML to Markdown using Flexmark
        String markdown = convertHtmlToMarkdownWithFlexmark(html);
        
        // Write markdown file
        String outputFilename = adocFile.getName().replace(".adoc", ".md");
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
     * Fallback method to convert AsciiDoc to basic HTML when AsciiDoctor fails
     */
    private String convertAsciiDocToHtmlFallback(String asciidoc) {
        if (asciidoc == null || asciidoc.trim().isEmpty()) {
            return "<html><body><p>No content available</p></body></html>";
        }
        
        StringBuilder htmlBuilder = new StringBuilder("<html><body>");
        
        // Convert headers first
        String processed = asciidoc;
        processed = processed.replaceAll("(?m)^= (.+)$", "<h1>$1</h1>");
        processed = processed.replaceAll("(?m)^== (.+)$", "<h2>$1</h2>");
        processed = processed.replaceAll("(?m)^=== (.+)$", "<h3>$1</h3>");
        processed = processed.replaceAll("(?m)^==== (.+)$", "<h4>$1</h4>");
        processed = processed.replaceAll("(?m)^===== (.+)$", "<h5>$1</h5>");
        
        // Split into lines and process paragraphs
        String[] lines = processed.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (!line.startsWith("<h") && !line.startsWith("*") && !line.startsWith(".")) {
                htmlBuilder.append("<p>").append(line).append("</p>");
            } else {
                htmlBuilder.append(line);
            }
        }
        
        htmlBuilder.append("</body></html>");
        return htmlBuilder.toString();
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