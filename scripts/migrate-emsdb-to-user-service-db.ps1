[CmdletBinding()]
param(
    [string]$ContainerName = "my-postgres",
    [string]$SourceDatabase = "emsdb",
    [string]$TargetDatabase = "user_service_db",
    [string]$PostgresUser = "postgres"
)

$ErrorActionPreference = "Stop"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupDirectory = Join-Path $PSScriptRoot "..\backups"
$containerBackupPath = "/tmp/$SourceDatabase-user-service-$timestamp.dump"
$localBackupPath = Join-Path $backupDirectory "$SourceDatabase-user-service-$timestamp.dump"

New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null

$existingTarget = docker exec $ContainerName psql -U $PostgresUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$TargetDatabase'"
$targetExists = $existingTarget.Trim() -eq "1"
if ($targetExists) {
    $targetTableCount = docker exec $ContainerName psql -U $PostgresUser -d $TargetDatabase -tAc "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public'"
    if ([int]$targetTableCount.Trim() -gt 0) {
        throw "Database '$TargetDatabase' already contains tables. Refusing to overwrite it."
    }
    Write-Host "Using existing empty database '$TargetDatabase'."
}

Write-Host "Backing up User Service tables from '$SourceDatabase'..."
docker exec $ContainerName pg_dump -U $PostgresUser -d $SourceDatabase --format=custom --no-owner --table=public.users --table=public.roles --table=public.user_roles --file=$containerBackupPath
if ($LASTEXITCODE -ne 0) { throw "Backup failed." }

docker cp "${ContainerName}:$containerBackupPath" $localBackupPath
if ($LASTEXITCODE -ne 0) { throw "Copying the backup locally failed." }

if (-not $targetExists) {
    Write-Host "Creating '$TargetDatabase'..."
    docker exec $ContainerName createdb -U $PostgresUser $TargetDatabase
    if ($LASTEXITCODE -ne 0) { throw "Database creation failed." }
}

Write-Host "Restoring the User Service tables..."
docker exec $ContainerName pg_restore -U $PostgresUser -d $TargetDatabase --no-owner $containerBackupPath
if ($LASTEXITCODE -ne 0) { throw "Restore failed. The source database and local backup remain unchanged." }

Write-Host "Verifying copied record counts..."
docker exec $ContainerName psql -U $PostgresUser -d $TargetDatabase -v ON_ERROR_STOP=1 -c "SELECT 'users' AS entity, COUNT(*) AS record_count FROM users UNION ALL SELECT 'roles', COUNT(*) FROM roles UNION ALL SELECT 'user_roles', COUNT(*) FROM user_roles;"
if ($LASTEXITCODE -ne 0) { throw "Verification failed." }

Write-Host "Migration completed. Backup saved to: $localBackupPath"
