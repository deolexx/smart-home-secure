@echo off
cd /d %~dp0\device-simulator
if not exist target\device-simulator-1.0.0-jar-with-dependencies.jar (
  mvn -q -DskipTests package
)
echo Running device simulator...
java -jar target\device-simulator-1.0.0-jar-with-dependencies.jar

