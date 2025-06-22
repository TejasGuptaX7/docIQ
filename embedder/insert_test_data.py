import requests
from sentence_transformers import SentenceTransformer

model = SentenceTransformer("all-MiniLM-L6-v2")

texts = [
    "VectorMind is an AI-powered document search system.",
    "It uses semantic embeddings to return better answers.",
]

for text in texts:
    vector = model.encode(text).tolist()
    payload = {
        "class": "Document",
        "properties": {"text": text},
        "vector": vector
    }

    r = requests.post("http://localhost:8080/v1/objects", json=payload)
    print(f"Inserted: {text} — Status {r.status_code}")
