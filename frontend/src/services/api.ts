import axios from 'axios';
import type { AxiosInstance } from 'axios';
import type {
  Child,
  MonthlyReport,
  Transaction,
  AuthResponse,
  LoginRequest,
  UploadedStatement,
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Add auth token to requests
    this.client.interceptors.request.use((config) => {
      const token = localStorage.getItem('authToken');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });

    // Handle 401 and 403 errors (expired or invalid token)
    this.client.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401 || error.response?.status === 403) {
          localStorage.removeItem('authToken');
          localStorage.removeItem('userData');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  // Authentication
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await this.client.post<AuthResponse>('/auth/login', credentials);
    return response.data;
  }

  // Children
  async getChildren(activeOnly: boolean = true): Promise<Child[]> {
    const response = await this.client.get<Child[]>('/children', {
      params: { activeOnly },
    });
    return response.data;
  }

  async getChildById(id: number): Promise<Child> {
    const response = await this.client.get<Child>(`/children/${id}`);
    return response.data;
  }

  async createChild(child: Child): Promise<Child> {
    const response = await this.client.post<Child>('/children', child);
    return response.data;
  }

  async updateChild(id: number, child: Child): Promise<Child> {
    const response = await this.client.put<Child>(`/children/${id}`, child);
    return response.data;
  }

  async deleteChild(id: number): Promise<void> {
    await this.client.delete(`/children/${id}`);
  }

  async searchChildren(name: string): Promise<Child[]> {
    const response = await this.client.get<Child[]>('/children/search', {
      params: { name },
    });
    return response.data;
  }

  // Reports
  async getMonthlyReport(month: number, year: number): Promise<MonthlyReport> {
    const response = await this.client.get<MonthlyReport>('/reports/monthly', {
      params: { month, year },
    });
    return response.data;
  }

  async getCurrentMonthReport(): Promise<MonthlyReport> {
    const response = await this.client.get<MonthlyReport>('/reports/monthly/current');
    return response.data;
  }

  // Transactions
  async getUnmatchedTransactions(): Promise<Transaction[]> {
    const response = await this.client.get<Transaction[]>('/transactions/unmatched');
    return response.data;
  }

  async matchAllTransactions(): Promise<void> {
    await this.client.post('/transactions/match-all');
  }

  async manuallyMatchTransaction(
    transactionId: number,
    childId: number,
    month: number,
    year: number
  ): Promise<void> {
    await this.client.post(`/transactions/${transactionId}/match`, null, {
      params: { childId, month, year },
    });
  }

  // Statement Upload
  async uploadStatement(file: File): Promise<UploadedStatement> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await this.client.post<UploadedStatement>('/statements/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  }

  async getAllStatements(): Promise<UploadedStatement[]> {
    const response = await this.client.get<UploadedStatement[]>('/statements');
    return response.data;
  }

  async getStatementById(id: number): Promise<UploadedStatement> {
    const response = await this.client.get<UploadedStatement>(`/statements/${id}`);
    return response.data;
  }
}

export const api = new ApiClient();

// Export individual methods for convenience
export const {
  login,
  getChildren,
  getChildById,
  createChild,
  updateChild,
  deleteChild,
  searchChildren,
  getMonthlyReport,
  getCurrentMonthReport,
  getUnmatchedTransactions,
  matchAllTransactions,
  manuallyMatchTransaction,
  uploadStatement,
  getAllStatements,
  getStatementById,
} = api;
