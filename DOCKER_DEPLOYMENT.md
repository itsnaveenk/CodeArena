# Docker Deployment Guide

This guide explains how to deploy CodeArena using Docker in a single command.

---

## 🚀 Quick Start

### Deploy Everything with One Command

```bash
./deploy.sh
```

That's it! The script will:
1. ✅ Build all Docker images
2. ✅ Start all services (PostgreSQL, MongoDB, Redis, Backend, Frontend)
3. ✅ Wait for services to be healthy
4. ✅ Show you the URLs to access

---

## 📋 Prerequisites

**Required:**
- Docker 20.10+
- Docker Compose 2.0+

**Optional:**
- Judge0 API key (or use local fallback)

---

## ⚙️ Configuration

### 1. Create Environment File

```bash
cp .env.docker .env
```

### 2. Edit `.env` File

**Required variables:**

```bash
# Database passwords
DATABASE_PASSWORD=your_secure_postgres_password
MONGO_PASSWORD=your_secure_mongo_password
REDIS_PASSWORD=your_secure_redis_password

# JWT secret (generate with: openssl rand -base64 48)
JWT_SECRET=your_jwt_secret_minimum_32_characters

# Judge0 (optional - falls back to local execution)
JUDGE0_API_KEY=your_judge0_api_key
```

**Generate secure JWT secret:**
```bash
openssl rand -base64 48
```

---

## 🎯 Deployment Commands

### Start Services
```bash
./deploy.sh up
# or simply
./deploy.sh
```

### Stop Services
```bash
./deploy.sh down
```

### Restart Services
```bash
./deploy.sh restart
```

### View Logs
```bash
./deploy.sh logs

# Or for specific service
docker-compose logs -f backend
docker-compose logs -f frontend
```

### Clean Everything (⚠️ Destroys all data)
```bash
./deploy.sh clean
```

---

## 📡 Access Points

After deployment, access:

| Service | URL | Description |
|---------|-----|-------------|
| **Frontend** | http://localhost:80 | Main application |
| **Backend API** | http://localhost:8080 | REST API |
| **API Docs** | http://localhost:8080/swagger-ui.html | Interactive API explorer |
| **Health Check** | http://localhost:8080/actuator/health | Service health status |

---

## 🏗️ Architecture

The Docker deployment includes:

```
┌─────────────────────────────────────────┐
│         Docker Compose Stack            │
├─────────────────────────────────────────┤
│                                         │
│  ┌──────────┐  ┌─────────┐            │
│  │ Frontend │  │ Backend │            │
│  │  nginx   │  │ Java 21 │            │
│  │  :80     │  │ :8080   │            │
│  └──────────┘  └─────────┘            │
│       │            │                   │
│       └────────────┼──────┐            │
│                    │      │            │
│              ┌─────▼──┐   │            │
│              │ Redis  │   │            │
│              │  :6379 │   │            │
│              └────────┘   │            │
│                           │            │
│              ┌─────────┐  │            │
│              │ MongoDB │◄─┘            │
│              │ :27017  │               │
│              └─────────┘               │
│                                        │
│              ┌──────────┐              │
│              │PostgreSQL│              │
│              │  :5432   │              │
│              └──────────┘              │
│                                        │
└─────────────────────────────────────────┘
```

---

## 📦 What Gets Built

### Backend Image
- **Base:** Eclipse Temurin 21 JRE (Alpine)
- **Size:** ~300MB
- **Features:**
  - Multi-stage build (Maven → JRE)
  - Non-root user
  - Health checks
  - Optimized JVM settings

### Frontend Image
- **Base:** nginx:1.25-alpine
- **Size:** ~50MB
- **Features:**
  - Multi-stage build (Node → nginx)
  - Gzip compression
  - API proxy to backend
  - Security headers

---

## 🔒 Security Features

### Container Security
- ✅ Non-root users
- ✅ Read-only file systems where possible
- ✅ Resource limits (CPU, memory)
- ✅ Health checks

### Network Security
- ✅ Private Docker network
- ✅ Services communicate via service names
- ✅ Only frontend and backend exposed to host

### Data Persistence
- ✅ Named volumes for databases
- ✅ Survives container restarts
- ✅ Backed up separately

---

## 🔧 Troubleshooting

### Services Not Starting

**Check service status:**
```bash
docker-compose ps
```

**View logs:**
```bash
docker-compose logs backend
docker-compose logs frontend
```

**Restart specific service:**
```bash
docker-compose restart backend
```

---

### Backend Can't Connect to Database

**Check database health:**
```bash
docker-compose ps postgres
```

**Verify environment variables:**
```bash
docker-compose exec backend env | grep DATABASE
```

**Restart database:**
```bash
docker-compose restart postgres
```

---

### Frontend Shows 502 Bad Gateway

**Check backend health:**
```bash
curl http://localhost:8080/actuator/health
```

**Check nginx logs:**
```bash
docker-compose logs frontend
```

---

### Port Already in Use

**Change ports in `.env`:**
```bash
FRONTEND_PORT=3000  # instead of 80
BACKEND_PORT=8888   # instead of 8080
```

---

## 🗄️ Data Management

### Backup Data

**PostgreSQL:**
```bash
docker-compose exec postgres pg_dump -U codearena codearena > backup.sql
```

**MongoDB:**
```bash
docker-compose exec mongodb mongodump --out=/backup
docker cp codearena-mongodb:/backup ./mongodb-backup
```

**Redis:**
```bash
docker-compose exec redis redis-cli --rdb /data/backup.rdb
docker cp codearena-redis:/data/backup.rdb ./redis-backup.rdb
```

### Restore Data

**PostgreSQL:**
```bash
docker-compose exec -T postgres psql -U codearena codearena < backup.sql
```

**MongoDB:**
```bash
docker cp ./mongodb-backup codearena-mongodb:/backup
docker-compose exec mongodb mongorestore /backup
```

---

## 📊 Monitoring

### Check Resource Usage

```bash
docker stats
```

### View Container Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend

# Last 100 lines
docker-compose logs --tail=100 backend
```

### Check Health Status

```bash
# All services
docker-compose ps

# Backend health
curl http://localhost:8080/actuator/health

# Frontend health
curl http://localhost:80/
```

---

## 🚀 Production Deployment

### Recommended Changes for Production

1. **Use HTTPS**
   - Add SSL certificates
   - Configure nginx for HTTPS
   - Redirect HTTP to HTTPS

2. **External Databases**
   - Use managed PostgreSQL (RDS, CloudSQL)
   - Use MongoDB Atlas
   - Use Redis Cloud

3. **Secrets Management**
   - Use Docker secrets
   - Or environment-specific `.env` files
   - Never commit `.env` files

4. **Monitoring**
   - Add Prometheus + Grafana
   - Configure alerting
   - Set up log aggregation

5. **Backups**
   - Automated daily backups
   - Offsite backup storage
   - Test restore procedures

---

## 📝 Environment Variables Reference

See `.env.docker` for full list of configurable variables.

**Critical Settings:**

| Variable | Purpose | Example |
|----------|---------|---------|
| `DATABASE_PASSWORD` | PostgreSQL password | `SecureP@ss123` |
| `MONGO_PASSWORD` | MongoDB password | `SecureM0ng0!` |
| `REDIS_PASSWORD` | Redis password | `SecureRedis$` |
| `JWT_SECRET` | JWT signing key | `base64_encoded_string` |
| `JUDGE0_API_KEY` | Code execution API | `your_api_key` |

---

## 🆘 Getting Help

**Check logs first:**
```bash
./deploy.sh logs
```

**Verify configuration:**
```bash
cat .env
```

**Test connectivity:**
```bash
docker-compose exec backend ping postgres
docker-compose exec backend ping mongodb
docker-compose exec backend ping redis
```

---

## 📄 License

Same as main project (MIT License)
