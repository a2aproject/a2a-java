
# A2A Java SDK Documentation

A static site built with [Roq](https://iamroq.dev), a static site generator powered by Quarkus.

## Getting Started

Install the [Roq CLI](https://iamroq.dev/docs/getting-started/) (installs [JBang](https://www.jbang.dev/download) if needed):
```bash
curl -Ls https://sh.jbang.dev | bash -s - app install --fresh --force roq@quarkiverse/quarkus-roq
```

Then:
```bash
# Start dev mode with live-reload
roq start

# Build the static site
roq generate

# Preview the generated site
roq serve
```

Dev mode runs on http://localhost:8080 (use `-p` to change the port).

## Project Structure

```
content/           Pages and collections (posts/, etc.)
templates/
  layouts/         Page layouts
  partials/        Reusable template fragments
data/              Structured data files (YAML/JSON)
public/            Static assets served as-is (images, etc.)
web/               JS/CSS sources (bundled by Quarkus Web Bundler)
config/
  application.properties
```

## Useful Commands

```bash
roq add plugin:tagging     # Add a plugin
roq add theme:default      # Add a theme
roq update                 # Update to latest Roq version
```

## AI Coding Assistants

Give your AI assistant full context about Roq by pointing it to https://iamroq.dev/llms.txt.

For detailed skill files, run:
```bash
mvn dependency:list -DincludeGroupIds=io.quarkiverse.roq -DoutputAbsoluteArtifactFilename=true
```
Then extract skills from the `*-deployment` JARs listed in the output:
```bash
unzip -p PATH_TO_JAR META-INF/quarkus-skill.md > .claude/skills/SKILL_NAME.md
```

## Learn More

- [Roq Documentation](https://iamroq.dev/docs/)
- [Qute Template Reference](https://quarkus.io/guides/qute-reference)
- [Quarkus](https://quarkus.io/)
