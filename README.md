# IRI Facility Status API Reference Implementation

The **IRI Facility Status API** is a reference implementation for the **Integrated Research Infrastructure Facility Status** specification. It provides endpoints to query facility statuses, incidents, events, and related resources to support facility monitoring and status reporting.

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Endpoints](#endpoints)
- [Usage](#usage)
- [Development](#development)
- [License](#license)

---

## Overview
The **IRI Facility Status API** enables users to query facility-related resources, including:
- Facility details
- Status updates
- Incidents and events
- Sites and Geographical locations

This implementation is built with **Spring Boot** and follows RESTful API design principles.

---

## Features
✅ RESTful API endpoints for querying facility statuses, resources, incidents, events, and sites/locations  
✅ Built with **Spring Boot** for scalable and modular development  
✅ Uses **ConcurrentMap** for in-memory storage of data  
✅ Implements proper exception handling and response codes  
✅ Supports `If-Modified-Since` and `Last-Modified` headers for efficient caching

---

## Prerequisites
Ensure the following are installed before proceeding:
- **Java 21+**
- **Maven 3.8+**
- **Docker (optional for containerized deployment)**

---

## Installation

1. **Clone the Repository**
```sh
$ git clone https://github.com/esnet/iri-facility-status-api.git
$ cd iri-facility-status-api
```

2. **Build the Application**
```sh
$ mvn clean install
```

3. **Run the Application**
```sh
$ mvn spring-boot:run
```

4. **Docker Deployment (Optional)**
   To run the application inside a Docker container:
```sh
$ docker build -t iri-facility-status-api .
$ docker run -p 8081:8081 iri-facility-status-api

```

---

## Configuration
The API can be configured through **application.yml** located in `/src/main/resources/`.

### Sample Configuration
```yaml
# Standard spring runtime configuration.
server:
   port: 8081  # Change to the desired port number
   
# Application specific configuration.
iri:
   server:
      root: "http://localhost:8081"
      proxy: "https://iri.es.net"

# Standard logging runtime configuration.
logging:
   level:
      root: INFO
      net.es.iri.api.facility: DEBUG  # Change package-level logging

# IRI Facility Status API test data.
status:
   facilities:
      - id: "09a22593-2be8-46f6-ae54-2904b04e13a4"
        name: "National Energy Research Scientific Computing Center"
        shortName: "NERSC"
```

---

## Endpoints
### **Facility Endpoints**
| Method | Endpoint                   | Description                             |
|---------|----------------------------|-----------------------------------------|
| `GET`   | `/api/v1/status/facility`  | Retrieves facility details              |
| `GET`   | `/api/v1/status/incidents` | Retrieves a list of incidents           |
| `GET`   | `/api/v1/status/events`    | Retrieves a list of events              |
| `GET`   | `/api/v1/status/resources` | Retrieves a list of available resources |
| `GET`   | `/api/v1/status/sites`     | Retrieves a list of physical sites      |
| `GET`   | `/api/v1/status/locations` | Retrieves a list of locations           |

### **Sample Request**
```http
GET /api/v1/status/resources HTTP/1.1
Host: localhost:8081
Accept: application/json
If-Modified-Since: Mon, 01 Jan 2024 00:00:00 GMT
```

### **Sample Response**
```json
[
   {
      "id": "29ea05ad-86de-4df8-b208-f0691aafbaa2",
      "name": "Scratch",
      "short_name": "scratch",
      "description": "The Perlmutter Scratch File System is an all-flash file system.",
      "last_modified": "2025-03-03T07:51:20.000Z",
      "links": [
         {
            "rel": "self",
            "href": "http://localhost:8081/api/v1/status/resources/29ea05ad-86de-4df8-b208-f0691aafbaa2"
         },
         {
            "rel": "hasDependent",
            "href": "http://localhost:8081/api/v1/status/resources/303a692d-c52b-47f0-8699-045f962650e2"
         },
         {
            "rel": "dependsOn",
            "href": "http://localhost:8081/api/v1/status/resources/8b61b346-b53c-4a8e-83b4-776eaa14cc67"
         }
      ],
      "type": "storage",
      "group": "perlmutter"
   }
]
```

---

## Usage

1. **Fetching All Facilities**
```sh
curl -X GET http://localhost:8081/api/v1/status/facility
```

2. **Fetching Resources**
```sh
curl -X GET http://localhost:8081/api/v1/status/resources
```

3. **Requesting Conditional Data**
   Add the `If-Modified-Since` header for optimized data retrieval:
```sh
curl -X GET http://localhost:8081/api/v1/status/incidents -H "If-Modified-Since: Mon, 01 Jan 2024 00:00:00 GMT"
```

---

## Development
### Running Tests
```sh
$ mvn test
```

### Code Style
Follow standard Java best practices. For consistent formatting, we recommend:
- **Checkstyle** for code style enforcement
- **SpotBugs** for static analysis

---

## License
The IRI Facility Status API Reference Implementation is licensed under the **BSD 3-Clause License**. See the [LICENSE](LICENSE) file for more details.

---

## Contact
For issues, improvements, or questions, please contact:
**The IRI Interfaces Subcommittee**  
📧 Email: [software@es.net](mailto:software@es.net)  
🌐 Website: [https://iri.science/ts/interfaces/](https://iri.science/ts/interfaces/)

