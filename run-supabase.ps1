# Starts IOPMS against Supabase (transaction pooler — the direct connection is IPv6-only
# and most home networks can't reach it).
# Usage:  .\run-supabase.ps1
# Reads DB_PASSWORD from the environment if set, otherwise prompts (not saved anywhere).

$env:DB_URL      = "jdbc:postgresql://aws-0-ap-northeast-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0"
$env:DB_USER     = "postgres.xkfruqyvujflgmyclsab"   # transaction pooler wants postgres.<project-ref>
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = Read-Host "Supabase DB password" }
if (-not $env:JWT_SECRET)  { $env:JWT_SECRET  = -join ((48..57)+(97..122) | Get-Random -Count 48 | ForEach-Object {[char]$_}) }

mvn -q -B spring-boot:run    # no dev profile — that would switch back to H2
