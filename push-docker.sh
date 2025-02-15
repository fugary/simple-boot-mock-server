#!/bin/bash

# 设置字符集为 UTF-8
export LANG=en_US.UTF-8

# 检查是否提供了版本号参数
if [ -z "$1" ]; then
  echo "没有指定版本号，请使用 ./push-docker.sh 1.0.0 格式调用"
  exit 1
fi

# 获取版本号
VERSION=$1

# 构建并推送指定版本的 Docker 镜像
echo "构建 Docker 镜像：fugary/simple-boot-mock-server:$VERSION"
docker buildx build --platform linux/amd64,linux/arm64 -t fugary/simple-boot-mock-server:$VERSION --push .

echo "构建 Docker 镜像：fugary/simple-boot-mock-server:latest"
docker buildx build --platform linux/amd64,linux/arm64 -t fugary/simple-boot-mock-server:latest --push .

# 显示成功信息
echo "推送 Docker 镜像成功：$VERSION"

exit 0
