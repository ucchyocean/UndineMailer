@echo off
pushd %~dp0

echo ===== clean up =====
if exist release rmdir /s /q release
mkdir release

echo ===== making release package for BukkitDev =====
move /y pom.xml pom.xml.backup
java -jar XmlSetter.jar pom.xml.backup pom.xml release.lang en
call mvn clean deploy
pushd target
ren UndineMailer-*-dist.zip UndineMailer-*-en.zip
popd
move /y target\UndineMailer-*-en.zip release\

echo ===== making release package for Japan User Forum =====
java -jar XmlSetter.jar pom.xml.backup pom.xml release.lang ja
call mvn clean javadoc:jar source:jar deploy
pushd target
ren UndineMailer-*-dist.zip UndineMailer-*-ja.zip
popd
move /y target\UndineMailer-*-ja.zip release\

echo ===== finalize =====
move /y pom.xml.backup pom.xml

echo ===== end =====

popd
