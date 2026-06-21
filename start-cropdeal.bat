@echo off
echo Starting CropDeal Platform...

echo Starting Config Server...
start "Config Server" cmd /k "cd /d C:\Users\yamin\cropdeal\config-server && mvn spring-boot:run"
timeout /t 20 /nobreak

echo Starting Eureka Server...
start "Eureka Server" cmd /k "cd /d C:\Users\yamin\cropdeal\eureka-server && mvn spring-boot:run"
timeout /t 20 /nobreak

echo Starting User Service...
start "User Service" cmd /k "cd /d C:\Users\yamin\cropdeal\user-service && mvn spring-boot:run"
timeout /t 15 /nobreak

echo Starting Crop Service...
start "Crop Service" cmd /k "cd /d C:\Users\yamin\cropdeal\crop-service && mvn spring-boot:run"
timeout /t 15 /nobreak

echo Starting Order Service...
start "Order Service" cmd /k "cd /d C:\Users\yamin\cropdeal\order-service && mvn spring-boot:run -DskipTests"
timeout /t 15 /nobreak

echo Starting Payment Service...
start "Payment Service" cmd /k "cd /d C:\Users\yamin\cropdeal\payment-service && mvn spring-boot:run -DskipTests"
timeout /t 15 /nobreak

echo Starting Notification Service...
start "Notification Service" cmd /k "cd /d C:\Users\yamin\cropdeal\notification-service && mvn spring-boot:run"
timeout /t 15 /nobreak

echo Starting Admin Service...
start "Admin Service" cmd /k "cd /d C:\Users\yamin\cropdeal\admin-service && mvn spring-boot:run"
timeout /t 15 /nobreak

echo Starting API Gateway...
start "API Gateway" cmd /k "cd /d C:\Users\yamin\cropdeal\api-gateway && mvn spring-boot:run"
timeout /t 20 /nobreak

echo Starting Angular Frontend...
start "Frontend" cmd /k "cd /d C:\Users\yamin\cropdeal\frontend && ng serve"

echo.
echo All services starting!
echo Wait 3 minutes then open http://localhost:4200
pause
