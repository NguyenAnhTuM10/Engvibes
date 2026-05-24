# Load env vars from .env
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^#][^=]*)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], "Process")
    }
}

Set-Location backend
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
