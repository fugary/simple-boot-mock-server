@echo off
chcp 65001
cd simple-boot-mock-newui
echo "===========================build simple-boot-mock-newui ==========================="
call npm install && npm run build && cd .. &&^
echo "copy simple-boot-mock-newui static resources............" &&^
rd /Q/S simple-boot-mock-server\src\main\resources\static\ &^
XCOPY simple-boot-mock-newui\dist simple-boot-mock-server\src\main\resources\static\ /S /E /Y
