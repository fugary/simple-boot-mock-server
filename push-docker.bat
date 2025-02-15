@echo off
chcp 65001
if "%1" equ "" goto paramError
:: 创建并切换到一个支持多架构的 Buildx builder（如果已存在可省略此步骤）
docker buildx create --name multiarchbuilder --use --bootstrap
:: 利用 Buildx 构建并推送支持 linux/amd64 和 linux/arm64 架构的镜像
echo 构建 Docker 镜像：fugary/simple-boot-mock-server:%1
docker buildx build --platform linux/amd64,linux/arm64 -t fugary/simple-boot-mock-server:%1 --push .
echo 构建 Docker 镜像：fugary/simple-boot-mock-server:latest
docker buildx build --platform linux/amd64,linux/arm64 -t fugary/simple-boot-mock-server:latest --push .
echo 推送 Docker 镜像成功：%1
goto :EOF
:paramError
echo 没有指定版本号，请使用 push-docker.bat 1.0.0 格式调用
