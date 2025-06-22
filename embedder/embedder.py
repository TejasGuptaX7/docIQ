from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer

app = Flask(__name__)
model = SentenceTransformer("all-MiniLM-L6-v2")

@app.route("/embed", methods=["POST"])
def embed():
    try:
        data = request.get_json()
        texts = data.get("texts", [])

        if not isinstance(texts, list) or not texts:
            return jsonify({"error": "Expected a non-empty list of 'texts'"}), 400

        embeddings = model.encode(texts).tolist()

        return jsonify({"embeddings": embeddings})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001)
