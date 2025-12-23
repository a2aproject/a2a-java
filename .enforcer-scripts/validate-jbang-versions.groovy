// Fetch the property from the Maven project
def scriptName = project.properties['jbang.script.name']

// Fail if the script property is missing
if (scriptName == null) {
    System.err.println("[ERROR] JBang validator: No jbang.script.name set in properties")
    throw new IllegalStateException("[ERROR] JBang validator: No jbang.script.name set in properties")
}

def jbangFile = new File(project.basedir, scriptName)
if (!jbangFile.exists()) {
    // If a script name was explicitly provided but doesn't exist, fail.
    // If using the fallback, we might want to just skip (return true).
    System.err.println("[INFO] JBang validator: File not found: " + jbangFile.absolutePath)
    throw new IllegalStateException("[ERROR] JBang validator: File not found: " + jbangFile.absolutePath)
}

def expectedVersion = project.version
def groupPrefix = "//DEPS io.github.a2asdk:"
def success = true

jbangFile.eachLine { line ->
    if (line.trim().startsWith(groupPrefix)) {
        def lastColon = line.lastIndexOf(":")
        if (lastColon != -1) {
            def actualVersion = line.substring(lastColon + 1).trim()
            if (actualVersion != expectedVersion) {
                System.err.println("[ERROR] JBang Version Mismatch in " + scriptName)
                System.err.println("  Expected: " + expectedVersion)
                System.err.println("  Found:    " + actualVersion)
                success = false
            }
        }
    }
}

if (!success) {
    throw new IllegalStateException("[ERROR] JBang version validation failed")
}