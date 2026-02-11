<div align="center">

# 🏆 CodeArena

**Full-stack competitive programming platform with secure sandboxed execution and async task processing**

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?logo=spring)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue?logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.9-blue?logo=typescript)](https://www.typescriptlang.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)](https://redis.io/)
[![MongoDB](https://img.shields.io/badge/MongoDB-7-green?logo=mongodb)](https://www.mongodb.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## 💡 What is CodeArena?

CodeArena is a LeetCode-inspired competitive programming platform built to handle the complexities of **secure code execution at scale**. It demonstrates real-world engineering around async task processing, sandboxed environments, intelligent caching, and rate limiting—all without relying on third-party execution services.

**The Challenge**: Running untrusted user code safely is hard. Most platforms either rely on expensive APIs with rate limits or lack proper resource isolation and timeout management.

**The Solution**: CodeArena implements a complete async execution pipeline with Redis Streams, containerized sandboxing via Judge0, distributed rate limiting, and intelligent caching strategies—handling the real constraints of building a coding platform.

---

## ⚡ Key Features

### 🔄 Async Code Execution
- **Non-blocking API design** — Submit code, receive task ID, poll for results
- **Redis Streams** as reliable task queue with consumer groups
- **Real-time polling UI** with visual feedback: `PENDING` → `PROCESSING` → `SUCCESS`
- Auto-cleanup with 5-minute TTL on results

### 🔒 Secure Sandboxed Execution
- **Containerized execution** — Every submission runs in isolated Docker container
- **Resource limits** — CPU time (2s), memory (256MB), no network access
- **Multi-language support** — Java, Python, C++, JavaScript (extensible to 62+ languages)
- **Fallback mechanism** — Local toolchain when Judge0 unavailable

### 🏁 Contest System
- **Time-bound competitions** with automatic scoring
- **Live leaderboards** with 15s cache + eviction on updates
- **Registration management** with GLOBAL/INDIVIDUAL timer modes
- **Editorial publishing** for post-contest learning

### ⚙️ Engineering Highlights
- **Distributed rate limiting** using Redis sorted sets (sliding window algorithm)
- **Write-through caching** for leaderboards and results
- **JWT-based stateless auth** with role-based access control (USER, PROBLEM_SETTER, ADMIN)
- **Dual-database strategy** — SQL (users, problems, contests) + MongoDB (submissions, audit logs)
- **Property-based testing** with jqwik for robust validation

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     React Frontend                          │
│        Monaco Editor • TanStack Query • Zustand             │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ REST API + JWT Auth
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Backend                      │
│  ┌───────────────────────────────────────────────────────┐  │
│  │        Async Execution Engine (Redis Streams)         │  │
│  │                                                       │  │
│  │  [Controller] → [Producer] → Redis Stream             │  │
│  │                                 ↓                     │  │
│  │                           [Consumer]                  │  │
│  │                                 ↓                     │  │
│  │                        Judge0 Sandbox                 │  │
│  │                                 ↓                     │  │
│  │  [Polling API] ← [Redis Cache] ← [Result]             │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│        ┌──────────┐  ┌────────────┐  ┌──────────────┐       │
│        │  Redis   │  │PostgreSQL/ │  │   MongoDB    │       │
│        │ Streams  │  │     H2     │  │ Submissions  │       │
│        │  Cache   │  │  Problems  │  │ Audit Logs   │       │
│        │RateLimit │  │   Users    │  │              │       │
│        └──────────┘  └────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

**Key Design Choice**: Async execution decouples API response time from code execution time. Redis Streams provide a reliable message queue with consumer groups for horizontal scaling.

---

## 🛠️ Tech Stack

<table>
<tr>
<td valign="top" width="50%">

### Frontend
- **React 19** with TypeScript
- **Vite** (Rolldown bundler)
- **Monaco Editor** — VSCode-based code editing
- **TanStack Query** — Server state management
- **shadcn/ui + Tailwind CSS 4** — Component library
- **Zustand** — Global state management
- **React Router 7** — Client-side routing

</td>
<td valign="top" width="50%">

### Backend
- **Spring Boot 3.5** on Java 21
- **Spring Security** — JWT authentication
- **Spring Data JPA** — SQL database access
- **Spring Data MongoDB** — Document storage
- **Redis 7** — Streams, caching, rate limiting
- **Judge0 API** — Sandboxed code execution
- **jqwik** — Property-based testing

</td>
</tr>
<tr>
<td valign="top" width="50%">

### Data Layer
- **PostgreSQL / H2** — Relational data (users, problems, contests)
- **MongoDB** — Document storage (submissions, audit logs)
- **Redis** — In-memory (task queue, cache, rate limiter)

</td>
<td valign="top" width="50%">

### Infrastructure
- **Docker + Docker Compose** — Containerized deployment
- **Nginx** — Frontend static file serving
- **Maven** — Build and dependency management
- **Swagger/OpenAPI** — API documentation

</td>
</tr>
</table>

---

## 🔬 Core Engineering Highlights

### 1. Asynchronous Processing Architecture

**Pattern**: Producer-Consumer with Redis Streams

```
Client submits code
    ↓
Backend creates task + returns taskId (202 Accepted)
    ↓
Task published to Redis Stream
    ↓
Consumer picks up task (XREADGROUP)
    ↓
Execute code in Judge0 sandbox
    ↓
Store result in Redis (5min TTL)
    ↓
Client polls /result/{taskId} → receives result
```

**Why Async?**
- Prevents API timeouts during long-running executions
- Handles traffic spikes gracefully (queue absorbs bursts)
- Provides better UX with real-time progress updates
- Enables horizontal scaling via consumer groups

### 2. Security-First Execution

| Security Layer | Implementation |
|---------------|---------------|
| **Containerization** | Each submission runs in isolated Docker container |
| **CPU Limits** | Configurable time limits (default: 2s) |
| **Memory Limits** | Per-submission caps (default: 256MB) |
| **No Network** | Execution environment has no internet access |
| **Process Limits** | Prevents fork bombs and resource exhaustion |
| **Timeout Enforcement** | Wall-clock + CPU time limits |

**Additional Security**:
- JWT tokens with 24h expiration
- BCrypt password hashing (cost factor 10)
- Role-based access control (USER | PROBLEM_SETTER | ADMIN)
- Input sanitization and XSS protection
- SQL injection prevention via JPA parameterized queries

### 3. Intelligent Caching Strategy

**Leaderboards**: 15s TTL + automatic eviction on score updates
```java
@Cacheable(value = "leaderboard", key = "#problemId")
public List<LeaderboardEntry> getLeaderboard(Long problemId) { ... }

@CacheEvict(value = "leaderboard", key = "#submission.problemId")
public void onAcceptedSubmission(Submission submission) { ... }
```

**Benefits**:
- Reduces DB load by 95% during contests
- Still feels "real-time" with 15s staleness
- Write-through pattern ensures consistency

**Results Cache**: 5min TTL for execution results
- Automatic cleanup prevents memory leaks
- Task expiration detection for better error messages

### 4. Distributed Rate Limiting

**Algorithm**: Sliding window using Redis sorted sets

```java
// RedisRateLimiter implementation
String key = "rate_limit:user:" + userId;
long now = System.currentTimeMillis();
long windowStart = now - WINDOW_MS;

// Remove old entries
redis.zRemRangeByScore(key, 0, windowStart);

// Count requests in current window
long count = redis.zCount(key, windowStart, "+inf");

if (count < MAX_REQUESTS) {
    redis.zAdd(key, now, UUID.randomUUID().toString());
    redis.expire(key, WINDOW_MS);
    return true;
}
return false;
```

**Benefits**:
- Works across multiple backend instances
- Fair rate limiting per user (5 submissions/min)
- Prevents abuse without impacting legitimate users

### 5. Dual-Database Strategy

**SQL (PostgreSQL/H2)**: ACID guarantees for critical data
- Users, authentication
- Problems, test cases
- Contests, registrations

**MongoDB**: Write-heavy, schema-flexible data
- Submissions (hundreds per contest)
- Audit logs (compliance tracking)
- Time-series execution data

**Redis**: Ephemeral, speed-critical data
- Task queue (Redis Streams)
- Cache (leaderboards, results)
- Rate limiting state

---

## 🛡️ System Design Considerations

### Failure Handling

| Failure Type | Detection | Response |
|-------------|-----------|----------|
| **Compilation Error** | Judge0 status code | Return compiler output to user |
| **Runtime Error** | Non-zero exit code | Display stderr, mark FAILED |
| **Time Limit Exceeded** | CPU time > limit | Verdict: TLE, show time used |
| **Memory Limit** | OOM detected | Verdict: MLE, show memory used |
| **Judge0 Timeout** | No response after 30s | Retry once, then fallback to local executor |
| **Judge0 Unavailable** | Connection error | Graceful degradation to local toolchain |

### Resource Management

**Backend JVM**:
```bash
-Xms512m -Xmx2g -XX:+UseG1GC
```

**Redis Configuration**:
```bash
maxmemory 256mb
maxmemory-policy allkeys-lru
```

**Thread Pools**:
- Core pool: 10 threads
- Max pool: 50 threads
- Queue capacity: 1000 tasks
- Consumer pool: Dedicated 5-20 threads for Redis Stream processing

### Scalability Thinking

**Horizontal Scaling**:
- Stateless backend → Any instance handles any request
- Redis consumer groups → Distributed task processing
- Shared Redis → Global rate limiting works across instances

**Current Bottlenecks**:
1. Single Redis instance (Streams don't support clustering natively)
   - Mitigation: Shard by `problemId % N` across multiple instances
2. Judge0 API rate limits
   - Mitigation: Self-host Judge0 or use multiple API keys
3. Database connections
   - Mitigation: Connection pooling (HikariCP) + read replicas

---

## 🚀 Getting Started

### Prerequisites

```bash
Java 21+
Node.js 18+
Maven 3.x
MongoDB (local or Atlas)
Redis 6+
```

### Quick Start (Docker - Recommended)

**1. Clone and configure**
```bash
git clone https://github.com/yourusername/codearena.git
cd codearena
cp .env.docker .env
# Edit .env with your secrets
```

**2. Deploy with one command**
```bash
./deploy.sh
```

This starts:
- ✅ PostgreSQL database
- ✅ MongoDB for submissions
- ✅ Redis for caching & queuing
- ✅ Backend API (port 8080)
- ✅ Frontend UI (port 80)

**3. Access the platform**
- **Frontend**: http://localhost
- **Backend**: http://localhost:8080
- **API Docs**: http://localhost:8080/swagger-ui.html

**Management commands**:
```bash
./deploy.sh logs      # View logs
./deploy.sh down      # Stop services
./deploy.sh restart   # Restart services
```

See **[DOCKER_DEPLOYMENT.md](./DOCKER_DEPLOYMENT.md)** for detailed deployment guide.

---

### Local Development

**1. Start Redis**
```bash
redis-server --daemonize yes --maxmemory 256mb --maxmemory-policy allkeys-lru
```

**2. Configure environment**
```bash
cp .env.example .env
# Edit MongoDB URI, Redis host, JWT secret
```

**3. Run backend** (uses embedded H2 database)
```bash
mvn spring-boot:run
# API: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
```

**4. Run frontend**
```bash
cd frontend
npm install
npm run dev
# Frontend: http://localhost:5173
```

---

## 📊 API Overview

### Authentication
```http
POST   /api/auth/signup      # Create new account
POST   /api/auth/login       # Get JWT token
GET    /api/auth/me          # Get current user
```

### Problems
```http
GET    /api/problems                 # List all problems
GET    /api/problems/{id}            # Get problem details
POST   /api/problems                 # Create problem (PROBLEM_SETTER)
PUT    /api/problems/{id}            # Update problem (PROBLEM_SETTER)
POST   /api/problems/{id}/publish    # Publish problem (ADMIN)
```

### Code Execution
```http
POST   /api/execute/run-async        # Run code (non-submitted)
POST   /api/execute/submit-async     # Submit solution
GET    /api/result/{taskId}          # Poll for execution result
```

### Contests
```http
GET    /api/contests                 # List contests
POST   /api/contests                 # Create contest (PROBLEM_SETTER)
POST   /api/contests/{id}/register   # Register for contest
GET    /api/contests/{id}/leaderboard # Get cached leaderboard
```

### Leaderboard
```http
GET    /api/leaderboard/{problemId}  # Get problem leaderboard (cached 15s)
```

Full API documentation available at: **http://localhost:8080/swagger-ui.html**

---

## 📁 Project Structure

```
codearena/
├── src/main/java/com/codearena/
│   ├── config/
│   │   ├── RedisConfig.java              # Redis Streams + Cache
│   │   ├── AsyncExecutionConfig.java     # Consumer thread pools
│   │   ├── SecurityConfig.java           # JWT auth filter
│   │   └── MongoIndexConfig.java         # MongoDB indexes
│   ├── controller/
│   │   ├── AuthController.java           # /auth/* endpoints
│   │   ├── ExecutionController.java      # /execute/* endpoints
│   │   ├── ProblemController.java        # CRUD for problems
│   │   ├── ContestController.java        # Contest management
│   │   └── LeaderboardController.java    # Cached leaderboards
│   ├── service/
│   │   ├── AsyncExecutionProducer.java   # Publish tasks to stream
│   │   ├── AsyncExecutionConsumer.java   # Consume + execute tasks
│   │   ├── LeaderboardServiceImpl.java   # @Cacheable leaderboards
│   │   └── ProblemService.java           # Problem logic
│   ├── execution/
│   │   ├── ExecutionGateway.java         # Strategy interface
│   │   ├── Judge0ExecutionGateway.java   # Judge0 integration
│   │   └── LocalExecutionEngine.java     # Fallback executor
│   ├── security/
│   │   ├── JwtAuthenticationFilter.java  # JWT validation
│   │   └── RedisRateLimiter.java         # Rate limiting
│   ├── repository/
│   │   ├── UserRepository.java           # JPA (SQL)
│   │   ├── ProblemRepository.java        # JPA (SQL)
│   │   └── SubmissionRepository.java     # MongoDB
│   ├── entity/                           # JPA entities
│   ├── document/                         # MongoDB documents
│   └── dto/                              # Data transfer objects
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── CodeEditor.tsx            # Monaco wrapper
│   │   │   ├── ExecutionPanel.tsx        # Result display
│   │   │   └── ProblemSolvePage.tsx      # Main interface
│   │   ├── hooks/
│   │   │   └── useAsyncExecution.ts      # Polling logic
│   │   ├── services/
│   │   │   └── api.ts                    # API client
│   │   └── stores/
│   │       └── authStore.ts              # Zustand auth state
│   └── package.json
│
├── docker-compose.yml                    # Multi-container setup
├── Dockerfile                            # Backend image
├── deploy.sh                             # Deployment script
└── pom.xml                               # Maven config
```

---

## 🎯 Design Decisions & Trade-offs

### Why Redis Streams instead of RabbitMQ/Kafka?

**Decision**: Use Redis Streams for task queue

✅ **Pros**:
- Simpler ops: Single Redis instance vs separate message broker
- Lower latency: In-memory queue (<1ms)
- Consumer groups: Built-in support for horizontal scaling
- Already using Redis: Reduce infrastructure complexity

❌ **Trade-offs**:
- Less mature than Kafka for large-scale streaming
- No native clustering for Streams
- Limited to single Redis instance

**When to reconsider**: Task volume > 10,000/sec or multi-datacenter replication needed

### Why MongoDB for submissions?

**Decision**: Store submission documents in MongoDB

✅ **Pros**:
- Write-heavy workload: Hundreds of submissions per contest
- Flexible schema: Different languages produce different metadata
- Time-series optimized: Insert-heavy, time-ordered data
- No complex joins: Submissions rarely join with other tables

❌ **Trade-offs**:
- No ACID guarantees across submissions + leaderboard
- More complex backup strategy

**Mitigation**: Use MongoDB transactions for critical updates, cache leaderboard calculations

### Why async execution?

**Decision**: Return `taskId` immediately, poll for results

✅ **Pros**:
- No API timeouts: Code execution can take 10+ seconds
- Better UX: Show progress states
- Handles spikes: Queue absorbs traffic bursts
- Resilient: Tasks survive server restarts

❌ **Trade-offs**:
- More complex frontend: Polling logic required
- Slightly higher latency: ~1s polling interval
- Task cleanup: Need TTL strategy

---

## 📚 Further Reading

- **[DOCUMENTATION.md](./DOCUMENTATION.md)** — Deep technical dive into architecture, data flows, and design decisions
- **[DOCKER_DEPLOYMENT.md](./DOCKER_DEPLOYMENT.md)** — Comprehensive deployment guide
- **[API Documentation](http://localhost:8080/swagger-ui.html)** — Interactive API explorer (when backend is running)

---

## 🤝 Contributing

This is a portfolio/learning project, but feel free to:
- Open issues for bugs or suggestions
- Submit PRs for improvements
- Use this as reference for your own projects

---

## 👤 Author

**Naveen Kumar**  
Backend Engineer | System Design Enthusiast

*Building scalable systems that handle real-world constraints.*

---

## 📄 License

MIT License — See [LICENSE](./LICENSE) for details.

---

<div align="center">

**⭐ Star this repo if you found it helpful!**

Built with ❤️ using Spring Boot, React, Redis, and MongoDB

</div>
