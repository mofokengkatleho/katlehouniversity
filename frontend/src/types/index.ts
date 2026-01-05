export interface Child {
  id: string;
  studentNumber?: string;
  firstName: string;
  lastName: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER';
  studentIdNumber?: string;
  physicalAddress?: string;
  allergies?: string;
  paymentReference?: string;
  monthlyFee: number;
  paymentDay?: number;
  parentPhone?: string;
  parentEmail?: string;
  guardianEmail?: string;
  parentName?: string;
  guardianName?: string;
  guardianContact?: string;
  gradeClass?: string;
  academicYear?: string;
  dateOfBirth?: string;
  enrollmentDate?: string;
  status: 'ACTIVE' | 'GRADUATED' | 'WITHDRAWN' | 'SUSPENDED';
  active: boolean;
  notes?: string;
  payments?: Payment[];
}

export interface UploadedStatement {
  id?: number;
  fileName: string;
  fileType: 'CSV' | 'MARKDOWN';
  totalTransactions: number;
  matchedCount: number;
  unmatchedCount: number;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  errorMessage?: string;
  uploadDate: string;
  processedDate?: string;
}

export interface Payment {
  id?: number;
  childId: number;
  paymentMonth: number;
  paymentYear: number;
  amountPaid: number;
  expectedAmount: number;
  paymentDate: string;
  status: 'PENDING' | 'PAID' | 'PARTIAL' | 'OVERPAID' | 'REVERSED';
  paymentMethod: 'BANK_TRANSFER' | 'CASH' | 'CARD' | 'OTHER';
  transactionReference?: string;
  matchedAutomatically?: boolean;
  notes?: string;
}

export interface Transaction {
  id: string;
  bankReference: string;
  amount: number;
  transactionDate: string;
  paymentReference?: string;
  description: string;
  senderName?: string;
  senderAccount?: string;
  status: 'UNMATCHED' | 'MATCHED' | 'PARTIALLY_MATCHED' | 'IGNORED' | 'DISPUTED';
  type: 'CREDIT' | 'DEBIT' | 'REVERSAL';
}

export interface MonthlyReport {
  month: number;
  year: number;
  period: string;
  paidChildren: ChildPaymentStatus[];
  owingChildren: ChildPaymentStatus[];
  totalCollected: number;
  totalExpected: number;
  totalOutstanding: number;
  totalChildren: number;
  paidCount: number;
  owingCount: number;
}

export interface ChildPaymentStatus {
  childId: number;
  fullName: string;
  monthlyFee: number;
  amountPaid: number;
  outstanding: number;
  paymentReference: string;
  status: string;
  paymentDate?: string;
}

export interface User {
  id?: number;
  username: string;
  fullName: string;
  email?: string;
  role: 'ADMIN' | 'SUPER_ADMIN';
  active: boolean;
}

export interface AuthResponse {
  token: string;
  type: string;
  username: string;
  fullName: string;
  role: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}
