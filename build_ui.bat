cd simple-boot-mock-ui
call npm install & npm run build & cd .. &^
echo "copy simple-boot-mock-ui static resources............" &^
rd /Q/S simple-boot-mock-server\src\main\resources\static\ &^
XCOPY simple-boot-mock-ui\dist simple-boot-mock-server\src\main\resources\static\ /S /E /Y