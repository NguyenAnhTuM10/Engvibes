# Load env vars from .env
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^#][^=]*)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], "Process")
    }
}

Set-Location backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
