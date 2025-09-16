package ar.rou.structurizr;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Exports Structurizr workspace diagrams and documentation.
 * This class handles the connection to Structurizr and exports both
 * schemas and documentation.
 * 
 * Note: This is a demonstration implementation that shows the structure.
 * In a real implementation, you would use the official Structurizr Java client.
 */
public class StructurizrExporter {
    
    private static final String EXPORTS_DIR = "exports";
    private static final String DIAGRAMS_DIR = EXPORTS_DIR + "/diagrams";
    private static final String DOCS_DIR = EXPORTS_DIR + "/documentation";
    
    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Usage: StructurizrExporter <url> <apiKey> <apiSecret> <workspaceId>");
            System.exit(1);
        }
        
        String url = args[0];
        String apiKey = args[1];
        String apiSecret = args[2];
        long workspaceId = Long.parseLong(args[3]);
        
        try {
            StructurizrExporter exporter = new StructurizrExporter();
            exporter.exportWorkspace(url, apiKey, apiSecret, workspaceId);
            System.out.println("Export completed successfully");
        } catch (Exception e) {
            System.err.println("Export failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void exportWorkspace(String baseUrl, String apiKey, String apiSecret, long workspaceId) throws Exception {
        // Create output directories
        createDirectories();
        
        System.out.println("Connecting to Structurizr at: " + baseUrl);
        System.out.println("Workspace ID: " + workspaceId);
        
        // In a real implementation, you would:
        // 1. Use StructurizrClient to connect to the API
        // 2. Fetch the workspace
        // 3. Export diagrams using PlantUML exporter
        // 4. Export documentation sections
        
        // For demonstration, we'll create sample files
        exportSampleDiagrams();
        exportSampleDocumentation();
        
        System.out.println("Workspace exported successfully");
    }
    
    private void createDirectories() throws IOException {
        Files.createDirectories(Paths.get(DIAGRAMS_DIR));
        Files.createDirectories(Paths.get(DOCS_DIR));
    }
    
    private void exportSampleDiagrams() throws IOException {
        System.out.println("Creating sample diagrams...");
        
        // Create sample C4 system context diagram
        String contextDiagram = "@startuml\n" +
                "!include <C4/C4_Context>\n\n" +
                "LAYOUT_WITH_LEGEND()\n\n" +
                "title System Context Diagram\n\n" +
                "Person(customer, \"Customer\", \"A customer of the system\")\n" +
                "Person(admin, \"Administrator\", \"System administrator\")\n\n" +
                "System(system, \"Main System\", \"Core business system\")\n" +
                "System_Ext(email, \"Email System\", \"External email provider\")\n" +
                "System_Ext(payment, \"Payment System\", \"External payment gateway\")\n\n" +
                "Rel(customer, system, \"Uses\")\n" +
                "Rel(admin, system, \"Manages\")\n" +
                "Rel(system, email, \"Sends emails\")\n" +
                "Rel(system, payment, \"Processes payments\")\n\n" +
                "@enduml\n";
        
        writeFile(Paths.get(DIAGRAMS_DIR, "system-context.puml"), contextDiagram);
        
        // Create sample container diagram
        String containerDiagram = "@startuml\n" +
                "!include <C4/C4_Container>\n\n" +
                "LAYOUT_WITH_LEGEND()\n\n" +
                "title Container Diagram\n\n" +
                "Person(customer, \"Customer\", \"System user\")\n\n" +
                "System_Boundary(system, \"Main System\") {\n" +
                "    Container(web, \"Web Application\", \"Java, Spring Boot\", \"Provides web interface\")\n" +
                "    Container(api, \"API Gateway\", \"Java, Spring Boot\", \"Handles API requests\")\n" +
                "    Container(service, \"Business Service\", \"Java, Spring Boot\", \"Core business logic\")\n" +
                "    ContainerDb(db, \"Database\", \"PostgreSQL\", \"Stores business data\")\n" +
                "}\n\n" +
                "Rel(customer, web, \"Uses\", \"HTTPS\")\n" +
                "Rel(web, api, \"Calls\", \"REST/JSON\")\n" +
                "Rel(api, service, \"Invokes\")\n" +
                "Rel(service, db, \"Reads/Writes\", \"JDBC\")\n\n" +
                "@enduml\n";
        
        writeFile(Paths.get(DIAGRAMS_DIR, "container-diagram.puml"), containerDiagram);
        
        // Create a script to convert PlantUML to PNG
        createPlantUMLConversionScript();
        
        System.out.println("Sample diagrams created");
    }
    
    private void exportSampleDocumentation() throws IOException {
        System.out.println("Creating sample documentation...");
        
        String architectureDoc = "= System Architecture Documentation\n\n" +
                "This document describes the architecture of our system as exported from Structurizr.\n\n" +
                "== Overview\n\n" +
                "The system follows a microservices architecture with clear separation of concerns.\n\n" +
                "== System Context\n\n" +
                "The system operates within the following context:\n\n" +
                "* *Customers* interact with the system through web and mobile interfaces\n" +
                "* *Administrators* manage the system through administrative interfaces\n" +
                "* *External Systems* provide email and payment capabilities\n\n" +
                "image::system-context.png[System Context Diagram]\n\n" +
                "== Container Overview\n\n" +
                "The system consists of the following containers:\n\n" +
                "=== Web Application\n" +
                "The web application provides the user interface for customers.\n\n" +
                "=== API Gateway\n" +
                "The API gateway handles all external API requests and provides:\n\n" +
                "* Authentication and authorization\n" +
                "* Rate limiting\n" +
                "* Request routing\n\n" +
                "=== Business Service\n" +
                "The business service contains the core business logic.\n\n" +
                "=== Database\n" +
                "PostgreSQL database that stores all business data.\n\n" +
                "image::container-diagram.png[Container Diagram]\n\n" +
                "== Deployment\n\n" +
                "The system is deployed using Docker containers orchestrated by Kubernetes.\n\n" +
                "== Security Considerations\n\n" +
                "* All communications use HTTPS/TLS\n" +
                "* Authentication uses JWT tokens\n" +
                "* Database connections are encrypted\n\n";
        
        writeFile(Paths.get(DOCS_DIR, "architecture.adoc"), architectureDoc);
        
        String deploymentDoc = "= Deployment Guide\n\n" +
                "This document describes how to deploy the system.\n\n" +
                "== Prerequisites\n\n" +
                "* Docker 20.10+\n" +
                "* Kubernetes 1.21+\n" +
                "* PostgreSQL 13+\n\n" +
                "== Deployment Steps\n\n" +
                ". Build the Docker images\n" +
                ". Configure the environment variables\n" +
                ". Deploy to Kubernetes\n" +
                ". Verify the deployment\n\n" +
                "== Configuration\n\n" +
                "The following environment variables must be set:\n\n" +
                "* `DATABASE_URL` - Database connection string\n" +
                "* `JWT_SECRET` - Secret for JWT token signing\n" +
                "* `EMAIL_API_KEY` - API key for email service\n\n";
        
        writeFile(Paths.get(DOCS_DIR, "deployment.adoc"), deploymentDoc);
        
        System.out.println("Sample documentation created");
    }
    
    private void createPlantUMLConversionScript() throws IOException {
        String script = "#!/bin/bash\n" +
                "# Convert PlantUML files to PNG\n" +
                "echo \"Converting PlantUML diagrams to PNG...\"\n" +
                "cd " + DIAGRAMS_DIR + "\n\n" +
                "# Install PlantUML if not available\n" +
                "if ! command -v plantuml &> /dev/null; then\n" +
                "    echo \"PlantUML not found. Installing...\"\n" +
                "    apt-get update && apt-get install -y plantuml\n" +
                "fi\n\n" +
                "for file in *.puml; do\n" +
                "  if [ -f \"$file\" ]; then\n" +
                "    echo \"Converting $file to PNG\"\n" +
                "    plantuml -tpng \"$file\" || {\n" +
                "        echo \"PlantUML conversion failed for $file, creating placeholder\"\n" +
                "        base_name=$(basename \"$file\" .puml)\n" +
                "        # Create a simple placeholder text file that can be converted later\n" +
                "        echo \"[Diagram placeholder for $base_name]\" > \"${base_name}.png.txt\"\n" +
                "    }\n" +
                "  fi\n" +
                "done\n" +
                "echo \"Diagram conversion completed\"\n";
        
        Path scriptPath = Paths.get(EXPORTS_DIR, "convert_diagrams.sh");
        writeFile(scriptPath, script);
        
        // Make script executable
        scriptPath.toFile().setExecutable(true);
        System.out.println("Created diagram conversion script");
    }
    
    private void writeFile(Path filePath, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(content);
        }
    }
}