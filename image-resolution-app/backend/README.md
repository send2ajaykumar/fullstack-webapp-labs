A lightweight REST API for managing image metadata.

This backend is part of the fullstack-webapp-labs project and provides REST endpoints to create, retrieve, and manage image metadata. It is built using Spring Boot, Java 25, and Maven, with H2 as the in‑memory database.

🚀 Features
- REST APIs for image metadata
- Spring Boot 4.x with Java 25
- In‑memory H2 database (no setup required)
- JPA/Hibernate for persistence
- Lombok for cleaner code
- Simple, extensible project structure

🏗 Add dependencies:
- Spring Web
- Spring Data JPA
- H2 Database
- Lombok


🏗 Project Structure

backend/  
│  
├── src/  
│   ├── main/  
│   │   ├── java/com/ajay/imageresolution/  
│   │   │   ├── ImageResolutionApplication.java  
│   │   │   ├── controller/  
│   │   │   │     └── ImageResolutionController.java  
│   │   │   ├── model/  
│   │   │   │     └── Image.java  
│   │   │   └── service/  
│   │   │         ├── ImageService.java  
│   │   │         └── ImageServiceImpl.java  
│   │   └── resources/  
│   │         └── application.properties  
│   └── test/  
│         └── ImageResolutionApplicationTests.java  
│  
├── pom.xml  
├── mvnw / mvnw.cmd  
└── .gitignore  

| Component | Technology |
| --- | --- |
| Language | Java 25 |
| Framework | Spring Boot 4.0.4 |
| Build Tool | Maven |
| Database | H2 (in‑memory) |
| ORM | Spring Data JPA |
| Utilities | Lombok |

📚 Future Enhancements
- Add PUT/DELETE endpoints
- Add image upload support
- Add Swagger/OpenAPI documentation
- Add global exception handling
- Add DTOs + ModelMapper
