# 🛡️ Obsidian API Gateway

Obsidian is a reactive API Gateway built with Spring Boot and WebFlux for managing authentication, rate limiting, and routing in microservice architectures.

---

## Installation

Clone the repository and build the project:

```bash
git clone https://github.com/your-username/obsidian-gateway.git
cd obsidian-gateway
mvn clean install
```

---

## Configuration

Update your `application.yml` with database and Redis credentials:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/obsidian
    username: your_username
    password: your_password

  redis:
    host: localhost
    port: 6379
```

---

## Usage

Run the application:

```bash
mvn spring-boot:run
```

Example request:

```bash
curl -H "X-API-KEY: your_api_key" http://localhost:8080/proxy/service
```

---

## Features

- API Key Authentication via request headers  
- Redis-based rate limiting per user  
- Reactive request handling using Spring WebFlux  
- Dynamic routing to backend services  
- Request filtering and header sanitization  
- Metrics collection using Micrometer  

---

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

---

## License

MIT License
