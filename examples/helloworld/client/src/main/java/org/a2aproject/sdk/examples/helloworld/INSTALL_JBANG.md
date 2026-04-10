# Installing JBang

[JBang](https://www.jbang.dev/) is a tool that makes it easy to run Java code with zero installation. This guide provides quick installation instructions for different platforms.

## Linux and macOS

You can install JBang using `curl` or `wget`:

```bash
# Using curl
curl -Ls https://sh.jbang.dev | bash -s - app setup

# OR using wget
wget -q https://sh.jbang.dev -O - | bash -s - app setup
```

After installation, you may need to restart your terminal or source your shell configuration file:

```bash
source ~/.bashrc   # For Bash
source ~/.zshrc    # For Zsh
```

## Windows

### Using PowerShell

```powershell
iex "& { $(iwr https://ps.jbang.dev) } app setup"
```

### Using Chocolatey

```powershell
choco install jbang
```

### Using Scoop

```powershell
scoop install jbang
```

## Verifying Installation

To verify that JBang is installed correctly, run:

```bash
jbang --version
```

You should see the JBang version number displayed.

## Further Information

For more detailed installation instructions and options, visit the [JBang installation documentation](https://www.jbang.dev/documentation/guide/latest/installation.html). 