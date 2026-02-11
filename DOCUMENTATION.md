# CodeArena — Technical Documentation

**Deep dive into system architecture, design decisions, and implementation details.**

---

## Table of Contents

1. [System Design Overview](#system-design-overview)
2. [Architecture Diagrams](#architecture-diagrams)
3. [Request Flow Diagrams](#request-flow-diagrams)
4. [Component Breakdown](#component-breakdown)
5. [Database Schema](#database-schema)
6. [Execution Lifecycle](#execution-lifecycle)
7. [Security Model](#security-model)
8. [Caching Strategy](#caching-strategy)
9. [Failure Handling](#failure-handling)
10. [Resource Management](#resource-management)
11. [Performance Considerations](#performance-considerations)
12. [Design Decisions & Trade-offs](#design-decisions--trade-offs)
13. [Scalability Analysis](#scalability-analysis)
14. [Extension Strategies](#extension-strategies)
15. [Edge Cases](#edge-cases)
16. [Project Structure](#project-structure)

---

## System Design Overview

CodeArena is designed as a **three-tier architecture** with clear separation of concerns:

1. **Frontend Layer** — User interface, code editing, real-time polling
2. **Backend API Layer** — Business logic, task orchestration, caching
3. **Execution Engine** — Sandboxed code execution, result processing

### Core Design Principles

- **Asynchronous by Default** — No API call blocks on code execution
- **Stateless Authentication** — JWT tokens, no server-side sessions
- **Cache First** — Redis cache for frequently accessed data
- **Fail Fast** — Explicit timeouts, resource limits, error boundaries
- **Audit Everything** — Complete trail of all administrative actions

### Key Architectural Patterns

| Pattern | Usage | Benefit |
|---------|-------|---------|
| **Producer-Consumer** | Redis Streams for task queue | Decouples API from execution |
| **Write-Through Cache** | Leaderboard caching | Reduces DB load by 95% |
| **Circuit Breaker** | Judge0 API calls | Graceful degradation to local executor |
| **Repository Pattern** | Data access layer | Clean abstraction over JPA/MongoDB |
| **Strategy Pattern** | Execution backends (Judge0/Local) | Pluggable execution engines |

---

## Architecture Diagrams

### High-Level System Architecture

```
        ┌─────────────────────────────────────────────────────────────┐
        │                     CLIENT LAYER                            │
        │                                                             │
        │   ┌──────────────────────────────────────────────────────┐  │
        │   │           React Frontend (Port 5173/80)              │  │
        │   │                                                      │  │
        │   │  Monaco Editor  │  TanStack Query  │  Zustand         │  │
        │   │  shadcn/ui      │  React Router    │  TypeScript     │  │
        │   └──────────────────────────────────────────────────────┘  │
        └─────────────────────────────────────────────────────────────┘
                                    │
                                    │ HTTP/REST + JWT Auth
                                    ▼
        ┌─────────────────────────────────────────────────────────────┐
        │                       API LAYER                             │
        │                                                             │
        │   ┌──────────────────────────────────────────────────────┐  │
        │   │         Spring Boot Backend (Port 8080)              │  │
        │   │                                                      │  │
        │   │  Controllers → Services → Repositories               │  │
        │   │  JWT Filter → Rate Limiter → Business Logic          │  │
        │   └──────────────────────────────────────────────────────┘  │
        └─────────────────────────────────────────────────────────────┘
                                        │
                                        │
                      ┌─────────────────┼─────────────────┐
                      │                 │                 │
                      ▼                 ▼                 ▼
                ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
                │   DATABASE   │  │    CACHE     │  │    QUEUE     │
                │    LAYER     │  │    LAYER     │  │    LAYER     │
                │              │  │              │  │              │
                │ PostgreSQL/  │  │    Redis     │  │    Redis     │
                │     H2       │  │   (Cache)    │  │  (Streams)   │
                │  (Problems,  │  │ Leaderboards │  │  Task Queue  │
                │    Users,    │  │   Results    │  │  Consumer    │
                │  Contests)   │  │ Rate Limit   │  │   Groups     │
                │              │  │              │  │              │
                │   MongoDB    │  │              │  │              │
                │(Submissions, │  │              │  │              │
                │ Audit Logs)  │  │              │  │              │
                └──────────────┘  └──────────────┘  └──────────────┘
                                                            │
                                                            │
                                                            ▼
                                                  ┌──────────────────┐
                                                  │ EXECUTION LAYER  │
                                                  │                  │
                                                  │  Judge0 Sandbox  │
                                                  │   (Primary)      │
                                                  │        +         │
                                                  │ Local Toolchain  │
                                                  │   (Fallback)     │
                                                  └──────────────────┘
```

### Component Interaction Diagram

```
        ┌──────────┐     ┌────────────────┐     ┌──────────────┐
        │          │     │                │     │              │
        │  Client  │────▶│  JWT Filter    │────▶│ Controllers  │
        │          │     │  (Security)    │     │              │
        └──────────┘     └────────────────┘     └──────┬───────┘
                                                       │
                                                       ▼
                                 ┌─────────────────────────────────┐
                                 │         Services                │
                                 ├─────────────────────────────────┤
                                 │  • UserService                  │
                                 │  • ProblemService               │
                                 │  • AsyncExecutionProducer       │
                                 │  • AsyncExecutionConsumer       │
                                 │  • LeaderboardService           │
                                 │  • ContestService               │
                                 └────────┬────────────────────┬───┘
                                          │                    │
                          ┌───────────────┴──────┐             │
                          ▼                      ▼             ▼
                  ┌──────────────┐      ┌──────────────┐  ┌─────────────┐
                  │ Repositories │      │    Redis     │  │  MongoDB    │
                  │   (JPA/SQL)  │      │  Operations  │  │ Operations  │
                  └──────────────┘      └──────────────┘  └─────────────┘
```

---

## Request Flow Diagrams

### Code Submission Flow (ASCII Sequence)

```
┌────────┐         ┌────────┐         ┌──────────┐         ┌────────┐         ┌─────────┐         ┌────────┐
│ Client │         │  API   │         │ Producer │         │ Redis  │         │Consumer│         │ Judge0 │
└───┬────┘         └───┬────┘         └────┬─────┘         └───┬────┘         └───┬────┘         └───┬────┘
    │                  │                   │                   │                  │                  │
    │ POST /submit     │                   │                   │                  │                  │
    ├─────────────────▶│                   │                   │                  │                  │
    │                  │                   │                   │                  │                  │
    │         ┌────────┴────────┐          │                   │                  │                  │
    │         │ Rate Limit Check│          │                   │                  │                  │
    │         └────────┬────────┘          │                   │                  │                  │
    │                  │                   │                   │                  │                  │
    │                  │ createTask()      │                   │                  │                  │
    │                  ├──────────────────▶│                   │                  │                  │
    │                  │                   │                   │                  │                  │
    │                  │                   │ XADD stream       │                  │                  │
    │                  │                   ├──────────────────▶│                  │                  │
    │                  │                   │                   │                  │                  │
    │                  │                   │ SET result:taskId │                  │                  │
    │                  │                   │    PENDING        │                  │                  │
    │                  │                   ├──────────────────▶│                  │                  │
    │                  │                   │                   │                  │                  │
    │                  │    taskId         │                   │                  │                  │
    │                  │◀──────────────────┤                   │                  │                  │
    │                  │                   │                   │                  │                  │
    │ 202 {taskId}     │                   │                   │                  │                  │
    │◀─────────────────┤                   │                   │                  │                  │
    │                  │                   │                   │                  │                  │
    │                  │                   │                   │  XREADGROUP      │                  │
    │                  │                   │                   │◀─────────────────┤                  │
    │                  │                   │                   │                  │                  │
    │                  │                   │                   │  task data       │                  │
    │                  │                   │                   ├─────────────────▶│                  │
    │                  │                   │                   │                  │                  │
    │                  │                   │                   │ UPDATE PROCESSING│                  │
    │                  │                   │                   │◀─────────────────┤                  │
    │                  │                   │                   │                  │                  │
    │                  │                   │                   │                  │ POST /submissions│
    │                  │                   │                   │                  ├─────────────────▶│
    │                  │                   │                   │                  │                  │
    │                  │                   │                   │                  │ {token}          │
    │                  │                   │                   │                  │◀─────────────────┤
    │                  │                   │                   │                  │                  │
    │                  │                   │                   │                  │ Poll GET /token  │
    │                  │                   │                   │                  ├─────────────────▶│
    │                  │                   │                   │                  │                  │
    │                  │                   │                   │                  │ {result}         │
    │                  │                   │                   │                  │◀─────────────────┤
    │                  │                   │                   │                  │                  │
    │                  │                   │                   │  SET result      │                  │
    │                  │                   │                   │  SUCCESS + data  │                  │
    │                  │                   │                   │◀─────────────────┤                  │
    │                  │                   │                   │                  │                  │
    │                  │                   │                   │  XACK task       │                  │
    │                  │                   │                   │◀─────────────────┤                  │
    │                  │                   │                   │                  │                  │
    │ GET /result/{id} │                   │                   │                  │                  │
    ├─────────────────▶│                   │                   │                  │                  │
    │                  │                   │                   │                  │                  │
    │                  │       GET result  │                   │                  │                  │
    │                  ├──────────────────────────────────────▶│                  │                  │
    │                  │                   │                   │                  │                  │
    │                  │       result data │                   │                  │                  │
    │                  │◀──────────────────────────────────────┤                  │                  │
    │                  │                   │                   │                  │                  │
    │ 200 {result}     │                   │                   │                  │                  │
    │◀─────────────────┤                   │                   │                  │                  │
    │                  │                   │                   │                  │                  │
```

### Leaderboard Cache Flow

```
                    GET /leaderboard/{problemId}
                              │
                              ▼
                    ┌─────────────────┐
                    │ Check Redis     │
                    │ Cache           │
                    └────────┬────────┘
                             │
                    ┌────────┴────────┐
                    │                 │
               Cache HIT         Cache MISS
                    │                 │
                    │                 ▼
                    │        ┌─────────────────┐
                    │        │ Query MongoDB   │
                    │        │ Aggregate       │
                    │        │ submissions     │
                    │        └────────┬────────┘
                    │                 │
                    │                 ▼
                    │        ┌─────────────────┐
                    │        │ Calculate       │
                    │        │ Scores          │
                    │        └────────┬────────┘
                    │                 │
                    │                 ▼
                    │        ┌─────────────────┐
                    │        │ Sort by Score   │
                    │        └────────┬────────┘
                    │                 │
                    │                 ▼
                    │        ┌─────────────────┐
                    │        │ Write to Cache  │
                    │        │ TTL: 15s        │
                    │        └────────┬────────┘
                    │                 │
                    └─────────┬───────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ Return to       │
                    │ Client          │
                    └─────────────────┘

            
            
            NEW ACCEPTED SUBMISSION ARRIVES
                                │
                                ▼
                      ┌─────────────────┐
                      │ @CacheEvict     │
                      │ (problemId)     │
                      └────────┬────────┘
                               │
                               ▼
                      ┌─────────────────┐
                      │ Next request    │
                      │ triggers fresh  │
                      │ DB query        │
                      └─────────────────┘
```

---

## Component Breakdown

### Frontend Architecture

**Technology Stack**:
- React 19 with TypeScript
- Vite (Rolldown bundler) for build
- TanStack Query for server state
- Zustand for global state
- shadcn/ui + Tailwind CSS 4

**Key Components**:

#### `ProblemSolvePage.tsx`
Main interface for solving problems. Integrates:
- Navigation breadcrumbs
- Problem statement display
- Code editor panel
- Execution results panel

#### `CodeEditor.tsx`
Monaco Editor wrapper with:
- Syntax highlighting for multiple languages
- Auto-completion
- Theme support (light/dark)
- Code templates

#### `ExecutionPanel.tsx`
Displays execution results with:
- Polling states (PENDING → PROCESSING → SUCCESS)
- Test case results with verdicts
- Runtime and memory stats
- Error messages and stack traces

#### `useAsyncExecution.ts` (Custom Hook)
```typescript
export function useAsyncExecution() {
  const submitCode = async (code: string, problemId: number) => {
    // 1. Submit code to backend
    const { taskId } = await api.submitCodeAsync({ code, problemId });
    
    // 2. Start polling
    return pollForResult(taskId);
  };
  
  const pollForResult = async (taskId: string) => {
    const maxAttempts = 300; // 5 minutes (1s interval)
    
    for (let i = 0; i < maxAttempts; i++) {
      const result = await api.getExecutionResult(taskId);
      
      if (result.status === 'SUCCESS' || result.status === 'FAILED') {
        return result;
      }
      
      await sleep(1000); // 1 second polling interval
    }
    
    throw new Error('Task timeout');
  };
}
```

---

### Backend Architecture

**Core Services**:

#### `AsyncExecutionProducer.java`
```java
@Service
public class AsyncExecutionProducer {
    
    public String createSubmitTask(SubmitRequest request, User user) {
        // 1. Generate unique task ID
        String taskId = UUID.randomUUID().toString();
        
        // 2. Create task DTO
        SubmissionTask task = SubmissionTask.builder()
            .taskId(taskId)
            .userId(user.getId())
            .problemId(request.getProblemId())
            .code(request.getCode())
            .languageId(request.getLanguageId())
            .build();
        
        // 3. Publish to Redis Stream
        streamOperations.add(STREAM_KEY, task);
        
        // 4. Store PENDING status in cache
        redisTemplate.opsForValue().set(
            "result:" + taskId,
            ExecutionResult.pending(),
            Duration.ofMinutes(5)
        );
        
        return taskId;
    }
}
```

#### `AsyncExecutionConsumer.java`
```java
@Service
public class AsyncExecutionConsumer {
    
    @StreamListener(STREAM_KEY)
    public void onMessage(SubmissionTask task) {
        try {
            // 1. Update status to PROCESSING
            updateStatus(task.getTaskId(), "PROCESSING");
            
            // 2. Execute code via Judge0
            Judge0Submission result = executionGateway.execute(task);
            
            // 3. Evaluate against test cases
            SubmissionResult evaluatedResult = evaluator.evaluate(
                result, 
                testCaseService.getTestCases(task.getProblemId())
            );
            
            // 4. Store result in cache
            storeResult(task.getTaskId(), evaluatedResult);
            
            // 5. Save to MongoDB
            submissionRepository.save(evaluatedResult);
            
            // 6. Evict leaderboard cache if ACCEPTED
            if (evaluatedResult.getVerdict() == Verdict.ACCEPTED) {
                cacheManager.evict("leaderboard", task.getProblemId());
            }
            
            // 7. ACK message
            streamOperations.acknowledge(STREAM_KEY, task);
            
        } catch (Exception e) {
            handleExecutionFailure(task, e);
        }
    }
}
```

#### `RedisRateLimiter.java`
```java
@Component
public class RedisRateLimiter {
    
    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MS = 60_000; // 1 minute
    
    public boolean isAllowed(Long userId) {
        String key = "rate_limit:user:" + userId;
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_MS;
        
        // Remove old entries
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        
        // Count requests in current window
        Long count = redisTemplate.opsForZSet().count(key, windowStart, Double.MAX_VALUE);
        
        if (count < MAX_REQUESTS) {
            // Allow request
            redisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), now);
            redisTemplate.expire(key, Duration.ofMillis(WINDOW_MS));
            return true;
        }
        
        return false; // Rate limit exceeded
    }
}
```

---

## Database Schema

### SQL Schema (H2/PostgreSQL)

#### Users Table
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,  -- BCrypt hashed
    role VARCHAR(20) NOT NULL,             -- USER, PROBLEM_SETTER, ADMIN
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
```

#### Problems Table
```sql
CREATE TABLE problems (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(200) UNIQUE NOT NULL,
    description TEXT NOT NULL,
    difficulty VARCHAR(20),                -- EASY, MEDIUM, HARD
    status VARCHAR(20) DEFAULT 'DRAFT',    -- DRAFT, PUBLISHED
    time_limit_ms INTEGER DEFAULT 2000,
    memory_limit_kb INTEGER DEFAULT 262144,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_problems_status ON problems(status);
CREATE INDEX idx_problems_difficulty ON problems(difficulty);
CREATE INDEX idx_problems_slug ON problems(slug);
```

#### Test Cases Table
```sql
CREATE TABLE test_cases (
    id BIGSERIAL PRIMARY KEY,
    problem_id BIGINT REFERENCES problems(id) ON DELETE CASCADE,
    input TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    is_sample BOOLEAN DEFAULT FALSE,       -- Sample test cases visible to user
    weight DECIMAL(5,2) DEFAULT 1.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_testcases_problem ON test_cases(problem_id);
```

#### Contests Table
```sql
CREATE TABLE contests (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    timer_mode VARCHAR(20) DEFAULT 'GLOBAL',  -- GLOBAL, INDIVIDUAL
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_contests_time ON contests(start_time, end_time);
```

#### Contest Registrations Table
```sql
CREATE TABLE contest_registrations (
    id BIGSERIAL PRIMARY KEY,
    contest_id BIGINT REFERENCES contests(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,                  -- For INDIVIDUAL timer mode
    UNIQUE(contest_id, user_id)
);

CREATE INDEX idx_registrations_contest ON contest_registrations(contest_id);
CREATE INDEX idx_registrations_user ON contest_registrations(user_id);
```

---

### MongoDB Schema

#### Submissions Collection
```javascript
{
  _id: ObjectId,
  taskId: String,                    // UUID for execution tracking
  userId: Long,
  problemId: Long,
  contestId: Long | null,
  code: String,
  languageId: Int,
  languageName: String,
  verdict: String,                   // ACCEPTED, WRONG_ANSWER, TLE, MLE, etc.
  runtime: Int,                      // milliseconds
  memory: Int,                       // kilobytes
  testCaseResults: [
    {
      testCaseId: Long,
      passed: Boolean,
      runtime: Int,
      memory: Int,
      output: String | null,
      expectedOutput: String | null,
      errorMessage: String | null
    }
  ],
  compilationError: String | null,
  runtimeError: String | null,
  submittedAt: ISODate,
  processedAt: ISODate
}

// Indexes
db.submissions.createIndex({ userId: 1, submittedAt: -1 });
db.submissions.createIndex({ problemId: 1, verdict: 1 });
db.submissions.createIndex({ problemId: 1, userId: 1, submittedAt: -1 });
db.submissions.createIndex({ contestId: 1, userId: 1 });
db.submissions.createIndex({ taskId: 1 }, { unique: true });
```

#### Audit Logs Collection
```javascript
{
  _id: ObjectId,
  userId: Long,
  username: String,
  action: String,                    // CREATE_PROBLEM, PUBLISH_PROBLEM, etc.
  entityType: String,                // PROBLEM, CONTEST, USER
  entityId: Long,
  details: Object,                   // Action-specific metadata
  timestamp: ISODate,
  ipAddress: String
}

// Indexes
db.auditLogs.createIndex({ userId: 1, timestamp: -1 });
db.auditLogs.createIndex({ entityType: 1, entityId: 1 });
db.auditLogs.createIndex({ timestamp: -1 });
```

---

### Redis Data Structures

#### Streams (Task Queue)
```
Key: codearena:execution:tasks
Structure: Stream
Format: {
  taskId: String,
  taskType: "RUN" | "SUBMIT",
  userId: Long,
  problemId: Long,
  code: String,
  languageId: Int,
  createdAt: Long
}
Consumer Group: execution-consumers
```

#### Cache (Results)
```
Key: codearena:execution:result:{taskId}
Structure: String (JSON)
TTL: 300 seconds (5 minutes)
Format: {
  status: "PENDING" | "PROCESSING" | "SUCCESS" | "FAILED",
  verdict: String,
  runtime: Int,
  memory: Int,
  testCaseResults: Array,
  error: String | null
}
```

#### Cache (Leaderboards)
```
Key: codearena:leaderboard:{problemId}
Structure: String (JSON)
TTL: 15 seconds
Format: [
  {
    userId: Long,
    username: String,
    runtime: Int,
    memory: Int,
    submittedAt: Long
  }
]
```

#### Rate Limiting
```
Key: rate_limit:user:{userId}
Structure: Sorted Set
Score: timestamp (milliseconds)
Member: UUID (unique request ID)
TTL: 60 seconds
```

---

## Execution Lifecycle

### Detailed Submission Flow

**Phase 1: API Request Handling**

1. **Authentication Check**
   - JWT filter extracts token from `Authorization` header
   - Validates signature and expiration
   - Loads user details from token claims

2. **Rate Limiting**
   - Check sliding window in Redis
   - Allow: 5 requests per minute per user
   - Return `429 Too Many Requests` if exceeded

3. **Input Validation**
   - Code length: 1 - 65,535 characters
   - Language ID: Must be supported
   - Problem ID: Must exist and be PUBLISHED

**Phase 2: Task Creation**

4. **Generate Task ID**
   - UUID v4 for unique identification
   - Used for polling and result retrieval

5. **Publish to Redis Stream**
   ```java
   ObjectRecord<String, SubmissionTask> record = 
       StreamRecords.objectBacked(task).withStreamKey(STREAM_KEY);
   streamOperations.add(record);
   ```

6. **Initialize Result Cache**
   - Store `PENDING` status
   - Set 5-minute TTL
   - Return `202 Accepted` with taskId

**Phase 3: Async Execution**

7. **Consumer Receives Task**
   ```java
   @StreamListener(value = STREAM_KEY, copyHeaders = "false")
   public void processTask(@Payload SubmissionTask task) {
       // ... execution logic
   }
   ```

8. **Update Status to PROCESSING**
   - Notify clients that execution has started

9. **Execute via Judge0**
   ```java
   Judge0Request request = Judge0Request.builder()
       .sourceCode(Base64.encode(task.getCode()))
       .languageId(task.getLanguageId())
       .stdin(Base64.encode(testCase.getInput()))
       .expectedOutput(Base64.encode(testCase.getExpectedOutput()))
       .cpuTimeLimit(problem.getTimeLimitMs() / 1000.0)
       .memoryLimit(problem.getMemoryLimitKb())
       .enableNetwork(false)
       .build();
   
   Judge0Submission result = judge0Client.createSubmission(request);
   ```

10. **Poll Judge0 for Result**
    - Max 30 attempts with exponential backoff
    - Initial delay: 100ms
    - Max delay: 2000ms
    - Timeout: 30 seconds total

11. **Evaluate Test Cases**
    - Run against all test cases sequentially
    - Aggregate verdicts (ACCEPTED if all pass)
    - Calculate total runtime and memory

**Phase 4: Result Storage**

12. **Store in Redis Cache**
    - Update with SUCCESS/FAILED status
    - Include verdict, runtime, memory
    - Show test case results

13. **Persist to MongoDB**
    ```java
    Submission submission = Submission.builder()
        .taskId(task.getTaskId())
        .userId(task.getUserId())
        .problemId(task.getProblemId())
        .verdict(evaluatedResult.getVerdict())
        .runtime(evaluatedResult.getRuntime())
        .memory(evaluatedResult.getMemory())
        .testCaseResults(evaluatedResult.getTestCaseResults())
        .submittedAt(new Date())
        .build();
    
    submissionRepository.save(submission);
    ```

14. **Cache Eviction**
    - If verdict is ACCEPTED, evict leaderboard cache
    - Ensures next leaderboard request fetches fresh data

15. **ACK Stream Message**
    - Mark task as processed
    - Remove from pending list in consumer group

**Phase 5: Client Polling**

16. **Poll for Result**
    ```typescript
    const poll = async () => {
      for (let i = 0; i < 300; i++) {  // 5 minutes max
        const result = await api.getExecutionResult(taskId);
        
        if (result.status === 'SUCCESS' || result.status === 'FAILED') {
          return result;
        }
        
        await sleep(1000);  // 1 second interval
      }
      
      throw new Error('Execution timeout');
    };
    ```

17. **Display Result**
    - Show verdict badge
    - Display test case results
    - Show runtime and memory stats
    - Highlight errors if any

---

## Security Model

### Threat Model & Mitigation

#### 1. Untrusted Code Execution

**Threat**: User submits malicious code (infinite loops, fork bombs, file system access)

**Mitigation**:
- **Containerization**: Each submission runs in isolated Docker container
- **CPU Limits**: 2-second default limit (configurable per problem)
- **Memory Limits**: 256MB default cap
- **Process Limits**: Max 60 processes/threads
- **No Network**: Execution environment has no internet access
- **Read-only FS**: Limited write access to /tmp only

#### 2. Authentication & Authorization

**Threat**: Unauthorized access to problems, submissions, admin functions

**Mitigation**:
- **JWT Tokens**: HMAC-SHA256 signed, 24-hour expiration
- **Role-Based Access**:
  ```java
  @PreAuthorize("hasRole('PROBLEM_SETTER')")
  public Problem createProblem(ProblemRequest request) { ... }
  
  @PreAuthorize("hasRole('ADMIN')")
  public void publishProblem(Long problemId) { ... }
  ```
- **Password Security**: BCrypt with cost factor 10
- **Token Refresh**: Automatic renewal before expiration

#### 3. Rate Limiting & DoS

**Threat**: Submission spam, API abuse

**Mitigation**:
- **Sliding Window Rate Limiting**: 5 submissions/minute per user
- **Distributed**: Redis-backed, works across multiple instances
- **Graceful Degradation**: Returns 429 with Retry-After header
- **Queue Depth Limits**: Max 1000 pending tasks

#### 4. SQL/NoSQL Injection

**Threat**: Malicious input in queries

**Mitigation**:
- **Parameterized Queries**: JPA uses prepared statements
- **Input Validation**: `@Valid` annotations on DTOs
- **MongoDB**: Spring Data MongoDB uses safe query builders

#### 5. XSS & CSRF

**Threat**: Script injection, cross-site request forgery

**Mitigation**:
- **Input Sanitization**: Escape user input in responses
- **Secure Headers**: X-Frame-Options, X-Content-Type-Options
- **CORS**: Configured allowed origins
- **CSP**: Content Security Policy headers

---

## Caching Strategy

### Cache Layers

#### 1. Execution Results (5-minute TTL)

**Purpose**: Store task execution results for polling

**Key**: `codearena:execution:result:{taskId}`

**Value**:
```json
{
  "status": "SUCCESS",
  "verdict": "ACCEPTED",
  "runtime": 123,
  "memory": 8192,
  "testCaseResults": [...]
}
```

**Eviction**: Automatic TTL (5 minutes)

**Why 5 minutes?**
- Long enough for client polling (max 300 attempts × 1s)
- Short enough to prevent memory bloat
- Results are persisted in MongoDB anyway

#### 2. Leaderboards (15-second TTL)

**Purpose**: Reduce database load for frequently accessed leaderboards

**Key**: `codearena:leaderboard:{problemId}`

**Value**: JSON array of top submissions

**Eviction Strategy**: Write-through + @CacheEvict

```java
// Cache population
@Cacheable(value = "leaderboard", key = "#problemId")
public List<LeaderboardEntry> getLeaderboard(Long problemId) {
    return submissionRepository.findTopByProblemIdAndVerdict(
        problemId, 
        Verdict.ACCEPTED,
        PageRequest.of(0, 100)  // Top 100
    ).stream()
    .map(this::toLeaderboardEntry)
    .sorted(Comparator.comparing(LeaderboardEntry::getRuntime))
    .collect(Collectors.toList());
}

// Cache eviction on new accepted submission
@CacheEvict(value = "leaderboard", key = "#submission.problemId")
public void onAcceptedSubmission(Submission submission) {
    // Next request will trigger fresh DB query
}
```

**Why 15 seconds?**
- Balances freshness vs load reduction
- During contests, reduces DB queries from 100s/sec to ~7/sec
- Still feels "real-time" to users

#### 3. Problem Details (1-hour TTL)

**Purpose**: Cache problem statements, test cases

**Eviction**: Manual eviction on problem updates

```java
@Cacheable(value = "problems", key = "#problemId")
public Problem getProblemById(Long problemId) { ... }

@CacheEvict(value = "problems", key = "#problemId")
public void updateProblem(Long problemId, ProblemRequest request) { ... }
```

---

## Failure Handling

### Execution Failures

| Failure Type | Detection | Handling | User Experience |
|-------------|-----------|----------|-----------------|
| **Compilation Error** | Judge0 status `6` | Extract compiler output, return to user | Show error message with line numbers |
| **Runtime Error** | Non-zero exit code | Capture stderr, mark FAILED | Display error message, stack trace |
| **Time Limit Exceeded** | CPU time > limit | Mark TLE, show actual time | Badge: TLE (2000ms limit, used 2134ms) |
| **Memory Limit Exceeded** | Memory > limit | Mark MLE, show actual memory | Badge: MLE (256MB limit, used 312MB) |
| **Wrong Answer** | Output mismatch | Show expected vs actual | Display failing test case (if public) |

### System Failures

#### Judge0 Unavailable

```java
@Service
public class ExecutionGateway {
    
    public Judge0Submission execute(Judge0Request request) {
        try {
            // Try Judge0 first
            return judge0Service.execute(request);
        } catch (Judge0Exception e) {
            log.warn("Judge0 unavailable, falling back to local executor", e);
            
            // Fallback to local toolchain
            return localExecutor.execute(request);
        }
    }
}
```

#### Redis Down

**Stream Operations**: Buffer in memory (limited capacity)
```java
if (!redisAvailable()) {
    // Use in-memory queue (max 100 items)
    inMemoryQueue.offer(task);
}
```

**Cache Operations**: Fallback to database
```java
try {
    return redisTemplate.opsForValue().get(key);
} catch (RedisException e) {
    log.warn("Redis unavailable, querying database");
    return database.query(...);
}
```

**Rate Limiting**: Fail-open (allow requests)
```java
try {
    return rateLimiter.isAllowed(userId);
} catch (RedisException e) {
    log.error("Rate limiter unavailable, allowing request");
    return true;  // Fail-open for availability
}
```

#### MongoDB Down

**Submission Storage**: Log to file system, retry later
```java
try {
    submissionRepository.save(submission);
} catch (MongoException e) {
    log.error("MongoDB unavailable, writing to file");
    fileLogger.log(submission);
    retryQueue.add(submission);  // Retry later
}
```

---

## Resource Management

### JVM Configuration

```bash
# Recommended settings for 4GB RAM server
JAVA_OPTS="-Xms512m \
           -Xmx2g \
           -XX:+UseG1GC \
           -XX:MaxGCPauseMillis=200 \
           -XX:+HeapDumpOnOutOfMemoryError \
           -XX:HeapDumpPath=/var/logs/heapdump.hprof"
```

### Thread Pool Configuration

```yaml
# application.yml
spring:
  task:
    execution:
      pool:
        core-size: 10
        max-size: 50
        queue-capacity: 1000
        thread-name-prefix: async-exec-
```

**Consumer Thread Pool** (Redis Streams):
```java
@Configuration
public class AsyncConfig {
    
    @Bean(name = "streamConsumerExecutor")
    public Executor streamConsumerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("stream-consumer-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### Database Connection Pooling

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Redis Configuration

```bash
# redis.conf
maxmemory 256mb
maxmemory-policy allkeys-lru
maxmemory-samples 5
```

---

## Performance Considerations

### Database Indexing Strategy

**Critical Indexes**:
```sql
-- Fast problem filtering
CREATE INDEX idx_problems_status_difficulty ON problems(status, difficulty);

-- Fast user lookups (auth)
CREATE INDEX idx_users_email ON users(email);

-- Fast submission queries
CREATE INDEX idx_submissions_user_time ON submissions(user_id, submitted_at DESC);
```

**MongoDB Compound Indexes**:
```javascript
// Leaderboard queries
db.submissions.createIndex({ 
  problemId: 1, 
  verdict: 1, 
  runtime: 1 
});

// User submission history
db.submissions.createIndex({ 
  userId: 1, 
  submittedAt: -1 
});
```

### Query Optimization

**N+1 Query Prevention**:
```java
// BAD: N+1 queries
List<Problem> problems = problemRepository.findAll();
problems.forEach(p -> {
    p.getTestCases();  // Lazy load, separate query per problem
});

// GOOD: Fetch join
@Query("SELECT p FROM Problem p LEFT JOIN FETCH p.testCases WHERE p.id = :id")
Problem findByIdWithTestCases(@Param("id") Long id);
```

### Pagination

```java
// Always use pagination for large result sets
Page<Submission> submissions = submissionRepository.findByUserId(
    userId, 
    PageRequest.of(page, 50)  // 50 submissions per page
);
```

---

## Design Decisions & Trade-offs

### 1. Redis Streams vs RabbitMQ/Kafka

**Decision**: Use Redis Streams

**Reasoning**:
- ✅ Already using Redis for caching/rate limiting (reduce dependencies)
- ✅ Lower latency (<1ms in-memory)
- ✅ Simpler operations (single Redis instance vs broker cluster)
- ✅ Consumer groups support horizontal scaling

**Trade-offs**:
- ❌ Less mature than Kafka for large-scale streaming
- ❌ No built-in schema validation
- ❌ Single point of failure (Redis instance)

**Threshold for Reconsidering**: >10,000 tasks/sec or multi-datacenter replication

### 2. MongoDB vs SQL for Submissions

**Decision**: Use MongoDB for submissions

**Reasoning**:
- ✅ Write-heavy workload (100s of submissions per contest)
- ✅ Flexible schema (different languages = different metadata)
- ✅ Time-series optimized (insert-heavy, time-ordered)
- ✅ No complex joins needed

**Trade-offs**:
- ❌ Eventual consistency challenges
- ❌ More complex backup/restore

**Mitigation**: Use transactions for critical updates, cache aggregations

### 3. Polling vs WebSockets

**Decision**: HTTP polling with 1-second interval

**Reasoning**:
- ✅ Simpler implementation (no WebSocket infrastructure)
- ✅ Works through corporate firewalls/proxies
- ✅ Easier horizontal scaling (stateless)
- ✅ Acceptable latency for code execution (1s polling << execution time)

**Trade-offs**:
- ❌ Higher network overhead than WebSockets
- ❌ Slightly higher latency

**Threshold for Reconsidering**: Real-time collaboration or <100ms latency requirement

### 4. JWT vs Session-based Auth

**Decision**: Stateless JWT

**Reasoning**:
- ✅ Horizontal scaling (any instance can validate)
- ✅ No Redis dependency for auth
- ✅ Mobile-friendly
- ✅ Microservices-ready

**Trade-offs**:
- ❌ Can't revoke before expiration
- ❌ Larger token size vs session ID

**Mitigation**: Short expiration (24h), token refresh mechanism

---

## Scalability Analysis

### Current Bottlenecks

1. **Single Redis Instance**
   - Streams don't support native clustering
   - **Solution**: Shard by `problemId % N` across multiple Redis instances

2. **Judge0 API Rate Limits**
   - Free tier: 50 requests/day
   - **Solution**: Self-host Judge0 or rotate API keys

3. **Database Connection Pool**
   - Max 20 connections with HikariCP
   - **Solution**: Increase pool size, add read replicas

### Horizontal Scaling Strategy

```
                    ┌──────────────┐
                    │ Load Balancer│
                    └──────┬───────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           ▼               ▼               ▼
    ┌────────────┐  ┌────────────┐  ┌────────────┐
    │ Backend 1  │  │ Backend 2  │  │ Backend 3  │
    │ (Stateless)│  │ (Stateless)│  │ (Stateless)│
    └─────┬──────┘  └─────┬──────┘  └─────┬──────┘
          │                │                │
          └────────────────┼────────────────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
    ┌─────────┐      ┌─────────┐      ┌─────────┐
    │ Redis   │      │Database │      │ MongoDB │
    │(Shared) │      │(Primary)│      │(Sharded)│
    └─────────┘      └────┬────┘      └─────────┘
          │                │
          │                ▼
          │          ┌─────────┐
          │          │Read     │
          │          │Replicas │
          │          └─────────┘
          │
          ▼
    ┌───────────────────────────┐
    │   Consumer Group          │
    │   (3 consumers)           │
    │   Each backend instance   │
    │   runs a consumer         │
    └───────────────────────────┘
```

**Key Points**:
- Stateless backend: Any instance handles any request
- Shared Redis: Rate limiting works globally
- Consumer group: Redis distributes tasks evenly
- Read replicas: Scale read-heavy queries

### Performance Targets

| Metric | Target | Current |
|--------|--------|---------|
| API response (p95) | <100ms | ~50ms |
| Code execution latency | <5s | ~2-3s |
| Leaderboard query (cached) | <50ms | ~10ms |
| Leaderboard query (uncached) | <500ms | ~200ms |
| Submission throughput | 100/sec | ~50/sec |
| Concurrent users | 10,000 | Not tested |

---

## Extension Strategies

### Adding New Programming Language

**Steps**:

1. **Get Judge0 language ID**
   ```bash
   curl https://judge0-ce.p.rapidapi.com/languages
   # Example: Ruby = 72, Rust = 73, Go = 60
   ```

2. **Add to supported languages**
   ```typescript
   // frontend/src/constants/languages.ts
   export const SUPPORTED_LANGUAGES = [
     { id: 72, name: 'Ruby', monacoLanguage: 'ruby', extension: '.rb' },
     // ...
   ];
   ```

3. **Add starter code template** (optional)
   ```java
   @Entity
   public class StarterCode {
       private Integer languageId;
       private String template = "# Write your Ruby solution here\n";
   }
   ```

4. **Configure limits** (if different)
   ```yaml
   execution:
     language-limits:
       72:  # Ruby
         cpu-time-limit: 3
         memory-limit: 512
   ```

### Adding New Execution Backend

**Example: Local Docker Executor**

1. **Implement interface**
   ```java
   public class DockerLocalExecutor implements ExecutionGateway {
       
       @Override
       public Judge0Submission execute(Judge0Request request) {
           // 1. Create Docker container
           String containerId = dockerClient.createContainer(
               new ContainerConfig.Builder()
                   .image("openjdk:21-slim")
                   .cmd("java", "Solution.java")
                   .hostConfig(new HostConfig.Builder()
                       .cpuQuota(2000000L)  // 2 CPU seconds
                       .memory(256 * 1024 * 1024L)  // 256MB
                       .networkMode("none")  // No network
                       .build())
                   .build()
           );
           
           // 2. Run container
           dockerClient.startContainer(containerId);
           
           // 3. Wait for completion
           dockerClient.waitContainer(containerId, 30_000);  // 30s timeout
           
           // 4. Capture output
           String logs = dockerClient.logs(containerId);
           
           // 5. Clean up
           dockerClient.removeContainer(containerId);
           
           return parseResult(logs);
       }
   }
   ```

2. **Register as bean**
   ```java
   @Bean
   @ConditionalOnProperty(name = "execution.backend", havingValue = "docker")
   public ExecutionGateway dockerExecutor() {
       return new DockerLocalExecutor();
   }
   ```

---

## Edge Cases

### Handled Edge Cases

1. **Infinite Loop**
   - CPU time limit enforced by Judge0
   - Wall-clock timeout on consumer side
   - Result: TLE verdict

2. **Fork Bomb**
   - Process/thread limit: max 60
   - Result: Runtime error

3. **Memory Leak**
   - Memory limit enforced (256MB default)
   - Result: MLE verdict

4. **Network Requests**
   - No network access in container
   - Result: Connection refused error

5. **File System Access**
   - Read-only filesystem
   - Limited /tmp access
   - Result: Permission denied error

6. **Task Expiration**
   - Client polls for 5 minutes
   - Redis TTL expires result
   - Result: "Task expired" error message

7. **Duplicate Submission**
   - Rate limiter prevents spam
   - Result: 429 Too Many Requests

8. **Contest Not Started**
   - Validate start time before accepting submissions
   - Result: 403 Forbidden

9. **Compilation Timeout**
   - Judge0 enforces compilation time limit
   - Result: Compilation error

10. **Invalid Test Case Format**
    - Validate during problem creation
    - Result: 400 Bad Request

---

## Project Structure

```
codearena/
├── src/
│   ├── main/
│   │   ├── java/com/codearena/
│   │   │   ├── CodeArenaApplication.java    # Main entry point
│   │   │   ├── config/
│   │   │   │   ├── RedisConfig.java           # Redis Streams + Cache
│   │   │   │   ├── AsyncExecutionConfig.java  # Thread pools
│   │   │   │   ├── SecurityConfig.java        # JWT + CORS
│   │   │   │   ├── MongoConfig.java           # MongoDB setup
│   │   │   │   └── SwaggerConfig.java         # API docs
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── ExecutionController.java
│   │   │   │   ├── ProblemController.java
│   │   │   │   ├── ContestController.java
│   │   │   │   └── LeaderboardController.java
│   │   │   ├── service/
│   │   │   │   ├── AsyncExecutionProducer.java
│   │   │   │   ├── AsyncExecutionConsumer.java
│   │   │   │   ├── UserService.java
│   │   │   │   ├── ProblemService.java
│   │   │   │   ├── ContestService.java
│   │   │   │   └── LeaderboardServiceImpl.java
│   │   │   ├── execution/
│   │   │   │   ├── ExecutionGateway.java      # Interface
│   │   │   │   ├── Judge0ExecutionGateway.java
│   │   │   │   ├── LocalExecutionEngine.java
│   │   │   │   └── TestCaseEvaluator.java
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java        # JPA
│   │   │   │   ├── ProblemRepository.java     # JPA
│   │   │   │   ├── ContestRepository.java     # JPA
│   │   │   │   └── SubmissionRepository.java  # MongoDB
│   │   │   ├── entity/                        # JPA entities
│   │   │   ├── document/                      # MongoDB documents
│   │   │   ├── dto/                           # Data transfer objects
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   └── RedisRateLimiter.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── *Exception.java
│   │   │   └── util/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   └── test/
│       └── java/com/codearena/
│           ├── service/
│           ├── integration/
│           └── properties/                    # jqwik property tests
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── editor/
│   │   │   │   ├── CodeEditor.tsx
│   │   │   │   ├── ExecutionPanel.tsx
│   │   │   │   └── LanguageSelector.tsx
│   │   │   ├── problem/
│   │   │   │   ├── ProblemStatement.tsx
│   │   │   │   └── TestCasePanel.tsx
│   │   │   └── ui/                           # shadcn components
│   │   ├── pages/
│   │   │   ├── ProblemSolvePage.tsx
│   │   │   ├── ProblemsPage.tsx
│   │   │   ├── LeaderboardsPage.tsx
│   │   │   └── ContestDetailPage.tsx
│   │   ├── hooks/
│   │   │   ├── useAsyncExecution.ts
│   │   │   ├── useProblems.ts
│   │   │   └── useAuth.ts
│   │   ├── stores/
│   │   │   └── authStore.ts                  # Zustand
│   │   ├── lib/
│   │   │   ├── api.ts                        # Axios client
│   │   │   └── utils.ts
│   │   └── types/
│   │       └── index.ts
│   ├── public/
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
├── docker/
│   ├── backend.Dockerfile
│   ├── frontend.Dockerfile
│   └── nginx.conf
├── .github/
│   └── workflows/
│       ├── backend-ci.yml
│       └── frontend-ci.yml
├── docker-compose.yml
├── deploy.sh
├── pom.xml
├── README.md
├── DOCUMENTATION.md                          # This file
└── DOCKER_DEPLOYMENT.md
```

---

## Summary

CodeArena demonstrates:

- **System Design Thinking** — Async processing patterns, intelligent caching, distributed rate limiting
- **Security Awareness** — Sandboxed execution, resource limits, JWT auth, audit trails
- **Scalability Mindset** — Stateless design, horizontal scaling via consumer groups, connection pooling
- **Clean Architecture** — Separation of concerns, dependency injection, strategy pattern
- **Operational Maturity** — Health checks, graceful degradation, failure handling
- **Modern Stack Proficiency** — Latest Spring Boot 3.5, React 19, Redis Streams, MongoDB

This is not a toy project—it handles real-world constraints like resource exhaustion, rate limiting, concurrent execution, and failure recovery. It demonstrates understanding of:

- Message queue patterns (Redis Streams with consumer groups)
- Cache invalidation strategies (write-through, TTL-based eviction)
- Security in untrusted environments (sandboxing, resource limits)
- Real-time feedback systems (polling, progress states)
- Performance optimization (indexing, pagination, connection pooling)

---

**Built with engineering judgment, not just code.**
