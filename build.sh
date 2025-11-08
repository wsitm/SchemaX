#!/bin/bash

# 注意！这个构建是将 前端代码和后端代码构建成一个jar文件


# 判断node.js mvn是否存在
command -v npm >/dev/null 2>&1 || { echo >&2 "I require node.js v14.16.0+ but it's not installed.  Aborting."; sleep 5; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo >&2 "I require maven 3.5 + but it's not installed.  Aborting."; sleep 5; exit 1; }

# 工程根目录
BuildDir=`pwd`
ServerDir="${BuildDir}/schemax-code"
WebDir="${BuildDir}/schemax-ui"

cd `dirname $0`

echo "build web"
cd ${WebDir}
# 构建前端
npm run build:prod


# 这是在springboot内部放置静态文件的目录，按照springboot已定义静态文件目录的优先级放置的
WebStaticPath=${ServerDir}/src/main/resources/META-INF/resources
echo "publish web to springboot ${WebStaticPath}"
mkdir -p ${WebStaticPath}
cp -r ${WebDir}/dist/* ${WebStaticPath}


echo "build springboot"
cd ${BuildDir}
# 清理打包
mvn clean package -DskipTests
echo "Package finish, Dir is “${ServerDir}/target” "

# 移除前端静态文件
rm -rf ${WebStaticPath}
