# 🏥 MedChain - AI-Powered Fake Medicine Detection Platform

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--M4-blue.svg)](https://spring.io/projects/spring-ai)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> A comprehensive full-stack platform to combat fake medicines in rural India using AI, QR codes, and blockchain-inspired verification.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [API Documentation](#api-documentation)
- [Contributing](#contributing)
- [License](#license)

---

## 🎯 Overview

MedChain is an AI-powered platform designed to combat the fake medicine crisis in rural India. The system enables:

- **Manufacturers** to register medicines and generate unique QR codes
- **Patients/Chemists** to verify medicine authenticity instantly
- **AI-powered** image scanning, symptom checking, and drug interaction analysis
- **Admins** to manage reports, verify manufacturers, and track fake medicine hotspots
- **Real-time** notifications via WebSocket for recalls and alerts

### Problem Statement
India faces a significant challenge with counterfeit medicines, especially in rural areas. MedChain provides an accessible, technology-driven solution to verify medicine authenticity and protect public health.

---

## ✨ Features

### 🔐 Authentication & Authorization
- Multi-role system (Admin, Manufacturer, Patient, Chemist)
- JWT-based authentication with refresh tokens
- Email verification and notifications
- Profile management

### 💊 Medicine Management
- Medicine registration with detailed information
- QR code generation using ZXing library
- Batch management and tracking
- Medicine search and categorization

### ✅ Verification System
- Instant QR code verification
- Scan history tracking with geolocation
- Redis caching for fast responses
- Multiple verdict types (Genuine, Expired, Recalled, Not Found)

### 🤖 AI-Powered Features (Spring AI + Google Gemini)
- **Image Scanning**: Analyze medicine packaging for authenticity
- **Symptom Checker**: Get AI-powered medicine recommendations
- **Drug Interactions**: Check potential medicine interactions
- **Semantic Search**: pgvector-powered intelligent medicine search

### 🚨 Reports & Recalls
- Submit fake medicine reports with photo evidence
- Admin verification workflow
- Medicine recall system with severity levels
- Geographic hotspot mapping (GeoJSON)
- Real-time WebSocket notifications

### 📊 Admin Dashboard
- Comprehensive statistics and analytics
- Manufacturer verification management
- Report management with filtering
- Interactive charts (Chart.js)
- Hotspot visualization (Leaflet maps)

### 🏭 Manufacturer Dashboard
- Medicine CRUD operations
- Bulk QR code generation
- Batch management
- Statistics overview
- Recall notifications

### ⚡ Real-time Features
- WebSocket notifications (SockJS + STOMP)
- Live recall broadcasts
- Real-time alerts
- Toast notifications

### 📱 Responsive Design
- Mobile-first responsive design
- Dark mode support
- Cross-browser compatibility

### ⏰ Scheduled Jobs
- **Daily 1 AM**: Medicine expiry checks
- **Daily 6 AM**: AI-powered fake report analysis
- **Every 6 hours**: Risk zone recalculation

---

## 🛠️ Technology Stack

### Backend
- **Java 21** - Latest LTS with virtual threads
- **Spring Boot 3.2.5** - Application framework
- **Spring AI 1.0.0-M4** - AI integration with Google Gemini
- **Spring Security** - JWT authentication & RBAC
- **Spring Data JPA** - Database ORM
- **Spring WebSocket** - Real-time communication (STOMP)
- **Spring Scheduler** - Automated tasks

### Database & Cache
- **PostgreSQL 16** - Primary database with pgvector extension
- **Redis 7** - Caching and rate limiting
- **Flyway** - Database migrations

### AI & ML
- **Google Gemini 2.5 Flash** - Multimodal AI (text + vision)
- **pgvector** - Vector similarity search for semantic queries
- **Embeddings** - Medicine semantic search

### Media & QR
- **ZXing 3.5.2** - QR code generation
- **Cloudinary** - Image storage and CDN

### Frontend
- **HTML5 + CSS3** - Modern web standards
- **Vanilla JavaScript** - No framework dependencies
- **SockJS + STOMP** - WebSocket client
- **Chart.js** - Analytics visualization
- **Leaflet** - Interactive maps

### Infrastructure
- **Docker + Docker Compose** - Containerization
- **nginx** - Reverse proxy and static file serving
- **Swagger/OpenAPI** - API documentation

---

## 🏗️ Architecture

### System Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                         Frontend                             │
│  HTML5 + CSS3 + JavaScript + WebSocket                      │
└────────────────────┬────────────────────────────────────────┘
                     │ HTTPS/WSS
┌────────────────────▼────────────────────────────────────────┐
│                      nginx (Reverse Proxy)                   │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                   Spring Boot Backend                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ REST API     │  │ WebSocket    │  │ Schedulers   │     │
│  │ (38 endpoints)│  │ (STOMP)      │  │ (3 jobs)     │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Spring AI    │  │ Security     │  │ Services     │     │
│  │ (Gemini)     │  │ (JWT)        │  │ (Business)   │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└────────┬──────────────────┬──────────────────┬─────────────┘
         │                  │                  │
    ┌────▼────┐      ┌─────▼─────┐     ┌─────▼─────┐
    │PostgreSQL│      │   Redis   │     │Cloudinary │
    │(pgvector)│      │  (Cache)  │     │ (Images)  │
    └──────────┘      └───────────┘     └───────────┘
```

### Database Schema
- **users** - User accounts with roles
- **manufacturers** - Manufacturer profiles
- **medicines** - Medicine catalog
- **medicine_units** - Individual QR codes
- **scan_logs** - Verification history
- **fake_reports** - Fake medicine reports
- **recalls** - Medicine recalls
- **notifications** - User notifications
- **refresh_tokens** - JWT refresh tokens

---

## 🚀 Quick Start

### Prerequisites
- Java 21 or higher
- Maven 3.9+
- Docker & Docker Compose
- IntelliJ IDEA (recommended) or any Java IDE

### 1. Clone Repository
```bash
git clone https://github.com/yourusername/medchain.git
cd medchain
```

### 2. Configure Environment
```bash
# Copy environment template
cp .env.example .env

# Edit .env with your credentials
# Required: GEMINI_PROJECT_ID, CLOUDINARY credentials, MAIL credentials, JWT_SECRET
```

### 3. Start Database (Docker)
```bash
# Start PostgreSQL and Redis only
docker-compose up -d postgres redis

# Verify services are running
docker-compose ps
```

### 4. Run Backend (IntelliJ IDEA)
```bash
# Option A: Using IntelliJ IDEA
1. Open project in IntelliJ IDEA
2. Wait for Maven dependencies to download
3. Run MedChainApplication.java
4. Backend will start on http://localhost:8080

# Option B: Using Maven
mvn clean install
mvn spring-boot:run
```

### 5. Serve Frontend
```bash
# Option A: Using nginx (Docker)
docker-compose up -d nginx
# Access at http://localhost:3000

# Option B: Using Python HTTP server
cd frontend
python -m http.server 3000
# Access at http://localhost:3000

# Option C: Using Node.js http-server
cd frontend
npx http-server -p 3000
# Access at http://localhost:3000
```

### 6. Access Application
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html

### 7. Login with Demo Credentials
| Role | Email | Password |
|------|-------|----------|
| Admin | admin@medchain.com | Admin@123 |
| Manufacturer | manufacturer@medchain.com | Mfr@123 |
| Patient | patient@medchain.com | Patient@123 |
| Chemist | chemist@medchain.com | Chemist@123 |

---

## 📁 Project Structure

```
medchain/
├── src/main/java/com/medchain/
│   ├── config/              # Configuration classes
│   │   ├── JwtAuthFilter.java
│   │   ├── SecurityConfig.java
│   │   ├── SpringAIConfig.java
│   │   └── WebSocketConfig.java
│   ├── controller/          # REST controllers (8 files)
│   │   ├── AuthController.java
│   │   ├── VerificationController.java
│   │   ├── MedicineController.java
│   │   ├── ReportController.java
│   │   ├── RecallController.java
│   │   ├── NotificationController.java
│   │   ├── AIController.java
│   │   └── AdminController.java
│   ├── service/             # Business logic (15 files)
│   │   ├── AuthService.java
│   │   ├── MedicineService.java
│   │   ├── QRService.java
│   │   ├── VerificationService.java
│   │   ├── ImageScanService.java
│   │   ├── SymptomCheckerService.java
│   │   └── ...
│   ├── repository/          # Data access (9 files)
│   ├── entity/              # JPA entities (9 files)
│   ├── dto/                 # Data transfer objects (20 files)
│   ├── exception/           # Custom exceptions (7 files)
│   └── scheduler/           # Scheduled jobs (3 files)
├── src/main/resources/
│   ├── application.yaml     # Application configuration
│   └── db/migration/        # Flyway migrations
├── frontend/
│   ├── index.html           # Landing page
│   ├── verify.html          # Verification page
│   ├── auth.html            # Login/Register
│   ├── dashboard.html       # Manufacturer dashboard
│   ├── admin.html           # Admin panel
│   ├── report.html          # Report submission
│   ├── symptom.html         # Symptom checker
│   ├── interactions.html    # Drug interactions
│   ├── css/                 # Stylesheets (6 files)
│   └── js/                  # JavaScript (7 files)
├── docker-compose.yml       # Docker services
├── Dockerfile               # Backend container
├── nginx.conf               # nginx configuration
├── pom.xml                  # Maven dependencies
└── .env.example             # Environment template
```

---

## 📚 API Documentation

### Authentication Endpoints
```
POST   /api/v1/auth/register      - Register new user
POST   /api/v1/auth/login         - User login
POST   /api/v1/auth/refresh       - Refresh access token
POST   /api/v1/auth/logout        - User logout
GET    /api/v1/auth/me            - Get current user
PATCH  /api/v1/auth/profile       - Update profile
```

### Verification Endpoints
```
GET    /api/v1/verify/{qrCode}    - Verify medicine by QR code
```

### Medicine Endpoints (Manufacturer)
```
POST   /api/v1/manufacturer/medicines                    - Create medicine
GET    /api/v1/manufacturer/medicines                    - Get my medicines
GET    /api/v1/manufacturer/medicines/{id}               - Get medicine details
POST   /api/v1/manufacturer/medicines/{id}/generate-qr   - Generate QR codes
GET    /api/v1/manufacturer/medicines/{id}/units         - Get medicine units
```

### AI Endpoints
```
POST   /api/v1/ai/image-scan          - Scan medicine image
POST   /api/v1/ai/symptom-check       - Check symptoms
POST   /api/v1/ai/drug-interactions   - Check drug interactions
```

### Admin Endpoints
```
GET    /api/v1/admin/dashboard                      - Dashboard statistics
GET    /api/v1/admin/reports                        - Get all reports
PATCH  /api/v1/admin/reports/{id}/verify            - Verify report
GET    /api/v1/admin/manufacturers                  - Get manufacturers
PATCH  /api/v1/admin/manufacturers/{id}/verify      - Verify manufacturer
GET    /api/v1/admin/analytics                      - Get analytics
```

**Total: 38 REST Endpoints + WebSocket**

For complete API documentation, visit: http://localhost:8080/swagger-ui.html

---

## 🧪 Testing

### Manual Testing
```bash
# Test login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"patient@medchain.com","password":"Patient@123"}'

# Test verification
curl http://localhost:8080/api/v1/verify/MEDCHAIN:550e8400-e29b-41d4-a716-446655440001

# Test AI symptom checker
curl -X POST http://localhost:8080/api/v1/ai/symptom-check \
  -H "Content-Type: application/json" \
  -d '{"symptoms":"fever and headache","language":"English"}'
```

---

## 🔧 Configuration

### Required Environment Variables
```bash
# Google Gemini AI
GEMINI_PROJECT_ID=your-google-cloud-project-id

# Cloudinary (Image Storage)
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret

# Gmail SMTP (Email Notifications)
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# JWT Secret (256+ bits)
JWT_SECRET=your-secret-key-min-256-bits-long

# Database (if not using Docker defaults)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=medchain
DB_USERNAME=medchain
DB_PASSWORD=medchain123

# Redis (if not using Docker defaults)
REDIS_HOST=localhost
REDIS_PORT=6379
```

---

## 📊 Performance Metrics

- **API Response Time**: < 200ms (cached)
- **QR Verification**: < 100ms (Redis cached)
- **AI Response Time**: 2-5 seconds (Gemini API)
- **Database Queries**: Optimized with indexes
- **Caching Strategy**: Redis (5min - 6hours TTL)
- **Rate Limiting**: 5-20 requests/hour (AI endpoints)
- **WebSocket Latency**: < 100ms

---

## 🚀 Deployment

### Docker Deployment (Full Stack)
```bash
# Build and start all services
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Production Deployment
1. Set up cloud infrastructure (AWS/Azure/GCP)
2. Configure production environment variables
3. Set up CI/CD pipeline (GitHub Actions/Jenkins)
4. Configure monitoring (Prometheus/Grafana)
5. Set up SSL certificates (Let's Encrypt)
6. Configure domain and DNS
7. Set up backup strategy
8. Perform security audit

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👥 Authors

- **Omkar Shetake** - *Initial work* - [YourGitHub](https://github.com/OmkarShetake)

---

## 🙏 Acknowledgments

- Spring AI team for the excellent AI integration framework
- Google Gemini for multimodal AI capabilities
- PostgreSQL team for pgvector extension
- All open-source contributors

---

## 📞 Support

For support, email support@medchain.com or open an issue on GitHub.

---

**Made with ❤️ for a healthier India**
