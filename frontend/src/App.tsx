import { useState } from 'react';
import axios from 'axios';

function App() {
  const [text, setText] = useState('');
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploadStatus, setUploadStatus] = useState('');

  const storeText = async () => {
    if (!text.trim()) return;
    try {
      await axios.post('http://localhost:8082/store', { text });
      alert("Stored in VectorMind!");
      setText('');
    } catch (err) {
      console.error(err);
      alert("Failed to store");
    }
  };

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    try {
      setUploadStatus(`Uploading ${file.name}...`);
      const response = await axios.post('http://localhost:8082/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      
      const data = response.data;
      const chunkDetails = data.chunkDetails;
      setUploadStatus(
        `Successfully processed ${data.filename}\n` +
        `Divided into ${chunkDetails.totalChunks} chunks\n` +
        `Total words: ${chunkDetails.totalWords}\n` +
        `Words per chunk: ${chunkDetails.wordsPerChunk}`
      );
      setTimeout(() => setUploadStatus(''), 8000);
    } catch (err: any) {
      console.error(err);
      const errorMessage = err.response?.data?.message || err.message;
      setUploadStatus(`Failed to upload ${file.name}: ${errorMessage}`);
      setTimeout(() => setUploadStatus(''), 5000);
    }
  };

  const searchText = async () => {
    if (!query.trim()) return;
    setLoading(true);
    try {
      const res = await axios.post('http://localhost:8082/search', { query });
      const hits = res.data?.data?.Get?.Document || [];
      setResults(hits.map((item: any) => item.text));
    } catch (err) {
      console.error(err);
      alert("Search failed.");
    }
    setLoading(false);
  };

  return (
    <div className="fixed inset-0 bg-black">
      <div className="min-h-screen bg-zinc-900 p-6 font-sans">
        <div className="max-w-xl mx-auto bg-zinc-800 shadow-lg rounded-2xl p-6">
          <h1 className="text-3xl font-bold mb-6 text-center text-white">🧠 VectorMind</h1>

          {/* Chat Interface */}
          <div className="space-y-4">
            {/* Results Display */}
            {!loading && results.length > 0 && (
              <div className="bg-zinc-700 rounded-lg p-4">
                <h2 className="text-xl font-semibold mb-2 text-white">Search Results:</h2>
                <ul className="space-y-2">
                  {results.map((text, i) => (
                    <li key={i} className="text-gray-200 bg-zinc-600 p-3 rounded">{text}</li>
                  ))}
                </ul>
              </div>
            )}

            {/* Search Input */}
            <div className="flex gap-2">
              <input
                className="flex-1 bg-zinc-700 text-white border border-zinc-600 p-3 rounded-lg focus:outline-none focus:border-blue-500"
                placeholder="Ask a question..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
              <button
                className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition"
                onClick={searchText}
              >
                Search
              </button>
            </div>

            {/* Upload Section */}
            <div className="bg-zinc-700 rounded-lg p-4">
              <div className="flex items-center gap-4">
                <div className="flex-1">
                  <textarea
                    className="w-full bg-zinc-600 text-white border border-zinc-500 p-3 rounded-lg focus:outline-none focus:border-blue-500"
                    placeholder="Or type your text here..."
                    rows={3}
                    value={text}
                    onChange={(e) => setText(e.target.value)}
                  />
                </div>
                <div className="flex flex-col gap-2">
                  <button
                    className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition"
                    onClick={storeText}
                  >
                    Store
                  </button>
                  <label className="bg-zinc-600 text-white px-4 py-2 rounded-lg hover:bg-zinc-500 transition cursor-pointer text-center">
                    <input
                      type="file"
                      accept=".pdf,.txt,.png,.jpg,.jpeg"
                      onChange={handleFileUpload}
                      className="hidden"
                    />
                    Upload
                  </label>
                </div>
              </div>
              {uploadStatus && (
                <p className="mt-2 text-sm text-gray-300">{uploadStatus}</p>
              )}
            </div>
          </div>

          {/* Loading State */}
          {loading && (
            <div className="text-center mt-4 text-gray-300">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-white mx-auto"></div>
              <p className="mt-2">Searching...</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default App;
