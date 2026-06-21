# CropDeal — Local Startup Order

When running the application locally (outside Docker), always start services in this exact sequence:

1. config-server       (port 8888) — must be fully up before anything else fetches config
2. eureka-server       (port 8761) — must be up before any service tries to register
3. Business services   (any order, ports 8081–8086):
   - user-service
   - crop-service
   - order-service
   - payment-service
   - notification-service
   - admin-service
4. api-gateway         (port 8080) — must start AFTER all services are registered in Eureka,
                                     otherwise lb:// routes resolve to empty and requests fail
5. Angular frontend    (port 4200) — start last with `npm start`

## Why this order matters
- Spring Cloud Config clients (all services + gateway) call config-server on startup.
  If config-server is not up, services fail to start with a connection error.
- The gateway uses Eureka service discovery (lb:// URIs). If it starts before services
  register, the route table is empty and all proxied requests return 503 until the next
  refresh cycle.
- Starting the gateway before Eureka means it cannot register itself or discover any
  downstream service — routing will fail entirely.

## Verification checkpoints
- config-server ready : GET http://localhost:8888/actuator/health → {"status":"UP"}
- eureka-server ready : GET http://localhost:8761/actuator/health → {"status":"UP"}
- service registered  : visible at http://localhost:8761 (Eureka dashboard)
- gateway ready       : GET http://localhost:8080/actuator/health → {"status":"UP"}
