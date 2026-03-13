# Projeto SD — Search & Gateway Demo

Small distributed search system developed for a university course.  
The project is written in Java and consists of a backend with indexing components and a Spring Boot web interface.

## Overview

The system simulates a simple search engine architecture with multiple components:

- **Downloader** – collects web pages
- **Barrels** – store indexed data
- **Gateway** – handles communication between components
- **Frontend** – Spring Boot web interface for searching and monitoring

The frontend also includes a few integrations such as WebSockets for updates and optional external APIs (e.g. OpenAI summaries or Hacker News examples).

## Project structure

```
frontend/   Spring Boot web application
meta1/      Backend components (gateway, barrels, downloader)
```

Main entry points:

- `frontend/src/.../FrontendApplication` — starts the web interface
- `meta1/src/.../Gateway` — starts the backend gateway

## Technologies

- Java (OpenJDK 17 recommended)
- Maven
- Spring Boot
- WebSockets
- RMI for communication between components

## Build

Requirements:

- Java 11+
- Maven

Build the project:

```bash
mvn -pl frontend,meta1 -am clean package
```

## Running the project

### Start the frontend

```bash
cd frontend
mvn spring-boot:run
```

### Start the backend gateway

```bash
cd meta1
mvn exec:java -Dexec.mainClass="googol.gateway.Gateway"
```

You can also run the main classes directly from an IDE.

## Notes

This repository was created as part of a university assignment and is mainly intended as a demonstration of a simple distributed system with a web interface.