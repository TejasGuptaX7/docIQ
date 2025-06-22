# VectorMind

VectorMind is a local-first AI-powered document understanding system that combines vector search with LLM-powered Q&A.

## 🔧 Stack
- 🧠 Embedding: `MiniLM-L6-v2` (local)
- 📦 Vector DB: Weaviate (self-hosted via Docker)
- 🛠 Backend: Spring Boot (Java)
- 🌐 Embedder: Flask (Python)
- 🔍 Semantic search + RAG logic

## ✅ Features
- Upload `.pdf` or `.txt`
- Chunk text and embed
- Store in Weaviate
- Ask questions and get top-matching chunks (RAG-ready)

## 🚧 Coming Soon
- LLM answers using Groq/Gemini
- React frontend
- Multi-LLM switcher
- AWS deployment

## ⚙️ Run Locally

```bash
# Start Weaviate
docker compose up -d

# Start Spring Boot (port 8082)
cd api
./mvnw spring-boot:run

# Start embedder (port 5001)
cd embedder
python3 embedder.py
