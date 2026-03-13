# Distributed Search Engine (Googol) --- University Project

A small **distributed search engine prototype** developed for a
distributed systems course.

The project demonstrates how a simplified search engine architecture can
be built using multiple services that cooperate to crawl, index, and
query web pages. The backend is implemented in **Java**, and the user
interface is provided through a **Spring Boot web application**.

------------------------------------------------------------------------

## System Overview

The system simulates the core workflow of a search engine:

1.  A **Downloader** retrieves a webpage from a provided URL.
2.  The page content is parsed and links contained in the page are
    extracted.
3.  Newly discovered URLs are recursively scheduled for crawling.
4.  Page content is sent to **Barrel nodes**, which build and maintain a
    searchable index.
5.  A **Gateway** coordinates communication between distributed
    components.
6.  The **Frontend** allows users to perform search queries and monitor
    system activity.

This architecture demonstrates how crawling, indexing, and querying can
be separated into independent distributed services.

------------------------------------------------------------------------

## Distributed Architecture

The system is composed of several independent services:

-   **Downloader**
    -   Fetches webpages and extracts links for recursive crawling
-   **Barrels**
    -   Maintain indexed data and respond to search queries
-   **Gateway**
    -   Coordinates communication between services and acts as the main
        backend entry point
-   **Frontend**
    -   Spring Boot web interface used to submit search queries and
        monitor the system

Components communicate using **Java RMI**, enabling the system to run
across multiple processes or machines.

------------------------------------------------------------------------

## Docker / Multi‑Service Deployment

During development, **Docker** was used to simulate a distributed
environment where different services could run independently.

Typical containers included:

-   gateway
-   downloader
-   barrel nodes
-   frontend

This setup allowed testing of distributed communication and service
orchestration locally.

------------------------------------------------------------------------

## Project Structure

    frontend/   Spring Boot web application
    meta1/      Backend services (gateway, barrels, downloader)

Main entry points:

-   `frontend/src/.../FrontendApplication` --- starts the web interface
-   `meta1/src/.../Gateway` --- starts the backend gateway

------------------------------------------------------------------------

## Technologies

-   Java (OpenJDK 17 recommended)
-   Maven
-   Spring Boot
-   Java RMI
-   WebSockets
-   Docker (for distributed testing)

------------------------------------------------------------------------

## Build

### Requirements

-   Java 11+
-   Maven

### Build the project

``` bash
mvn -pl frontend,meta1 -am clean package
```

------------------------------------------------------------------------

## Running the Project

### Start the frontend

``` bash
cd frontend
mvn spring-boot:run
```

### Start the backend gateway

``` bash
cd meta1
mvn exec:java -Dexec.mainClass="googol.gateway.Gateway"
```

You can also run the main classes directly from your IDE.

------------------------------------------------------------------------

## Notes

This repository was created as part of a **university distributed
systems assignment** and is intended as a demonstration of:

-   distributed service architecture
-   recursive web crawling
-   indexing and search workflows
-   inter‑service communication using RMI
-   containerized multi‑service testing with Docker

External API integrations included in the original coursework
(e.g. OpenAI examples) were used only for demonstration purposes.
