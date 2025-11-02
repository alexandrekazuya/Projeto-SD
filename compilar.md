mkdir target\classes -Force | Out-Null
$src = Get-ChildItem -Recurse -Filter *.java .\src\main\java | % FullName
javac -d .\target\classes $src
