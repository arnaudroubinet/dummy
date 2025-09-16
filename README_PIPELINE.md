# Structurizr to Confluence Pipeline

Ce projet fournit un pipeline GitLab CI qui automatise l'export de diagrammes et de documentation depuis une instance Structurizr on-premise, convertit la documentation au format ADF d'Atlassian Confluence, et upload le tout sur Confluence.

## Architecture de la solution

La solution est composée de trois utilitaires Java distincts qui s'exécutent séquentiellement :

1. **StructurizrExporter** - Exporte les schémas et la documentation depuis Structurizr
2. **DocumentationConverter** - Convertit AsciiDoc vers Markdown puis vers ADF
3. **ConfluenceUploader** - Upload la documentation et les schémas sur Confluence

## Configuration requise

### Variables d'environnement GitLab CI

Configurez les variables suivantes dans les paramètres CI/CD de votre projet GitLab :

```
# Structurizr
STRUCTURIZR_URL=https://your-structurizr-instance.com
STRUCTURIZR_API_KEY=your-api-key
STRUCTURIZR_API_SECRET=your-api-secret
WORKSPACE_ID=1

# Confluence
CONFLUENCE_URL=https://your-confluence-instance.com
CONFLUENCE_USERNAME=your-username
CONFLUENCE_API_TOKEN=your-api-token
CONFLUENCE_SPACE_KEY=YOUR-SPACE
```

### Prérequis système

- Java 17+
- Maven 3.6+
- Accès réseau vers Structurizr et Confluence

## Structure du pipeline

Le pipeline GitLab CI (.gitlab-ci.yml) comprend trois stages :

### 1. Stage Export (`export_structurizr`)
- Se connecte à l'instance Structurizr
- Exporte les diagrammes au format PlantUML
- Exporte la documentation au format AsciiDoc
- Crée un script de conversion des diagrammes en PNG

### 2. Stage Conversion (`convert_documentation`)
- Convertit AsciiDoc vers Markdown
- Convertit Markdown vers ADF (Atlassian Document Format)
- Prépare les fichiers pour l'upload Confluence

### 3. Stage Upload (`upload_confluence`)
- Upload les diagrammes PNG comme attachments
- Upload la documentation convertie en ADF
- Remplace les références d'images dans la documentation

## Utilisation des utilitaires

### Export Structurizr

```bash
mvn exec:java -Dexec.mainClass="ar.rou.structurizr.StructurizrExporter" \
  -Dexec.args="<url> <apiKey> <apiSecret> <workspaceId>"
```

### Conversion de documentation

```bash
mvn exec:java -Dexec.mainClass="ar.rou.converter.DocumentationConverter"
```

### Upload Confluence

```bash
mvn exec:java -Dexec.mainClass="ar.rou.confluence.ConfluenceUploader" \
  -Dexec.args="<confluenceUrl> <username> <apiToken> <spaceKey> <branchName> <repositoryUrl> <commitHash>"
```

## Structure des fichiers générés

```
exports/
├── diagrams/
│   ├── system-context.puml
│   ├── container-diagram.puml
│   └── convert_diagrams.sh
└── documentation/
    ├── architecture.adoc
    └── deployment.adoc

converted/
├── markdown/
│   ├── architecture.md
│   └── deployment.md
└── adf/
    ├── architecture.json
    └── deployment.json
```

## Fonctionnalités spéciales

### Nom de page basé sur la branche Git
Les pages Confluence sont automatiquement nommées selon le format :
`<nom-de-branche> - <nom-du-document>`

### Métadonnées automatiques
Chaque page générée inclut en début de document (de manière discrète) :
- L'URL du repository source
- Le hash du commit courant 
- La date et heure de génération

Ces métadonnées permettent une traçabilité complète entre la documentation Confluence et le code source.

## Fonctionnalités implémentées

### StructurizrExporter
- ✅ Connexion à l'API Structurizr
- ✅ Export des diagrammes (avec exemple PlantUML/C4)
- ✅ Export de la documentation AsciiDoc
- ✅ Génération de script de conversion PlantUML vers PNG

### DocumentationConverter
- ✅ Conversion AsciiDoc vers Markdown
- ✅ Conversion Markdown vers ADF
- ✅ Support des éléments de base (headers, listes, emphasis, code)
- ✅ Gestion des images et liens

### ConfluenceUploader
- ✅ Upload d'attachments (diagrammes)
- ✅ Création/mise à jour de pages Confluence
- ✅ Support du format ADF
- ✅ Authentification API Token
- ✅ Nom de page basé sur la branche Git
- ✅ Métadonnées automatiques (repo, commit, date de génération)

## Personnalisation

### Ajout de nouveaux formats de conversion
Modifiez les méthodes dans `DocumentationConverter` pour supporter d'autres éléments AsciiDoc ou Markdown.

### Modification du mapping ADF
Personnalisez `MarkdownToAdfConverter` pour ajuster la conversion vers le format ADF selon vos besoins.

### Extension des fonctionnalités Confluence
Étendez `ConfluenceUploader` pour supporter des fonctionnalités avancées comme les métadonnées de page, les permissions, etc.

## Sécurité

- Utilisez des variables CI/CD masquées pour les API keys et tokens
- Les tokens Confluence doivent avoir les permissions appropriées pour créer/modifier des pages et uploader des attachments
- Vérifiez que l'accès réseau entre GitLab et vos instances Structurizr/Confluence est sécurisé

## Dépannage

### Problèmes de connexion Structurizr
- Vérifiez les credentials API
- Assurez-vous que l'URL Structurizr est accessible depuis GitLab

### Erreurs de conversion
- Vérifiez que les fichiers AsciiDoc sont bien formés
- Consultez les logs pour identifier les éléments non supportés

### Problèmes d'upload Confluence
- Vérifiez les permissions du token API
- Assurez-vous que l'espace Confluence existe
- Vérifiez les logs pour identifier les erreurs HTTP

## Améliorations futures

- Intégration avec un vrai client Structurizr (actuellement en mode démonstration)
- Support d'autres formats de sortie (PDF, Word, etc.)
- Interface de configuration plus avancée
- Gestion des versions et historique des changements
- Tests automatisés plus complets