package ar.rou.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Converts Markdown documentation to Atlassian Document Format (ADF).
 * This class processes Markdown files and converts them to ADF JSON format
 * compatible with Confluence.
 */
public class MarkdownToAdfConverter {
    
    private static final String CONVERTED_DIR = "converted";
    private static final String MARKDOWN_DIR = CONVERTED_DIR + "/markdown";
    private static final String ADF_DIR = CONVERTED_DIR + "/adf";
    
    private final ObjectMapper objectMapper;
    private final Parser markdownParser;
    
    public MarkdownToAdfConverter() {
        this.objectMapper = new ObjectMapper();
        
        MutableDataSet options = new MutableDataSet();
        this.markdownParser = Parser.builder(options).build();
    }
    
    public static void main(String[] args) {
        try {
            MarkdownToAdfConverter converter = new MarkdownToAdfConverter();
            converter.convertMarkdownToAdf();
            System.out.println("Markdown to ADF conversion completed successfully");
        } catch (Exception e) {
            System.err.println("Markdown to ADF conversion failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void convertMarkdownToAdf() throws IOException {
        // Create output directories
        createDirectories();
        
        // Process all Markdown files
        File markdownDir = new File(MARKDOWN_DIR);
        if (!markdownDir.exists() || !markdownDir.isDirectory()) {
            System.out.println("No markdown directory found at: " + MARKDOWN_DIR);
            return;
        }
        
        File[] markdownFiles = markdownDir.listFiles((dir, name) -> name.endsWith(".md"));
        if (markdownFiles == null || markdownFiles.length == 0) {
            System.out.println("No Markdown files found to convert");
            return;
        }
        
        for (File markdownFile : markdownFiles) {
            convertMarkdownFileToAdf(markdownFile);
        }
    }
    
    private void createDirectories() throws IOException {
        Files.createDirectories(Paths.get(ADF_DIR));
    }
    
    private void convertMarkdownFileToAdf(File markdownFile) throws IOException {
        System.out.println("Converting to ADF: " + markdownFile.getName());
        
        // Read markdown content
        String markdownContent = Files.readString(markdownFile.toPath());
        
        // Parse markdown
        Node document = markdownParser.parse(markdownContent);
        
        // Convert to ADF
        ObjectNode adfDocument = createAdfDocument();
        ArrayNode content = adfDocument.putArray("content");
        
        // Process each child node
        for (Node child : document.getChildren()) {
            ObjectNode adfNode = convertNodeToAdf(child);
            if (adfNode != null) {
                content.add(adfNode);
            }
        }
        
        // Write ADF file
        String outputFilename = markdownFile.getName().replace(".md", ".json");
        Path adfPath = Paths.get(ADF_DIR, outputFilename);
        
        try (FileWriter writer = new FileWriter(adfPath.toFile())) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, adfDocument);
        }
        
        System.out.println("Converted to ADF: " + outputFilename);
    }
    
    private ObjectNode createAdfDocument() {
        ObjectNode document = objectMapper.createObjectNode();
        document.put("version", 1);
        document.put("type", "doc");
        return document;
    }
    
    private ObjectNode convertNodeToAdf(Node node) {
        if (node instanceof Heading) {
            return convertHeading((Heading) node);
        } else if (node instanceof Paragraph) {
            return convertParagraph((Paragraph) node);
        } else if (node instanceof BulletList) {
            return convertBulletList((BulletList) node);
        } else if (node instanceof OrderedList) {
            return convertOrderedList((OrderedList) node);
        } else if (node instanceof FencedCodeBlock) {
            return convertCodeBlock((FencedCodeBlock) node);
        } else if (node instanceof BlockQuote) {
            return convertBlockQuote((BlockQuote) node);
        }
        
        return null;
    }
    
    private ObjectNode convertHeading(Heading heading) {
        ObjectNode headingNode = objectMapper.createObjectNode();
        headingNode.put("type", "heading");
        
        ObjectNode attrs = headingNode.putObject("attrs");
        attrs.put("level", heading.getLevel());
        
        ArrayNode content = headingNode.putArray("content");
        ObjectNode textNode = content.addObject();
        textNode.put("type", "text");
        textNode.put("text", heading.getText().toString());
        
        return headingNode;
    }
    
    private ObjectNode convertParagraph(Paragraph paragraph) {
        ObjectNode paragraphNode = objectMapper.createObjectNode();
        paragraphNode.put("type", "paragraph");
        
        ArrayNode content = paragraphNode.putArray("content");
        
        for (Node child : paragraph.getChildren()) {
            ObjectNode childNode = convertInlineNode(child);
            if (childNode != null) {
                content.add(childNode);
            }
        }
        
        return paragraphNode;
    }
    
    private ObjectNode convertInlineNode(Node node) {
        if (node instanceof Text) {
            ObjectNode textNode = objectMapper.createObjectNode();
            textNode.put("type", "text");
            textNode.put("text", node.getChars().toString());
            return textNode;
        } else if (node instanceof StrongEmphasis) {
            ObjectNode textNode = objectMapper.createObjectNode();
            textNode.put("type", "text");
            textNode.put("text", node.getChildChars().toString());
            
            ArrayNode marks = textNode.putArray("marks");
            ObjectNode strongMark = marks.addObject();
            strongMark.put("type", "strong");
            
            return textNode;
        } else if (node instanceof Emphasis) {
            ObjectNode textNode = objectMapper.createObjectNode();
            textNode.put("type", "text");
            textNode.put("text", node.getChildChars().toString());
            
            ArrayNode marks = textNode.putArray("marks");
            ObjectNode emMark = marks.addObject();
            emMark.put("type", "em");
            
            return textNode;
        } else if (node instanceof Link) {
            Link link = (Link) node;
            ObjectNode textNode = objectMapper.createObjectNode();
            textNode.put("type", "text");
            textNode.put("text", link.getText().toString());
            
            ArrayNode marks = textNode.putArray("marks");
            ObjectNode linkMark = marks.addObject();
            linkMark.put("type", "link");
            
            ObjectNode linkAttrs = linkMark.putObject("attrs");
            linkAttrs.put("href", link.getUrl().toString());
            
            return textNode;
        } else if (node instanceof Code) {
            ObjectNode textNode = objectMapper.createObjectNode();
            textNode.put("type", "text");
            textNode.put("text", node.getChars().toString());
            
            ArrayNode marks = textNode.putArray("marks");
            ObjectNode codeMark = marks.addObject();
            codeMark.put("type", "code");
            
            return textNode;
        }
        
        // Fallback for other text content
        if (node.getChars().length() > 0) {
            ObjectNode textNode = objectMapper.createObjectNode();
            textNode.put("type", "text");
            textNode.put("text", node.getChars().toString());
            return textNode;
        }
        
        return null;
    }
    
    private ObjectNode convertBulletList(BulletList list) {
        ObjectNode listNode = objectMapper.createObjectNode();
        listNode.put("type", "bulletList");
        
        ArrayNode content = listNode.putArray("content");
        
        for (Node child : list.getChildren()) {
            if (child instanceof BulletListItem) {
                ObjectNode listItemNode = convertListItem((BulletListItem) child);
                if (listItemNode != null) {
                    content.add(listItemNode);
                }
            }
        }
        
        return listNode;
    }
    
    private ObjectNode convertOrderedList(OrderedList list) {
        ObjectNode listNode = objectMapper.createObjectNode();
        listNode.put("type", "orderedList");
        
        ArrayNode content = listNode.putArray("content");
        
        for (Node child : list.getChildren()) {
            if (child instanceof OrderedListItem) {
                ObjectNode listItemNode = convertListItem((OrderedListItem) child);
                if (listItemNode != null) {
                    content.add(listItemNode);
                }
            }
        }
        
        return listNode;
    }
    
    private ObjectNode convertListItem(ListItem item) {
        ObjectNode listItemNode = objectMapper.createObjectNode();
        listItemNode.put("type", "listItem");
        
        ArrayNode content = listItemNode.putArray("content");
        
        for (Node child : item.getChildren()) {
            ObjectNode childNode = convertNodeToAdf(child);
            if (childNode != null) {
                content.add(childNode);
            }
        }
        
        return listItemNode;
    }
    
    private ObjectNode convertCodeBlock(FencedCodeBlock codeBlock) {
        ObjectNode codeBlockNode = objectMapper.createObjectNode();
        codeBlockNode.put("type", "codeBlock");
        
        // Set language if available
        String info = codeBlock.getInfo().toString().trim();
        if (!info.isEmpty()) {
            ObjectNode attrs = codeBlockNode.putObject("attrs");
            attrs.put("language", info);
        }
        
        ArrayNode content = codeBlockNode.putArray("content");
        ObjectNode textNode = content.addObject();
        textNode.put("type", "text");
        textNode.put("text", codeBlock.getContentChars().toString());
        
        return codeBlockNode;
    }
    
    private ObjectNode convertBlockQuote(BlockQuote blockQuote) {
        ObjectNode blockQuoteNode = objectMapper.createObjectNode();
        blockQuoteNode.put("type", "blockquote");
        
        ArrayNode content = blockQuoteNode.putArray("content");
        
        for (Node child : blockQuote.getChildren()) {
            ObjectNode childNode = convertNodeToAdf(child);
            if (childNode != null) {
                content.add(childNode);
            }
        }
        
        return blockQuoteNode;
    }
}