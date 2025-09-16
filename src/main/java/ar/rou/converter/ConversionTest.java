package ar.rou.converter;

import java.nio.file.Files;
import java.nio.file.Paths;

public class ConversionTest {
    public static void main(String[] args) throws Exception {
        String content = Files.readString(Paths.get("exports/documentation/architecture.adoc"));
        System.out.println("Original content length: " + content.length());
        System.out.println("First 100 chars: " + content.substring(0, Math.min(100, content.length())));
        
        MarkdownToAdfConverter converter = new MarkdownToAdfConverter();
        DocumentationConverter docConverter = new DocumentationConverter();
        
        // Test the direct conversion method
        String markdown = docConverter.testConvertAsciiDocToMarkdownDirect(content);
        System.out.println("Converted markdown length: " + markdown.length());
        System.out.println("First 100 chars of markdown: " + (markdown.length() > 0 ? markdown.substring(0, Math.min(100, markdown.length())) : "EMPTY"));
    }
}