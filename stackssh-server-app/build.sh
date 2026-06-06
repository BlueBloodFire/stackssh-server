# 普通镜像构建，随系统版本构建 amd/arm
#docker build -t wangjin/stackssh-server-app:1.0 -f ./Dockerfile .

# 多版本构建，兼容 amd、arm 构建镜像
docker build --platform linux/amd64,linux/arm64 --load -t wangjin/stackssh-server-app:1.1 -f ./Dockerfile .