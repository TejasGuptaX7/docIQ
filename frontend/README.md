# VectorMind

**VectorMind** is an open‑source Retrieval‑Augmented‑Generation (RAG) platform that lets you upload PDFs (or raw text), ask plain‑English questions, and receive source‑anchored answers from GPT‑3.5.  It is architected to be production‑ready first—so you can plug in your own standout feature (citations, clipboard capture, VS Code overlay, etc.) without rewriting the plumbing.

---

## ✨ Why You Might Use It

| Capability                    | What It Gives You                                                                                                            |
| ----------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| **Upload → Chunk → Embed**    | Streams multi‑page PDFs, splits into sensible chunks, embeds via `sentence‑transformers`.                                    |
| **Vector Store**              | Uses **Weaviate** with no built‑in vectorizer (you control embeddings).                                                      |
| **Query Pipeline**            | Spring Boot endpoint embeds the user question, performs `nearVector` search, feeds the top N chunks to GPT‑3.5 (or any LLM). |
| **Fallback & Logging**        | Automatic mock answer + console log if embedder, Weaviate, or OpenAI fails.                                                  |
| **Dark‑Mode React Front‑end** | Upload UI, chat‑style Q\&A panel, file‑history list—built with Tailwind‑CSS.                                                 |
| **Config‑Driven Secrets**     | `openai.api.key` pulled from `application.properties` **or** `OPENAI_API_KEY` env var—no hard‑coded secrets.                 |

---

## 🏗 Architecture

```text
client (React) ─┬─►  /upload  ─►  Spring Boot (API) ─►  Flask embedder  
               │              │                           ↓
               │              └──── store vectors ─────►  Weaviate DB
               │
               └─►  /search  ─►  (embed question) ─► Weaviate (nearVector)
                                          │
                                          ▼
                                   GPT‑3.5 / OpenAI
                                          │
                                          ▼
                                     JSON answer
```

---

## 🚀 Quick Start (Local)

```bash
# clone and enter
 git clone https://github.com/yourname/vectormind.git
 cd vectormind

# 1. start Weaviate via docker compose
 docker-compose up -d

# 2. run Flask embedder
 cd embedder
 python3 -m venv venv && source venv/bin/activate
 pip install -r requirements.txt   # flask + sentence‑transformers
 python embedder.py

# 3. run Spring Boot (backend)
 cd ../api
 # put your key in application.properties  OR  export OPENAI_API_KEY=sk‑...
 ./mvnw spring-boot:run

# 4. run React front‑end
 cd ../frontend
 npm install && npm run dev
```

Open [http://localhost:5173](http://localhost:5173) and start uploading.

---

## 🔧 Environment Configuration

| Property / Env Var                     | Purpose                 |
| -------------------------------------- | ----------------------- |
| `openai.api.key` *or* `OPENAI_API_KEY` | GPT access token        |
| `server.port` (Spring)                 | Default **8082**        |
| `EMBEDDER_URL` (optional)              | Override Flask endpoint |

---

## 🛠 Roadmap (Tech)

* [x] Health‑check endpoints for all services
* [x] Retry wrapper + exponential back‑off around OpenAI
* [ ] Source‑anchored citations (`page`, `start`, `end` props)
* [ ] Docker‑compose that spins everything, including front‑end
* [ ] Optional Firebase/Clerk auth gate

---

## 🤝 Contributing

Pull requests are welcome—especially around chunk‑splitting, citation rendering, and VS Code overlay ideas.

1. Fork → Branch → PR
2. `pre-commit run --all-files` to lint

---

## 📝 License

MIT.  Use it, fork it, ship your own killer feature.
