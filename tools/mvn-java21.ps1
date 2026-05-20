param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

$env:JAVA_HOME = "D:\Program Files (x86)\jdk-21.0.11"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:MAVEN_OPTS = "-Xshare:off -Xms128m -Xmx512m"

& "D:\Program Files\apache-maven-3.6.3\bin\mvn.cmd" @MavenArgs
exit $LASTEXITCODE
