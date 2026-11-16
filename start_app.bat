@echo off
echo Starting Spring Boot Backend...
start "Spring Boot Backend" cmd /k "cd ""demo (7)\demo"" && mvnw.cmd spring-boot:run"

echo Starting Next.js Frontend...
start "Next.js Frontend" cmd /k "cd ""frontend\frontend-quanlybanhang"" && npm run dev"

echo Both applications are starting in separate windows.
