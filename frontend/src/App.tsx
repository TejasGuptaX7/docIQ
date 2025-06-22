// Modern VectorMind Frontend with Cluely-style Layout, ShadCN, and Tailwind
// Features: File Upload, Chat-style Q&A, History List, Status UI

import React, { useState } from 'react';
import axios from 'axios';
import './App.css';


export default function VectorMind() {
  const [file, setFile] = useState<File | null>(null);
  const [query, setQuery] = useState('');
  const [answer, setAnswer] = useState('');
  const [loading, setLoading] = useState(false);
  const [uploadStatus, setUploadStatus] = useState('');
  const [history, setHistory] = useState<string[]>([]);

  const handleUpload = async () => {
    if (!file) return;
    const formData = new FormData();
    formData.append('file', file);

    try {
      setUploadStatus('Uploading...');
      await axios.post('http://localhost:8082/upload', formData);
      setUploadStatus('Upload successful');
      setHistory((prev) => [file.name, ...prev]);
      setTimeout(() => setUploadStatus(''), 4000);
    } catch (err) {
      console.error(err);
      setUploadStatus('Upload failed');
      setTimeout(() => setUploadStatus(''), 4000);
    }
  };

  const handleSearch = async () => {
    if (!query) return;
    try {
      setLoading(true);
      const res = await axios.post('http://localhost:8082/search', { query });
      setAnswer(res.data.answer);
    } catch (err) {
      console.error(err);
      setAnswer('Failed to retrieve answer.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-neutral-950 text-white p-6 font-sans">
      <div className="max-w-3xl mx-auto">
        <h1 className="text-4xl font-bold text-center mb-8">🧠 VectorMind</h1>

        {/* Upload Box */}
        <div className="bg-neutral-800 p-6 rounded-2xl mb-6">
          <h2 className="text-lg mb-2 font-semibold">Upload a Document</h2>
          <div className="flex items-center gap-4">
            <input
              type="file"
              accept="application/pdf"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
              className="w-full file:bg-blue-600 file:text-white file:rounded file:px-4 file:py-2 text-sm"
            />
            <button
              onClick={handleUpload}
              className="bg-blue-600 px-4 py-2 rounded-lg hover:bg-blue-700"
            >
              Upload
            </button>
          </div>
          {uploadStatus && <p className="text-sm mt-2 text-blue-300">{uploadStatus}</p>}
        </div>

        {/* Search Section */}
        <div className="bg-neutral-800 p-6 rounded-2xl mb-6">
          <h2 className="text-lg font-semibold mb-2">Ask a Question</h2>
          <div className="flex gap-2">
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Ask something about your document..."
              className="flex-1 bg-neutral-700 px-4 py-2 rounded focus:outline-none"
            />
            <button
              onClick={handleSearch}
              className="bg-green-600 px-4 py-2 rounded hover:bg-green-700"
              disabled={loading}
            >
              Search
            </button>
          </div>
          {loading && <p className="text-sm mt-2 text-gray-300">Searching...</p>}
        </div>

        {/* Answer Display */}
        {answer && (
          <div className="bg-neutral-800 p-6 rounded-2xl mb-6">
            <h2 className="text-lg font-semibold mb-2">Answer</h2>
            <p className="whitespace-pre-line text-gray-200">{answer}</p>
          </div>
        )}

        {/* File History */}
        {history.length > 0 && (
          <div className="bg-neutral-800 p-6 rounded-2xl">
            <h2 className="text-lg font-semibold mb-2">Recent Uploads</h2>
            <ul className="list-disc list-inside space-y-1 text-sm text-gray-300">
              {history.map((file, idx) => (
                <li key={idx}>{file}</li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
