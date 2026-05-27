# CareSync Backend - Quick Start Guide

## ⚡ 5-Minute Quick Start

### Prerequisites
- Docker & Docker Compose installed
- Git installed
- Basic terminal knowledge

### Step 1: Clone the Repository
```bash
git clone https://github.com/yourusername/caresync-backend.git
cd caresync-backend
```

### Step 2: Setup Environment
```bash
cp .env.example .env
# Edit .env with your Gmail app password (optional for email verification)
```

### Step 3: Start with Docker Compose
```bash
docker-compose up --build
```

The application will be available at:
- **API**: http://localhost:8080/api/v1
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/api/v1/auth/health

## 🧪 Testing the Application

### 1. Register a User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "John Doe",
    "email": "john@example.com",
    "password": "SecurePass@123",
    "confirmPassword": "SecurePass@123",
    "dateOfBirth": "1990-01-15",
    "gender": "MALE",
    "height": 175.5,
    "weight": 75.0,
    "country": "USA",
    "occupation": "Software Engineer"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "User registered successfully. Please verify your email.",
  "data": {
    "id": 1,
    "fullName": "John Doe",
    "email": "john@example.com",
    "dateOfBirth": "1990-01-15",
    "age": 33,
    "gender": "MALE",
    "height": 175.5,
    "weight": 75.0,
    "country": "USA",
    "occupation": "Software Engineer",
    "isEmailVerified": false,
    "isActive": true,
    "createdAt": "2026-05-27T10:30:00"
  }
}
```

### 2. Verify Email with OTP

First, check your email for the OTP. For development, you can view it in the logs:
```bash
docker-compose logs caresync-backend | grep "OTP generated"
```

Then verify:
```bash
curl -X POST http://localhost:8080/api/v1/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "otp": "123456",
    "otpType": "EMAIL_VERIFICATION"
  }'
```

### 3. Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePass@123"
  }'
```

**Response includes JWT tokens:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": { ... }
  }
}
```

### 4. Access Protected Endpoints

Use the access token in authorization header:
```bash
curl -X GET http://localhost:8080/api/v1/users/profile \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 5. Password Reset

Forgot password:
```bash
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com"
  }'
```

Reset password with OTP:
```bash
curl -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "otp": "123456",
    "newPassword": "NewSecurePass@456",
    "confirmPassword": "NewSecurePass@456"
  }'
```

## 📊 API Endpoints Summary

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|----------------|
| POST | `/auth/register` | Register new user | No |
| POST | `/auth/login` | Login user | No |
| POST | `/auth/verify-otp` | Verify OTP | No |
| POST | `/auth/forgot-password` | Request password reset | No |
| POST | `/auth/reset-password` | Reset password | No |
| POST | `/auth/refresh-token` | Refresh access token | Yes |
| POST | `/auth/logout` | Logout user | Yes |
| GET | `/auth/health` | Health check | No |
| GET | `/users/profile` | Get user profile | Yes |
| GET | `/users/{id}` | Get user by ID | Yes |
| PUT | `/users/profile` | Update profile | Yes |
| DELETE | `/users/profile` | Delete account | Yes |

## 🔍 API Documentation

Access interactive Swagger UI at: **http://localhost:8080/swagger-ui.html**

Features:
- Try out all endpoints directly
- See request/response schemas
- Automatic parameter validation
- Authorization header support

## 📁 Project Structure Quick Reference

```
caresync-backend/
├── src/main/java/com/caresync/
│   ├── config/           # Spring configurations & security
│   ├── controller/       # REST API endpoints
│   ├── dto/             # Data transfer objects
│   ├── entity/          # JPA entities
│   ├── exception/       # Exception handling
│   ├── mapper/          # Entity mappers
│   ├── repository/      # Data access layer
│   ├── security/        # JWT & authentication
│   ├── service/         # Business logic
│   └── util/            # Utility classes
├── src/main/resources/
│   ├── application.yml  # Main config
│   ├── application-dev.yml
│   ├── application-prod.yml
└── pom.xml             # Maven dependencies
```

## 🛠️ Local Development Setup

For local development without Docker:

### 1. Install Prerequisites
- Java 21: https://adoptium.net/
- Maven 3.9+: https://maven.apache.org/download.cgi
- MySQL 8.0+: https://dev.mysql.com/downloads/mysql/

### 2. Setup MySQL
```bash
# Create database
mysql -u root -p -e "CREATE DATABASE caresync_db CHARACTER SET utf8mb4;"
```

### 3. Clone and Build
```bash
git clone https://github.com/yourusername/caresync-backend.git
cd caresync-backend

# Build with Maven
mvn clean install

# Run application
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### 4. Access Application
- API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/swagger-ui.html

## 🐛 Common Issues

### Issue: "Connection refused" at localhost:3306
**Solution**: Make sure MySQL container is running
```bash
docker-compose ps
docker-compose logs mysql
```

### Issue: Port 8080 already in use
**Solution**: Change port in `application.yml`
```yaml
server:
  port: 8081
```

### Issue: Email not being sent
**Solution**: Configure Gmail credentials in `.env`
1. Visit https://myaccount.google.com/apppasswords
2. Generate app password
3. Update MAIL_USERNAME and MAIL_PASSWORD in .env

### Issue: OTP not received
**Solution**: Check email spam folder or view in application logs
```bash
docker-compose logs caresync-backend | grep OTP
```

## 📚 Additional Resources

- [Full README](README.md) - Comprehensive documentation
- [Build & Deployment Guide](BUILD_AND_DEPLOYMENT_GUIDE.md) - Detailed deployment instructions
- [API Documentation](http://localhost:8080/swagger-ui.html) - Interactive API docs
- [Spring Boot Docs](https://spring.io/projects/spring-boot) - Official documentation

## 🚀 Next Steps

### Phase 2: Weather Integration
- Integrate weather API
- Store weather data
- Create health recommendations based on weather

### Phase 3: Water Tracker
- Add water intake tracking
- Create hydration goals
- Send hydration reminders

### Phase 4: Frontend
- Build React frontend
- Integrate with backend APIs
- Deploy with backend

## 💡 Development Tips

1. **Use Swagger UI** for testing APIs during development
2. **Check logs** for debugging: `docker-compose logs -f`
3. **Use health endpoint** to verify service is ready
4. **Study error responses** to understand API behavior
5. **Keep JWTs in headers** when testing authentication

## 🤝 Support & Contribution

- **Issues**: [GitHub Issues](https://github.com/yourusername/caresync-backend/issues)
- **Documentation**: Check [BUILD_AND_DEPLOYMENT_GUIDE.md](BUILD_AND_DEPLOYMENT_GUIDE.md)
- **Email**: support@caresync.com

---

**Happy coding! 🎉**
