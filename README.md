# CareSync Backend

A production-grade smart healthcare platform backend built with Spring Boot 3, Java 21, and MySQL.

## 🏥 Overview

CareSync is a comprehensive healthcare management system designed to help users track and manage their health with weather-based recommendations, hydration monitoring, and personalized health insights. The backend provides robust APIs for authentication, user management, and health tracking features.

## ✨ Features

### Current Implementation
- **User Authentication**
  - Secure registration with email verification
  - JWT-based authentication
  - Login with email and password
  - Password reset with OTP verification
  - Refresh token mechanism
  - Logout functionality

- **Email Verification**
  - OTP-based email verification
  - HTML email templates
  - OTP expiry handling (5 minutes)
  - Multiple OTP types support (Registration, Password Reset)

- **Security**
  - BCrypt password encryption
  - JWT token generation and validation
  - CORS configuration
  - Role-based access control (RBAC)
  - Spring Security integration

- **API Documentation**
  - Swagger/OpenAPI UI
  - Automatic API documentation
  - Interactive API testing

- **Error Handling**
  - Global exception handler
  - Consistent error response format
  - Field-level validation
  - Proper HTTP status codes

### Coming Soon
- Weather-based health tracking
- Smart water intake calculation
- AI health assistant integration
- Emergency services
- Health analytics dashboard
- Appointment scheduling
- Doctor consultation

## 🚀 Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.2.5
- **Build Tool**: Maven
- **Database**: MySQL 8.0
- **Authentication**: JWT (JSON Web Tokens)
- **Documentation**: Swagger/OpenAPI 3.0
- **Containerization**: Docker & Docker Compose
- **Security**: Spring Security 6.0
- **Email**: Jakarta Mail (SMTP)

## 📋 Prerequisites

- Java 21 or higher
- Maven 3.9+
- MySQL 8.0+
- Docker & Docker Compose (for containerized deployment)
- Git

## 🔧 Installation

### Local Setup

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/caresync-backend.git
cd caresync-backend
```

2. **Configure MySQL**
```sql
CREATE DATABASE caresync_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'caresync_user'@'localhost' IDENTIFIED BY 'caresync_pass';
GRANT ALL PRIVILEGES ON caresync_db.* TO 'caresync_user'@'localhost';
FLUSH PRIVILEGES;
```

3. **Update application.yml**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/caresync_db
    username: caresync_user
    password: caresync_pass

email:
  from: your-email@gmail.com

jwt:
  secret: your-secret-key-min-256-bits-long
```

4. **Setup Gmail for Email (Optional)**
- Enable 2FA in Gmail
- Generate App Password: [Google Account Security](https://myaccount.google.com/apppasswords)
- Update `.env` file:
```bash
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
JWT_SECRET=your-secret-key-min-256-bits
```

5. **Build the project**
```bash
mvn clean install
```

6. **Run the application**
```bash
mvn spring-boot:run
```

The application will start at `http://localhost:8080`

## 🐳 Docker Deployment

### Using Docker Compose (Recommended)

1. **Create .env file**
```bash
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
JWT_SECRET=your-secret-key-min-256-bits
```

2. **Build and run**
```bash
docker-compose up --build
```

3. **Access the application**
- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- MySQL: `localhost:3306`

### Building Docker Image Manually

```bash
docker build -t caresync-backend:1.0.0 .

docker run -d \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/caresync_db \
  -e SPRING_DATASOURCE_USERNAME=caresync_user \
  -e SPRING_DATASOURCE_PASSWORD=caresync_pass \
  -e JWT_SECRET=your-secret-key \
  -p 8080:8080 \
  --name caresync-backend \
  caresync-backend:1.0.0
```

## 📚 API Documentation

### Access Swagger UI
Navigate to: `http://localhost:8080/swagger-ui.html`

### Authentication Endpoints

#### Register User
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "fullName": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass@123",
  "confirmPassword": "SecurePass@123",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "height": 175.5,
  "weight": 75.0,
  "country": "USA",
  "occupation": "Software Engineer",
  "phoneNumber": "+1-234-567-8900"
}
```

#### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass@123"
}
```

#### Verify OTP
```http
POST /api/v1/auth/verify-otp
Content-Type: application/json

{
  "email": "john@example.com",
  "otp": "123456",
  "otpType": "EMAIL_VERIFICATION"
}
```

#### Forgot Password
```http
POST /api/v1/auth/forgot-password
Content-Type: application/json

{
  "email": "john@example.com"
}
```

#### Reset Password
```http
POST /api/v1/auth/reset-password
Content-Type: application/json

{
  "email": "john@example.com",
  "otp": "123456",
  "newPassword": "NewSecurePass@456",
  "confirmPassword": "NewSecurePass@456"
}
```

#### Refresh Token
```http
POST /api/v1/auth/refresh-token
Authorization: Bearer <refresh_token>
```

#### Health Check
```http
GET /api/v1/auth/health
```

## 🏗️ Project Structure

```
caresync-backend/
├── src/main/java/com/caresync/
│   ├── config/              # Spring configurations
│   ├── controller/          # REST controllers
│   ├── dto/                 # Data Transfer Objects
│   ├── entity/              # JPA entities
│   ├── exception/           # Custom exceptions & global handler
│   ├── mapper/              # Entity-DTO mappers
│   ├── repository/          # Spring Data JPA repositories
│   ├── security/            # JWT & Security filters
│   ├── service/             # Business logic interfaces
│   │   └── impl/            # Service implementations
│   ├── util/                # Utility classes
│   └── CareSyncApplication.java
├── src/main/resources/
│   └── application.yml      # Spring Boot configuration
├── pom.xml                  # Maven configuration
├── Dockerfile               # Docker image definition
├── docker-compose.yml       # Docker Compose configuration
├── .gitignore              # Git ignore rules
└── README.md               # This file
```

## 🔐 Security Features

- **Password Encryption**: BCrypt with cost factor 12
- **JWT Tokens**: 
  - Access Token: 24 hours
  - Refresh Token: 7 days
- **CORS**: Configurable cross-origin requests
- **Input Validation**: Comprehensive field validation
- **SQL Injection Prevention**: Parameterized queries (JPA)
- **CSRF Protection**: Disabled for API (stateless)
- **Rate Limiting**: Ready for implementation
- **Non-Root Container User**: Docker security best practice

## 🧪 Testing the Application

### Health Check
```bash
curl http://localhost:8080/api/v1/auth/health
```

### User Registration
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Test User",
    "email": "test@example.com",
    "password": "TestPass@123",
    "confirmPassword": "TestPass@123",
    "dateOfBirth": "1990-01-01",
    "gender": "MALE"
  }'
```

### User Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass@123"
  }'
```

## 📝 Database Schema

### Users Table
```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  dob DATE,
  age INT,
  gender ENUM('MALE', 'FEMALE', 'OTHER'),
  height DOUBLE,
  weight DOUBLE,
  country VARCHAR(100),
  occupation VARCHAR(100),
  phone_number VARCHAR(20),
  profile_image_url VARCHAR(255),
  is_email_verified BOOLEAN DEFAULT FALSE,
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  last_login TIMESTAMP,
  version BIGINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_email ON users(email);
CREATE INDEX idx_created_at ON users(created_at);
```

### OTP Requests Table
```sql
CREATE TABLE otp_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(100) NOT NULL,
  otp VARCHAR(6) NOT NULL,
  otp_type ENUM('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'TWO_FACTOR'),
  is_verified BOOLEAN DEFAULT FALSE,
  attempt_count INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_email ON otp_requests(email);
CREATE INDEX idx_otp_type ON otp_requests(otp_type);
```

## 🚢 Deployment

### AWS EC2 Deployment
1. Launch EC2 instance with Amazon Linux 2
2. Install Docker and Docker Compose
3. Clone repository and update environment variables
4. Use docker-compose to deploy

### Azure Container Instances
```bash
az container create \
  --resource-group caresync \
  --name caresync-backend \
  --image caresync-backend:1.0.0 \
  --ports 8080 \
  --environment-variables \
    'SPRING_DATASOURCE_URL=<connection-string>' \
    'JWT_SECRET=<secret-key>'
```

## 📊 Monitoring & Logs

### Spring Boot Actuator
- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Info: `http://localhost:8080/actuator/info`

### Application Logs
```bash
# View logs from docker container
docker logs -f caresync-backend

# Tail application log file
tail -f logs/caresync.log
```

## 🐛 Troubleshooting

### Database Connection Error
```
Solution: Ensure MySQL is running and credentials are correct in application.yml
```

### Mail Configuration Error
```
Solution: 
1. Enable 2FA in Gmail
2. Generate App Password from: https://myaccount.google.com/apppasswords
3. Update MAIL_USERNAME and MAIL_PASSWORD in environment variables
```

### JWT Token Invalid
```
Solution: Ensure JWT_SECRET is at least 256 bits (32 characters) long
```

### Port 8080 Already in Use
```
Solution: 
1. Change port in application.yml: server.port=8081
2. Or: kill ProcessId running on port 8080
```

## 📚 Documentation Links

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JWT Introduction](https://jwt.io/)
- [Swagger/OpenAPI](https://swagger.io/specification/)
- [MySQL Documentation](https://dev.mysql.com/doc/)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the Apache License 2.0 - see [LICENSE](LICENSE) file for details.

## 👥 Support & Contact

- **Email**: support@caresync.com
- **Website**: https://caresync.com
- **Documentation**: https://docs.caresync.com
- **Issues**: [GitHub Issues](https://github.com/yourusername/caresync-backend/issues)

## 🗺️ Roadmap

### Phase 2 (Q2 2026)
- [ ] Weather API Integration
- [ ] Smart Water Tracker
- [ ] Health Metrics Dashboard

### Phase 3 (Q3 2026)
- [ ] AI Health Assistant
- [ ] Doctor Appointment System
- [ ] Emergency Services Integration

### Phase 4 (Q4 2026)
- [ ] Mobile App Beta
- [ ] Analytics Dashboard
- [ ] Advanced Health Reports

## 🎯 Scalability Considerations

- **Database Indexing**: Implemented on email and timestamps
- **Connection Pooling**: HikariCP with optimized settings
- **Stateless Architecture**: JWT-based authentication
- **Async Processing**: Spring Async for email sending
- **Caching**: Ready for Redis integration
- **Load Balancing**: Docker Swarm / Kubernetes ready
- **API Rate Limiting**: Framework in place
- **Monitoring**: Actuator endpoints configured

---

**Built with ❤️ for healthcare innovation**
