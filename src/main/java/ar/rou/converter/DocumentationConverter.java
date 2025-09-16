package ar.rou.converter;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;

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
        
        // Read the file content directly first
        String asciidocContent = Files.readString(adocFile.toPath());
        System.out.println("Read " + asciidocContent.length() + " characters from " + adocFile.getName());
        
        // Convert directly from AsciiDoc to Markdown
        String markdown = convertAsciiDocToMarkdownDirect(asciidocContent);
        System.out.println("Converted to " + markdown.length() + " characters of markdown");
        
        // Write markdown file
        String outputFilename = adocFile.getName().replace(".adoc", ".md");
        Path markdownPath = Paths.get(MARKDOWN_DIR, outputFilename);
        
        try (FileWriter writer = new FileWriter(markdownPath.toFile())) {
            writer.write(markdown);
        }
        
        System.out.println("Converted to: " + outputFilename);
    }
    
    /**
     * Simple HTML to Markdown converter.
     * This is a basic implementation that handles common HTML elements.
     * For production use, consider using a more robust library like flexmark-html2md-converter.
     */
    private String convertHtmlToMarkdown(String html) {
        if (html == null) {
            return "";
        }
        
        // If the input doesn't look like HTML, treat it as AsciiDoc and convert directly
        if (!html.contains("<html") && !html.contains("<body")) {
            return convertAsciiDocToMarkdownDirect(html);
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
    
    /**
     * Direct AsciiDoc to Markdown conversion for simple cases
     */
    private String convertAsciiDocToMarkdownDirect(String asciidoc) {
        if (asciidoc == null || asciidoc.trim().isEmpty()) {
            return "";
        }
        
        System.out.println("Converting AsciiDoc content of length: " + asciidoc.length());
        
        // Convert AsciiDoc headers to Markdown
        asciidoc = asciidoc.replaceAll("(?m)^= (.+)$", "# $1");
        asciidoc = asciidoc.replaceAll("(?m)^== (.+)$", "## $1");
        asciidoc = asciidoc.replaceAll("(?m)^=== (.+)$", "### $1");
        asciidoc = asciidoc.replaceAll("(?m)^==== (.+)$", "#### $1");
        asciidoc = asciidoc.replaceAll("(?m)^===== (.+)$", "##### $1");
        
        // Convert emphasis
        asciidoc = asciidoc.replaceAll("\\*\\*([^*]+)\\*\\*", "**$1**");
        asciidoc = asciidoc.replaceAll("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)", "*$1*");
        
        // Convert code blocks
        asciidoc = asciidoc.replaceAll("(?s)----\\s*\\n(.*?)\\n----", "```\n$1\n```");
        asciidoc = asciidoc.replaceAll("`([^`]+)`", "`$1`");
        
        // Convert lists (basic)
        asciidoc = asciidoc.replaceAll("(?m)^\\* (.+)$", "- $1");
        asciidoc = asciidoc.replaceAll("(?m)^\\. (.+)$", "1. $1");
        
        // Convert images
        asciidoc = asciidoc.replaceAll("image::([^\\[]+)\\[([^\\]]*)\\]", "![$2]($1)");
        
        System.out.println("Converted markdown length: " + asciidoc.length());
        return asciidoc;
    }
    
    // Test method for debugging
    public String testConvertAsciiDocToMarkdownDirect(String asciidoc) {
        return convertAsciiDocToMarkdownDirect(asciidoc);
    }
}