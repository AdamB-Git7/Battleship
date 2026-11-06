param(
    [string]$Server = "18.221.179.146",
    [string]$Key = "$env:USERPROFILE\.ssh\battleship-key.pem"
)

$ErrorActionPreference = "Stop"
$Target = "ec2-user@$Server"

.\mvnw.cmd clean package
if ($LASTEXITCODE -ne 0) { throw "Build or tests failed, nothing was deployed." }

scp -i $Key target\battleship-0.0.1-SNAPSHOT.jar "${Target}:/tmp/battleship.jar"
if ($LASTEXITCODE -ne 0) { throw "Copying the jar failed." }

ssh -i $Key $Target "sudo mv /tmp/battleship.jar /opt/battleship/battleship.jar && sudo chown battleship:battleship /opt/battleship/battleship.jar && sudo systemctl restart battleship && sudo systemctl is-active battleship"
if ($LASTEXITCODE -ne 0) { throw "Restarting the game on the server failed." }

Write-Host "Deployed. Play at http://$Server"
