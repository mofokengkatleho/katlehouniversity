import React, { useEffect, useState } from 'react';
import { api } from '../services/api';
import type { MonthlyReport } from '../types';
import Navigation from '../components/Navigation';

const Dashboard: React.FC = () => {
  const [report, setReport] = useState<MonthlyReport | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadReport();
  }, []);

  const loadReport = async () => {
    try {
      const data = await api.getCurrentMonthReport();
      setReport(data);
    } catch (err: any) {
      setError('Failed to load report');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-xl">Loading...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navigation />

      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 py-6 sm:px-0">
          <h2 className="text-2xl font-bold mb-6">
            Monthly Report - {report?.period || 'N/A'}
          </h2>

          {error && (
            <div className="bg-red-50 text-red-700 p-4 rounded-md mb-6">{error}</div>
          )}

          {report && (
            <>
              <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4 mb-8">
                <div className="bg-white overflow-hidden shadow rounded-lg">
                  <div className="px-4 py-5 sm:p-6">
                    <dt className="text-sm font-medium text-gray-500 truncate">
                      Total Children
                    </dt>
                    <dd className="mt-1 text-3xl font-semibold text-gray-900">
                      {report.totalChildren}
                    </dd>
                  </div>
                </div>

                <div className="bg-white overflow-hidden shadow rounded-lg">
                  <div className="px-4 py-5 sm:p-6">
                    <dt className="text-sm font-medium text-gray-500 truncate">Paid</dt>
                    <dd className="mt-1 text-3xl font-semibold text-green-600">
                      {report.paidCount}
                    </dd>
                  </div>
                </div>

                <div className="bg-white overflow-hidden shadow rounded-lg">
                  <div className="px-4 py-5 sm:p-6">
                    <dt className="text-sm font-medium text-gray-500 truncate">Owing</dt>
                    <dd className="mt-1 text-3xl font-semibold text-red-600">
                      {report.owingCount}
                    </dd>
                  </div>
                </div>

                <div className="bg-white overflow-hidden shadow rounded-lg">
                  <div className="px-4 py-5 sm:p-6">
                    <dt className="text-sm font-medium text-gray-500 truncate">
                      Total Collected
                    </dt>
                    <dd className="mt-1 text-3xl font-semibold text-gray-900">
                      R{report.totalCollected.toFixed(2)}
                    </dd>
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
                <div className="bg-white shadow rounded-lg p-6">
                  <h3 className="text-lg font-medium text-green-600 mb-4">
                    Paid Children ({report.paidCount})
                  </h3>
                  <div className="space-y-2">
                    {report.paidChildren.map((child) => (
                      <div
                        key={child.childId}
                        className="flex justify-between items-center p-3 bg-green-50 rounded"
                      >
                        <div>
                          <p className="font-medium">{child.fullName}</p>
                          <p className="text-sm text-gray-600">
                            Ref: {child.paymentReference}
                          </p>
                        </div>
                        <div className="text-right">
                          <p className="font-semibold text-green-700">
                            R{child.amountPaid.toFixed(2)}
                          </p>
                          <p className="text-xs text-gray-500">{child.paymentDate}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="bg-white shadow rounded-lg p-6">
                  <h3 className="text-lg font-medium text-red-600 mb-4">
                    Owing Children ({report.owingCount})
                  </h3>
                  <div className="space-y-2">
                    {report.owingChildren.map((child) => (
                      <div
                        key={child.childId}
                        className="flex justify-between items-center p-3 bg-red-50 rounded"
                      >
                        <div>
                          <p className="font-medium">{child.fullName}</p>
                          <p className="text-sm text-gray-600">
                            Ref: {child.paymentReference}
                          </p>
                        </div>
                        <div className="text-right">
                          <p className="font-semibold text-red-700">
                            Owes: R{child.outstanding.toFixed(2)}
                          </p>
                          <p className="text-xs text-gray-500">
                            Fee: R{child.monthlyFee.toFixed(2)}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
      </main>
    </div>
  );
};

export default Dashboard;
