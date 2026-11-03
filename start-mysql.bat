@echo off
REM Starts the local MySQL server for BloodCare (leave this window open while using the app).
"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" --datadir="%USERPROFILE%\bloodcare-mysql-data" --bind-address=127.0.0.1 --console
