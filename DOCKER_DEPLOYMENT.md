c# Docker Deployment Guide

Complete guide to deploy the ECD Payment Reconciliation System using Docker.

## Prerequisites

- Docker installed (version 20.10+)
- Docker Compose installed (version 2.0+)
- At least 2GB of free RAM
- Ports 3000, 8080, and 5432 available

## Quick Start

### 1. Clone the repository (if not already done)
```bash
cd /Users/palesamolefe/projects/katlehouniversity
```

### 2. Build and start all services
```bash
docker-compose up -d --build
```

This command will:
- Build the Spring Boot backend
- Build the React frontend
- Start PostgreSQL database
- Start all services in detached mode

### 3. Wait for services to be healthy
```bash
# Check service status
docker-compose ps

# Watch logs
docker-compose logs -f
```

### 4. Access the application

Once all services are running:

**Frontend Application:**
- **URL:** http://localhost:3000
- Login with default credentials:
  - Username: `admin`
  - Password: `admin123`

**Backend API:**
- **URL:** http://localhost:8080/api
- **Health Check:** http://localhost:8080/actuator/health

**Database:**
- **Host:** localhost
- **Port:** 5432
- **Database:** ecd_payment_db
- **Username:** ecd_user
- **Password:** ecd_password_change_in_production

## Service Architecture

```
┌─────────────────┐
│   Frontend      │ (Port 3000)
│   React + Nginx │
└────────┬────────┘
         │
         │ Proxied /api requests
         ▼
┌─────────────────┐
│   Backend       │ (Port 8080)
│   Spring Boot   │
└────────┬────────┘
         │
         │ JDBC
         ▼
┌─────────────────┐
│   Database      │ (Port 5432)
│   PostgreSQL    │
└─────────────────┘
```

## Docker Commands

### Start services
```bash
docker-compose up -d
```

### Stop services
```bash
docker-compose down
```

### Stop and remove volumes (WARNING: deletes all data)
```bash
docker-compose down -v
```

### Rebuild and restart
```bash
docker-compose up -d --build
```

### View logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
```

### Check service status
```bash
docker-compose ps
```

### Execute commands in containers
```bash
# Backend shell
docker-compose exec backend sh

# Database shell
docker-compose exec postgres psql -U ecd_user -d ecd_payment_db

# Frontend shell
docker-compose exec frontend sh
```

## Configuration

### Environment Variables

Edit `docker-compose.yml` to configure:

#### Backend Configuration
```yaml
environment:
  # Database
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/ecd_payment_db
  SPRING_DATASOURCE_USERNAME: ecd_user
  SPRING_DATASOURCE_PASSWORD: your_secure_password

  # JWT Secret (CHANGE IN PRODUCTION!)
  JWT_SECRET: your-super-secret-256-bit-key

  # Standard Bank API
  STANDARDBANK_CLIENT_ID: your-client-id
  STANDARDBANK_CLIENT_SECRET: your-client-secret
```

#### PostgreSQL Configuration
```yaml
environment:
  POSTGRES_DB: ecd_payment_db
  POSTGRES_USER: ecd_user
  POSTGRES_PASSWORD: your_secure_password
```

### Port Configuration

To change ports, edit `docker-compose.yml`:

```yaml
services:
  frontend:
    ports:
      - "8000:80"  # Change 3000 to 8000

  backend:
    ports:
      - "9090:8080"  # Change 8080 to 9090

  postgres:
    ports:
      - "5433:5432"  # Change 5432 to 5433
```

## Production Deployment

### 1. Security Hardening

**Update docker-compose.yml**:

```yaml
services:
  postgres:
    environment:
      POSTGRES_PASSWORD: ${DB_PASSWORD}  # Use env var
    # Remove port exposure if not needed externally
    # ports:
    #   - "5432:5432"

  backend:
    environment:
      JWT_SECRET: ${JWT_SECRET}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      STANDARDBANK_CLIENT_SECRET: ${STANDARDBANK_CLIENT_SECRET}
```

**Create .env file** (DO NOT commit to git):

```bash
# .env file
DB_PASSWORD=very_secure_database_password_12345
JWT_SECRET=super-secret-jwt-key-minimum-256-bits-random-string
STANDARDBANK_CLIENT_ID=actual-client-id
STANDARDBANK_CLIENT_SECRET=actual-client-secret
```

### 2. Use Production Images

For production, build optimized images:

```bash
# Build with production target
docker-compose -f docker-compose.prod.yml build

# Push to registry (optional)
docker tag ecd-backend:latest your-registry/ecd-backend:v1.0.0
docker push your-registry/ecd-backend:v1.0.0
```

### 3. Enable HTTPS

Use a reverse proxy (Nginx, Traefik, Caddy) in front:

```yaml
# docker-compose.prod.yml
services:
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    depends_on:
      - frontend
```

### 4. Set Resource Limits

```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

## Database Management

### Backup Database

```bash
# Create backup
docker-compose exec postgres pg_dump -U ecd_user ecd_payment_db > backup.sql

# Or using docker directly
docker exec ecd-postgres pg_dump -U ecd_user ecd_payment_db > backup_$(date +%Y%m%d).sql
```

### Restore Database

```bash
# Stop backend to prevent conflicts
docker-compose stop backend

# Restore
cat backup.sql | docker-compose exec -T postgres psql -U ecd_user -d ecd_payment_db

# Restart backend
docker-compose start backend
```

### Database Migrations

For production, use Flyway or Liquibase:

1. Add dependency to `backend/pom.xml`:
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

2. Create migration scripts in `backend/src/main/resources/db/migration/`

## Monitoring

### Health Checks

Check service health:

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

### Container Stats

```bash
# View resource usage
docker stats

# Specific container
docker stats ecd-backend
```

### Logs

```bash
# Follow all logs
docker-compose logs -f

# Last 100 lines
docker-compose logs --tail=100

# Specific service since timestamp
docker-compose logs --since 2024-01-01T00:00:00 backend
```

## Troubleshooting

### Services won't start

**Check logs:**
```bash
docker-compose logs backend
docker-compose logs postgres
```

**Common issues:**
- Port already in use: Change port in docker-compose.yml
- Database not ready: Wait for postgres health check to pass
- Build errors: Check Java 21 is available in build image

### Frontend can't reach backend

**Check:**
1. Backend is running: `docker-compose ps`
2. Backend health: `curl http://localhost:8080/actuator/health`
3. Network connectivity: `docker-compose exec frontend ping backend`

**Fix:**
- Ensure all services are on the same network
- Check nginx proxy configuration in `frontend/nginx.conf`

### Database connection issues

**Check:**
```bash
# Test database connection
docker-compose exec backend sh
# Inside container:
nc -zv postgres 5432
```

**Fix:**
- Verify SPRING_DATASOURCE_URL in docker-compose.yml
- Check postgres is healthy: `docker-compose ps postgres`
- View postgres logs: `docker-compose logs postgres`

### Backend build fails

**Common causes:**
- Java version mismatch
- Maven dependency issues
- Out of memory

**Fix:**
```bash
# Clear Maven cache
docker-compose build --no-cache backend

# Increase Docker memory (Docker Desktop)
# Settings -> Resources -> Memory (set to 4GB+)
```

### Frontend build fails

**Fix:**
```bash
# Clear npm cache
docker-compose build --no-cache frontend

# Check Node version in Dockerfile
```

## Scaling

### Horizontal Scaling

Scale backend instances:

```yaml
services:
  backend:
    deploy:
      replicas: 3
```

Or use:
```bash
docker-compose up -d --scale backend=3
```

Add load balancer (Nginx):

```yaml
services:
  loadbalancer:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx-lb.conf:/etc/nginx/nginx.conf
```

## Cleanup

### Remove stopped containers
```bash
docker-compose down
```

### Remove everything including volumes
```bash
docker-compose down -v
```

### Clean up Docker system
```bash
# Remove unused images
docker image prune -a

# Remove unused volumes
docker volume prune

# Remove everything unused
docker system prune -a --volumes
```

## CI/CD Integration

### GitHub Actions Example

```yaml
# .github/workflows/deploy.yml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Build images
        run: docker-compose build

      - name: Push to registry
        run: |
          echo ${{ secrets.REGISTRY_PASSWORD }} | docker login -u ${{ secrets.REGISTRY_USERNAME }} --password-stdin
          docker-compose push

      - name: Deploy to server
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SSH_KEY }}
          script: |
            cd /app
            docker-compose pull
            docker-compose up -d
```

## Performance Tuning

### Backend JVM Options

Edit `backend/Dockerfile`:

```dockerfile
ENTRYPOINT ["java", \
  "-Xms512m", \
  "-Xmx1g", \
  "-XX:+UseG1GC", \
  "-jar", "app.jar"]
```

### PostgreSQL Tuning

Add to docker-compose.yml:

```yaml
services:
  postgres:
    command:
      - "postgres"
      - "-c"
      - "max_connections=200"
      - "-c"
      - "shared_buffers=256MB"
```

## Support

For issues:
1. Check logs: `docker-compose logs`
2. Verify configuration in docker-compose.yml
3. Check Docker daemon is running
4. Ensure sufficient disk space and memory

## Next Steps

After successful deployment:

1. Change default passwords
2. Configure Standard Bank API credentials
3. Set up SSL/TLS certificates
4. Configure automated backups
5. Set up monitoring (Prometheus, Grafana)
6. Configure log aggregation (ELK stack)
7. Implement automated health checks

## Quick Reference

| Service | URL | Default Credentials |
|---------|-----|---------------------|
| Frontend | http://localhost:3000 | admin / admin123 |
| Backend API | http://localhost:8080/api | - |
| Health Check | http://localhost:8080/actuator/health | - |
| PostgreSQL | localhost:5432 | ecd_user / ecd_password_change_in_production |

**Important:** Change all default passwords before deploying to production!
