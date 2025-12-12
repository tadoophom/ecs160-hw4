## How to Run

You will need to run 3 different terminals running at the same timeto get this application working. 

### Terminal 1: Ollama Server

This runs the LLM backend.

```bash
ollama serve
```

Start this one first since the other components depend on it.

### Terminal 2: Spring Boot Backend

This is where you can see the LLM responses in real-time.

```bash
cd spring-boot-app
mvn spring-boot:run
```

### Terminal 3: Main Application

This runs the main app.

```bash
./script.sh
```

- **Terminal 1:** Ollama model activity
- **Terminal 2:** LLM responses 
- **Terminal 3:** The main application output
