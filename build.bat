@echo off
REM Compiles and runs BloodCare.  Put mysql-connector-j-*.jar inside the lib folder first.
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -cp "lib/*" -d out src\com\bloodcare\*.java src\com\bloodcare\db\*.java src\com\bloodcare\model\*.java src\com\bloodcare\ui\*.java
if errorlevel 1 exit /b 1
if "%1"=="compile" exit /b 0
java -cp "out;lib/*" com.bloodcare.Main
