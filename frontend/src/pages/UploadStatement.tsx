import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import type { UploadedStatement } from '../types';
import Navigation from '../components/Navigation';

export default function UploadStatement() {
  const navigate = useNavigate();
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState<UploadedStatement | null>(null);
  const [isDragging, setIsDragging] = useState(false);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile) {
      validateAndSetFile(selectedFile);
    }
  };

  const validateAndSetFile = (selectedFile: File) => {
    const validExtensions = ['.csv', '.md', '.pdf'];
    const fileExtension = selectedFile.name.toLowerCase().slice(selectedFile.name.lastIndexOf('.'));

    if (!validExtensions.includes(fileExtension)) {
      setError('Only CSV, MD, and PDF files are supported');
      return;
    }

    if (selectedFile.size > 10 * 1024 * 1024) {
      // 10MB limit
      setError('File size must be less than 10MB');
      return;
    }

    setFile(selectedFile);
    setError('');
    setResult(null);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);

    const droppedFile = e.dataTransfer.files[0];
    if (droppedFile) {
      validateAndSetFile(droppedFile);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!file) {
      setError('Please select a file to upload');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const uploadResult = await api.uploadStatement(file);
      setResult(uploadResult);

      if (uploadResult.status === 'FAILED') {
        setError(uploadResult.errorMessage || 'Failed to process statement');
      }
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to upload statement');
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setFile(null);
    setError('');
    setResult(null);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <Navigation />
      <div className="max-w-3xl mx-auto py-8 px-4">
        <div className="bg-white rounded-lg shadow-md p-8">
          <h1 className="text-3xl font-bold text-gray-800 mb-6">Upload Bank Statement</h1>

        <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded">
          <h3 className="font-semibold text-blue-800 mb-2">Instructions:</h3>
          <ul className="list-disc list-inside text-sm text-blue-700 space-y-1">
            <li>Upload bank statement in CSV, Markdown (.md), or PDF format</li>
            <li>System will automatically match transactions to students by student number</li>
            <li>Unmatched transactions can be manually assigned later</li>
            <li>Ensure parent payments include student number (e.g., STU-2025-001) in reference</li>
          </ul>
        </div>

        {error && (
          <div className="mb-4 p-4 bg-red-100 border border-red-400 text-red-700 rounded">
            {error}
          </div>
        )}

        {result && result.status === 'COMPLETED' && (
          <div className="mb-6 p-6 bg-green-50 border border-green-200 rounded">
            <h3 className="font-semibold text-green-800 mb-4">Upload Successful!</h3>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">File:</span>
                <span className="font-medium">{result.fileName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Total Transactions:</span>
                <span className="font-medium">{result.totalTransactions}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-green-600">Matched:</span>
                <span className="font-semibold text-green-700">{result.matchedCount}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-orange-600">Unmatched:</span>
                <span className="font-semibold text-orange-700">{result.unmatchedCount}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Processed:</span>
                <span className="font-medium">
                  {result.processedDate
                    ? new Date(result.processedDate).toLocaleString()
                    : 'N/A'}
                </span>
              </div>
            </div>

            <div className="mt-4 flex space-x-3">
              <button
                onClick={() => navigate('/dashboard')}
                className="flex-1 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                View Dashboard
              </button>
              <button
                onClick={handleReset}
                className="flex-1 px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300 focus:outline-none focus:ring-2 focus:ring-gray-500"
              >
                Upload Another
              </button>
            </div>
          </div>
        )}

        {!result && (
          <form onSubmit={handleSubmit}>
            <div
              className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
                isDragging
                  ? 'border-blue-500 bg-blue-50'
                  : 'border-gray-300 hover:border-gray-400'
              }`}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
            >
              <svg
                className="mx-auto h-12 w-12 text-gray-400 mb-4"
                stroke="currentColor"
                fill="none"
                viewBox="0 0 48 48"
                aria-hidden="true"
              >
                <path
                  d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>

              <div className="mb-4">
                {file ? (
                  <div className="space-y-2">
                    <p className="text-sm font-medium text-gray-700">Selected file:</p>
                    <p className="text-lg font-semibold text-blue-600">{file.name}</p>
                    <p className="text-sm text-gray-500">
                      {(file.size / 1024).toFixed(2)} KB
                    </p>
                  </div>
                ) : (
                  <p className="text-gray-600">
                    Drag and drop your bank statement here, or click to browse
                  </p>
                )}
              </div>

              <input
                type="file"
                id="file-upload"
                accept=".csv,.md,.pdf"
                onChange={handleFileChange}
                className="hidden"
              />

              <label
                htmlFor="file-upload"
                className="inline-block px-4 py-2 bg-blue-600 text-white rounded cursor-pointer hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {file ? 'Choose Different File' : 'Select File'}
              </label>

              <p className="mt-2 text-xs text-gray-500">CSV, MD, or PDF files up to 10MB</p>
            </div>

            <div className="mt-6 flex justify-end space-x-4">
              <button
                type="button"
                onClick={() => navigate('/dashboard')}
                className="px-6 py-2 border border-gray-300 rounded text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={!file || loading}
                className="px-6 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
              >
                {loading ? 'Uploading...' : 'Upload & Process'}
              </button>
            </div>
          </form>
        )}
        </div>
      </div>
    </div>
  );
}
