#!/bin/bash
# Convert PlantUML files to PNG
echo "Converting PlantUML diagrams to PNG..."
cd exports/diagrams

# Install PlantUML if not available
if ! command -v plantuml &> /dev/null; then
    echo "PlantUML not found. Installing..."
    apt-get update && apt-get install -y plantuml
fi

for file in *.puml; do
  if [ -f "$file" ]; then
    echo "Converting $file to PNG"
    plantuml -tpng "$file" || {
        echo "PlantUML conversion failed for $file, creating placeholder"
        base_name=$(basename "$file" .puml)
        # Create a simple placeholder text file that can be converted later
        echo "[Diagram placeholder for $base_name]" > "${base_name}.png.txt"
    }
  fi
done
echo "Diagram conversion completed"
