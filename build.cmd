@echo off
pushd %~dp0

rem ===== クリーン =====
if exist release rmdir /s /q release
mkdir release

rem ===== BukkitDev向けリリースファイルの作成 =====
move /y pom.xml pom.xml.backup
java -jar XmlSetter.jar pom.xml.backup pom.xml release.lang en
call mvn clean deploy
pushd target
ren Undine-*-dist.zip Undine-*-en.zip
popd
move /y target\Undine-*-en.zip release\

rem ===== 日本フォーラム向けリリースファイルの作成 =====
java -jar XmlSetter.jar pom.xml.backup pom.xml release.lang ja
call mvn clean javadoc:jar source:jar deploy
pushd target
ren Undine-*-dist.zip Undine-*-ja.zip
popd
move /y target\Undine-*-ja.zip release\

rem ===== 後片付け =====
move /y pom.xml.backup pom.xml

popd
