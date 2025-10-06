# 🚀 GitHub Repository Scorer

This project implements a backend service for **scoring GitHub repositories** based on stars, forks, and recency of updates. Built with **Spring WebFlux** and reactive programming.

---

## 🛠 Features

* Fetch repositories from GitHub based on **language** and **creation date**.
* Repo scoring using weighted **stars, forks, and recency**.
* Reactive, **non-blocking API** using Spring WebFlux.
* In-memory caching with **Caffeine** for faster repeated queries.
* Fully **containerized via Docker**.

---

## ⚡ Getting Started

### Prerequisites

* **Java 17+**
* **Maven**
* **Docker** (optional)

### Run Locally

```bash
# Build the project
mvn clean package

# Run the application
java -jar target/reposcorer-0.0.1-SNAPSHOT.jar
```

### Run with Docker

```bash
# Build Docker image
docker build -t reposcorer .

# Run container with GitHub token
docker run -e GITHUB_TOKEN=<your_token> -p 8080:8080 reposcorer
```

Access the API at: `http://localhost:8080/api/v1/repos`

---
## Swagger UI

You can access the Swagger UI to explore the API endpoints at:

http://localhost:8080/webjars/swagger-ui/index.html

## 🌐 API Usage

**Endpoint:** `GET /api/v1/repos`

| Parameter  | Required | Description                                                  |
|------------|----------|--------------------------------------------------------------|
| `language` | ✅        | Programming language (e.g., Java, Python)                    |
| `since`    | ✅        | ISO date `YYYY-MM-DD` (repositories created after this date) |
| `q`        | ❌        | Extra GitHub search query (optional)                         |
| `limit`    | ❌        | Max number of repositories (default: 100)                    |

**Example Request:**

```http
GET http://localhost:8080/api/v1/repos?language=Java&since=2025-01-01&limit=5
```
```curl
$ curl -X GET "http://localhost:8080/api/v1/repos?language=Java&since=2024-01-01&q=topic:backend&limit=200"  -H "Accept: application/json"
```

## 📈 Repo Scoring

Repositories are scored based on three main factors:

- **Stars → Log-Scaled**  
  The number of stars a repository has is transformed using a logarithmic scale (`log1p`) to reduce the impact of extremely popular repositories and to better differentiate moderately popular ones.

- **Forks → Log-Scaled**  
  Similarly, the number of forks is also log-scaled, giving a balanced contribution to the overall score without letting outliers dominate.

- **Recency → Exponential Decay (Half-Life)**  
  Recency of updates is calculated using an exponential decay function with a configurable half-life. This ensures that recent activity is weighted more heavily, while older updates contribute less to the overall repository score.

**Final Score Formula (simplified):**

```text
score = wStars * log1p(stars) + wForks * log1p(forks) + wRecency * exp(-λ * days_since_update)
```
Where `λ = ln(2) / halfLifeDays`.<br>
Weights (`wStars`, `wForks`, `wRecency`) are normalized and configurable in `application.properties`.

---

## ✅ Implemented Features
### Key Features

- **Reactive, non-blocking endpoints with WebFlux**  
  The API is built using **Spring WebFlux**, enabling asynchronous, non-blocking request handling. This ensures high throughput and scalability, allowing the service to handle multiple concurrent requests efficiently without blocking threads.

- **Repository Score Calculation with Half-Life Recency**  
  Each repository is scored using a combination of **stars, forks, and recency of updates**. Recency is calculated using an **exponential decay (half-life) function**, giving more weight to recent activity. This approach provides a dynamic and fair measure of repository relevance over time, highlighting projects that are both actively maintained and widely used.

- **Logging and error handling in GitHub search client**  
  Robust logging is implemented for all API calls. Errors such as GitHub downtime, network issues, or malformed responses are handled gracefully, ensuring the service remains reliable in production environments.

- **Caching with Caffeine**  
  In-memory caching using **Caffeine** reduces redundant API calls and improves performance. Cached results expire after a configurable period, balancing data freshness with efficiency and helping to mitigate GitHub API rate limits.

- **Integration and unit tests covering main flows**  
  The project includes **comprehensive unit tests** for individual components and **integration tests** for end-to-end scenarios. This ensures that the scoring logic, API endpoints, caching, and client communication behave correctly and consistently.

---

## ⚠️ Potential Improvements

1. **Authentication & Rate Limits**  
   The application makes direct calls to the GitHub API, which is subject to strict rate limits (60 requests/hour unauthenticated, 5,000/hour authenticated).
  - Implement token caching or rotation to stay within GitHub limits.
  - Gracefully handle `403` rate-limit responses through retries, backoff, or alternate tokens.
  - Optionally migrate to the **GitHub GraphQL API** for more efficient data retrieval.

---

2. **Pagination & Large Queries**  
   The GitHub Search API caps results at **1,000 repositories per query**.
  - Introduce pagination using the `page` and `per_page` parameters to fetch multiple result sets.
  - For very large data sets, implement **streaming** or **asynchronous batching**.
  - Optionally cache or persist partial results to improve performance and reduce redundant calls.

---

3. **Scoring Algorithm Enhancements**  
   The current scoring mechanism primarily considers **stars** and **forks**.
  - Enrich the score by including repository health indicators such as:
    - Open/closed issues
    - Pull requests merged
    - Contributor count
    - Recent commit activity
  - Normalize and weight these metrics to provide a more balanced and insightful repository score.

---

4. **Security**
   Currently, this project does not include authentication, authorization, or other security features.  
   - Future improvements could include:
     - **JWT-based authentication** to secure API endpoints(sample project -https://github.com/NahushaG/rest-api-showcase/tree/main/jwt).
     - Adding **Spring Security** for authentication and role-based access control(sample project -https://github.com/NahushaG/rest-api-showcase/tree/main/security-basic).
     - Enabling **HTTPS** for secure communication.
     - Adding **rate limiting** or **API key validation**.
     - Implementing **input validation** and **XSS/CSRF protection**.

---

5. **Configuration & Secrets**  
   Configuration currently resides in plain `application.properties` or environment files.
  - Move sensitive credentials (like the GitHub token) to **environment variables** or **Docker secrets**.
  - Update `application.yml` to reference secure variables:
    ```yaml
    github:
      token: ${GITHUB_TOKEN}
    ```  
  - Leverage Spring Boot’s `@ConfigurationProperties` for cleaner property binding.

---

6. **Metrics & Observability**  
   The service currently lacks built-in observability.
  - Integrate **Micrometer** with **Prometheus** to expose metrics such as:
    - GitHub API latency and error rates
    - Cache performance
    - Repository scoring time
  - Use these metrics to monitor reliability and detect performance bottlenecks.

---

7. **Resilience**  
   The current design does not include resilience mechanisms for external API failures.
  - Add **retries**, **circuit breakers**, and **fallbacks** using **Resilience4j** or **Spring Cloud Circuit Breaker**.
  - Implement **graceful degradation** (e.g., returning cached or partial results if GitHub is unavailable).
  - Improve user experience by ensuring predictable responses even during API downtime.


---

## 📦 Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/reposcorer-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV GITHUB_TOKEN=""
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

---

## 👨‍💻 Author

Prepared as a backend project for **coding challenge interviews**, demonstrating:
* Spring WebFlux and reactive programming
* API design and scoring algorithms
* Caching, testing, and Docker deployment
