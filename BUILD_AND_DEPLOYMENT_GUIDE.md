# CareSync Backend - Build and Deployment Guide

## Local Development Setup

### Step 1: Clone and Build
```bash
git clone https://github.com/yourusername/caresync-backend.git
cd caresync-backend
mvn clean install
```

### Step 2: Run with Maven
```bash
mvn spring-boot:run
```

### Step 3: Access the Application
- API Base URL: http://localhost:8080/api/v1
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/v3/api-docs

## Docker Deployment

### Option 1: Docker Compose (Recommended)

```bash
# 1. Create .env file with your credentials
cp .env.example .env
# Edit .env with your configuration

# 2. Build and start services
docker-compose up --build

# 3. View logs
docker-compose logs -f caresync-backend

# 4. Stop services
docker-compose down
```

### Option 2: Manual Docker Build

```bash
# 1. Build image
docker build -t caresync-backend:1.0.0 .

# 2. Run container
docker run -d \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/caresync_db \
  -e SPRING_DATASOURCE_USERNAME=caresync_user \
  -e SPRING_DATASOURCE_PASSWORD=caresync_pass \
  -e JWT_SECRET=your-256-bit-secret \
  -e MAIL_USERNAME=your-email@gmail.com \
  -e MAIL_PASSWORD=your-app-password \
  -p 8080:8080 \
  --name caresync-backend \
  caresync-backend:1.0.0

# 3. Check logs
docker logs -f caresync-backend

# 4. Stop container
docker stop caresync-backend
docker rm caresync-backend
```

## Database Setup

### Option 1: Local MySQL

```bash
# Start MySQL
docker run -d \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=caresync_db \
  -e MYSQL_USER=caresync_user \
  -e MYSQL_PASSWORD=caresync_pass \
  -p 3306:3306 \
  -v mysql_data:/var/lib/mysql \
  --name caresync-mysql \
  mysql:8.0
```

### Option 2: Existing MySQL Server

```bash
# Create database
mysql -u root -p -e "CREATE DATABASE caresync_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Create user
mysql -u root -p -e "CREATE USER 'caresync_user'@'localhost' IDENTIFIED BY 'caresync_pass';"
mysql -u root -p -e "GRANT ALL PRIVILEGES ON caresync_db.* TO 'caresync_user'@'localhost';"
mysql -u root -p -e "FLUSH PRIVILEGES;"
```

## Email Configuration

### Gmail Setup

1. Enable 2-Factor Authentication in Gmail
2. Generate an App-Specific Password:
   - Go to: https://myaccount.google.com/apppasswords
   - Select "Mail" and "Windows Computer"
   - Copy the generated 16-character password
3. Update environment variables:
   ```
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=<16-character-app-password>
   ```

### Other Email Providers

Update `application.yml` mail configuration:
```yaml
spring:
  mail:
    host: smtp.provider.com
    port: 587
    username: your-email@provider.com
    password: your-password
```

## Building and Running Tests

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev
```

## Maven Build Profiles

```bash
# Development profile
mvn clean install -P dev

# Production profile
mvn clean install -P prod

# Skip tests
mvn clean install -DskipTests=true
```

## IDE Setup

### IntelliJ IDEA

1. Open project in IntelliJ
2. File → Project Structure → SDK → Select JDK 21
3. Enable annotation processing: 
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Check "Enable annotation processing"
4. Run → Edit Configurations → Add Spring Boot configuration

### VS Code

1. Install extensions:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - REST Client

2. Create launch configuration in `.vscode/launch.json`:
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "CareSync Backend",
      "request": "launch",
      "mainClass": "com.caresync.CareSyncApplication",
      "projectName": "caresync-backend",
      "cwd": "${workspaceFolder}",
      "console": "integratedTerminal"
    }
  ]
}
```

## Production Deployment

### AWS EC2

```bash
# 1. Launch EC2 instance
# - AMI: Amazon Linux 2
# - Instance Type: t3.medium or higher
# - Storage: 20GB minimum

# 2. Install dependencies
sudo yum update -y
sudo yum install -y docker git
sudo usermod -a -G docker ec2-user

# 3. Clone and deploy
git clone https://github.com/yourusername/caresync-backend.git
cd caresync-backend
docker-compose up -d

# 4. Setup auto-start
sudo systemctl enable docker
sudo systemctl start docker
```

### Azure Container Instances

```bash
az group create --name caresync --location eastus

az container create \
  --resource-group caresync \
  --name caresync-backend \
  --image caresync-backend:1.0.0 \
  --ports 8080 \
  --memory 1.5 \
  --cpu 1 \
  --environment-variables \
    'SPRING_DATASOURCE_URL=server.mysql.database.azure.com' \
    'JWT_SECRET=your-secret' \
  --registry-login-server myregistry.azurecr.io \
  --registry-username username \
  --registry-password password
```

### Kubernetes Deployment

```bash
# Create namespace
kubectl create namespace caresync

# Create ConfigMap and Secrets
kubectl create configmap app-config \
  -n caresync \
  --from-file=application.yml

kubectl create secret generic app-secrets \
  -n caresync \
  --from-literal=jwt-secret=your-secret

# Deploy
kubectl apply -f k8s/deployment.yml -n caresync
kubectl apply -f k8s/service.yml -n caresync
```

## Monitoring and Troubleshooting

### Check Application Health

```bash
# Health endpoint
curl http://localhost:8080/api/v1/auth/health

# Actuator metrics
curl http://localhost:8080/actuator/metrics

# Database health
curl http://localhost:8080/actuator/health/db
```

### View Logs

```bash
# Docker logs
docker logs -f caresync-backend

# Application log file
tail -f logs/caresync.log

# Only errors
tail -f logs/caresync.log | grep ERROR
```

### Common Issues and Solutions

1. **MySQL Connection Refused**
   - Check MySQL is running: `docker ps | grep mysql`
   - Verify credentials in application.yml
   - Check firewall rules for port 3306

2. **Email Sending Failed**
   - Verify Gmail app password is correct
   - Enable "Less secure apps" if using regular password
   - Check SMTP server is reachable on port 587

3. **JWT Token Invalid**
   - Ensure JWT_SECRET is at least 32 characters
   - Check token hasn't expired within 24 hours
   - Verify token format: "Bearer <token>"

4. **Port Already in Use**
   - Change port in application.yml: server.port=8081
   - Or kill process: `lsof -i :8080 | grep LISTEN | awk '{print $2}' | xargs kill -9`

## Performance Optimization

### Database Optimization
```sql
-- Add indexes
CREATE INDEX idx_email ON users(email);
CREATE INDEX idx_created_at ON users(created_at);

-- Analyze query performance
EXPLAIN SELECT * FROM users WHERE email = 'test@example.com';
```

### Application Tuning
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 25
          fetch_size: 50
```

## Backup and Recovery

```bash
# Backup database
docker exec caresync-mysql mysqldump -u root -proot caresync_db > backup.sql

# Restore database
docker exec -i caresync-mysql mysql -u root -proot caresync_db < backup.sql

# Backup data volume
docker run --rm -v mysql_data:/data -v $(pwd):/backup busybox tar czf /backup/mysql_backup.tar.gz /data
```

## CI/CD Pipeline

### GitHub Actions

```yaml
name: Build and Deploy

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '21'
      - run: mvn clean package
      - run: docker build -t caresync-backend:${{ github.sha }} .
```

## Documentation

- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [MySQL Docs](https://dev.mysql.com/doc/)
- [Docker Docs](https://docs.docker.com/)
- [JWT Docs](https://jwt.io/)

---

**For more support, visit: https://docs.caresync.com**
