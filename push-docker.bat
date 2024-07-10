@echo off
chcp 65001
if "%1" equ "" goto paramError
call docker build -t fugary/simple-boot-mock-server:%1 .
call docker build -t fugary/simple-boot-mock-server:latest .
call docker push fugary/simple-boot-mock-server:%1
call docker push fugary/simple-boot-mock-server:latest
echo 推送Docker成功：%1
goto :EOF
:paramError
echo 没有指定版本号，请示用push-docker.bat 1.0.0格式调用

