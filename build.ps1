param(
    [ValidateSet('test','run','baseline','report')][string]$Task = 'test',
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$Workload,
    [int[]]$Quanta = @(5,10)
)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$javac = if ($JavaHome) { Join-Path $JavaHome 'bin\javac.exe' } else { 'javac' }
$java = if ($JavaHome) { Join-Path $JavaHome 'bin\java.exe' } else { 'java' }
$output = if ($Task -eq 'baseline') { 'build/baseline' } else { 'build/classes' }
New-Item -ItemType Directory -Force $output | Out-Null
$sources = if ($Task -eq 'baseline') { @(Get-ChildItem 'baseline/*.java') + @(Get-Item 'tests/RegressionTests.java') } else { @(Get-ChildItem 'src/*.java') + @(Get-ChildItem 'tests/*.java') + @(Get-ChildItem 'tools/*.java') }
& $javac --release 17 -d $output $sources.FullName
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($Task -eq 'report') {
    & $java -cp $output GenerateReport
} elseif ($Task -eq 'run') {
    if ($Workload) { & $java -cp $output scheduling_algorithms $Workload @Quanta }
    else { & $java -cp $output scheduling_algorithms }
} else {
    & $java -cp $output RegressionTests
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    if ($Task -ne 'baseline') { & $java -cp $output EngineTests }
}
exit $LASTEXITCODE
