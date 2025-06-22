# VectorMind

**VectorMind** is a **local‑first** AI‑powered document‑understanding toolkit that blends fast vector search with LLM‑ready retrieval.

---

## 🔧 Stack

* 🧠 **Embedding** · `MiniLM‑L6‑v2` (local Sentence‑Transformers)
* 📦 **Vector DB** · Weaviate (Docker self‑hosted)
* ☕ **Backend** · Spring Boot (Java)
* 🐍 **Embedder** · Flask (Python)
* 🔎 **RAG Logic** · Similarity search → context → LLM (plug‑in)

---

## ✅ Current Features

* 📤 Upload **PDF** or **TXT**
* 📑 Auto‑chunk & embed text
* 🗄 Store vectors in Weaviate
* 🔍 Ask questions → returns top‑matching chunks (RAG‑ready JSON)

---

## 🚧 Roadmap / Coming Soon

| Feature                        | Status        |
| ------------------------------ | ------------- |
| 💬 LLM answers (Groq / Gemini) | ⏳ In progress |
| 🌐 React frontend (dark UI)    | ⏳ Planned     |
| 🔄 Multi‑LLM switcher          | ⏳ Planned     |
| ☁️ AWS / Fargate deployment    | ⏳ Planned     |

---

## ⚙️ Run Locally

```bash
# 1. Clone
 git clone https://github.com/TejasGuptaX7/VectorMind.git && cd VectorMind

# 2. Start Weaviate
 docker compose up -d  # exposes :8080

# 3. Start Spring Boot (port 8082)
 cd api
 ./mvnw spring-boot:run

# 4. Start Embedder (port 5001)
 cd ../embedder
 python3 -m venv venv && source venv/bin/activate
 pip install -r requirements.txt
 python embedder.py
```

> Front‑end & RAG answer endpoint will run on `localhost:8082` once upcoming features land.

---

## 📝 License

MIT — fork, extend, and build your own killer feature.
