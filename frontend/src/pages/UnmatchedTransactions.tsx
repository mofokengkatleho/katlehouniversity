import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import type { Transaction, Child } from '@/types'
import { getUnmatchedTransactions, getChildren, manuallyMatchTransaction } from '@/services/api'

export default function UnmatchedTransactions() {
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [students, setStudents] = useState<Child[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [matching, setMatching] = useState(false)
  const [selectedTransaction, setSelectedTransaction] = useState<string | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      const [transactionsData, studentsData] = await Promise.all([
        getUnmatchedTransactions(),
        getChildren(true),
      ])
      setTransactions(transactionsData)
      setStudents(studentsData)
      setError('')
    } catch (err) {
      setError('Failed to load unmatched transactions')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleMatch = async (transactionId: string, studentId: string) => {
    try {
      setMatching(true)
      setSelectedTransaction(transactionId)
      const now = new Date()
      const month = now.getMonth() + 1
      const year = now.getFullYear()
      await manuallyMatchTransaction(parseInt(transactionId), parseInt(studentId), month, year)
      // Reload data after successful match
      await loadData()
      setError('')
    } catch (err) {
      setError('Failed to match transaction')
      console.error(err)
    } finally {
      setMatching(false)
      setSelectedTransaction(null)
    }
  }

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-ZA', {
      style: 'currency',
      currency: 'ZAR',
    }).format(amount)
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-ZA', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    })
  }

  const getTransactionTypeBadge = (type: string) => {
    const variants: Record<string, 'success' | 'destructive' | 'secondary'> = {
      CREDIT: 'success',
      DEBIT: 'destructive',
      REVERSAL: 'secondary',
    }
    return <Badge variant={variants[type] || 'outline'}>{type}</Badge>
  }

  const findSuggestedStudent = (transaction: Transaction): Child | null => {
    // Try to find student by reference or description
    const searchText = `${transaction.paymentReference || ''} ${transaction.description}`.toLowerCase()

    return students.find(student => {
      const studentNumber = (student.studentNumber || '').toLowerCase()
      const fullName = `${student.firstName} ${student.lastName}`.toLowerCase()
      return searchText.includes(studentNumber) || searchText.includes(fullName)
    }) || null
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <nav className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            <h1 className="text-2xl font-bold text-gray-900">ECD Payment System</h1>
            <div className="flex gap-4">
              <Button variant="ghost" onClick={() => navigate('/dashboard')}>
                Dashboard
              </Button>
              <Button variant="ghost" onClick={() => navigate('/register')}>
                Register Student
              </Button>
              <Button variant="ghost" onClick={() => navigate('/upload')}>
                Upload Statement
              </Button>
              <Button variant="ghost" onClick={() => navigate('/students')}>
                Students
              </Button>
              <Button variant="ghost" onClick={() => navigate('/transactions')}>
                Transactions
              </Button>
            </div>
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Card>
          <CardHeader>
            <CardTitle>Unmatched Transactions</CardTitle>
            <CardDescription>
              Review and manually match transactions to students
            </CardDescription>
          </CardHeader>
          <CardContent>
            {/* Summary Banner */}
            {!loading && (
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-blue-900">
                      {transactions.length} unmatched transaction{transactions.length !== 1 ? 's' : ''} requiring review
                    </p>
                    <p className="text-xs text-blue-700 mt-1">
                      These transactions could not be automatically matched to any student
                    </p>
                  </div>
                  <Button variant="outline" onClick={loadData}>
                    Refresh
                  </Button>
                </div>
              </div>
            )}

            {/* Error Message */}
            {error && (
              <div className="bg-red-100 text-red-700 p-4 rounded mb-4">
                {error}
              </div>
            )}

            {/* Loading State */}
            {loading && (
              <div className="text-center py-8 text-gray-500">
                Loading unmatched transactions...
              </div>
            )}

            {/* Empty State */}
            {!loading && transactions.length === 0 && (
              <div className="text-center py-12">
                <div className="text-green-600 text-4xl mb-4">✓</div>
                <p className="text-lg font-semibold text-gray-900">All Caught Up!</p>
                <p className="text-gray-500 mt-2">
                  There are no unmatched transactions at the moment.
                </p>
              </div>
            )}

            {/* Transactions Table */}
            {!loading && transactions.length > 0 && (
              <div className="space-y-6">
                {transactions.map((transaction) => {
                  const suggestedStudent = findSuggestedStudent(transaction)

                  return (
                    <Card key={transaction.id} className="border-2">
                      <CardContent className="pt-6">
                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                          {/* Transaction Details */}
                          <div>
                            <h4 className="font-semibold text-lg mb-4">Transaction Details</h4>
                            <div className="space-y-3">
                              <div>
                                <p className="text-sm text-gray-500">Date</p>
                                <p className="font-medium">{formatDate(transaction.transactionDate)}</p>
                              </div>
                              <div>
                                <p className="text-sm text-gray-500">Amount</p>
                                <p className="text-lg font-bold text-green-600">
                                  {formatCurrency(transaction.amount)}
                                </p>
                              </div>
                              <div>
                                <p className="text-sm text-gray-500">Description</p>
                                <p className="font-mono text-sm bg-gray-100 p-2 rounded">
                                  {transaction.description}
                                </p>
                              </div>
                              {transaction.paymentReference && (
                                <div>
                                  <p className="text-sm text-gray-500">Reference</p>
                                  <p className="font-mono text-sm">{transaction.paymentReference}</p>
                                </div>
                              )}
                              {transaction.senderName && (
                                <div>
                                  <p className="text-sm text-gray-500">Sender Name</p>
                                  <p>{transaction.senderName}</p>
                                </div>
                              )}
                              <div>
                                <p className="text-sm text-gray-500">Type</p>
                                {getTransactionTypeBadge(transaction.type)}
                              </div>
                            </div>
                          </div>

                          {/* Matching Interface */}
                          <div>
                            <h4 className="font-semibold text-lg mb-4">Match to Student</h4>

                            {/* Suggested Match */}
                            {suggestedStudent && (
                              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-4">
                                <p className="text-sm font-medium text-yellow-900 mb-2">
                                  Suggested Match
                                </p>
                                <div className="flex items-center justify-between">
                                  <div>
                                    <p className="font-semibold">
                                      {suggestedStudent.firstName} {suggestedStudent.lastName}
                                    </p>
                                    <p className="text-sm text-gray-600">{suggestedStudent.studentNumber}</p>
                                    <p className="text-sm text-gray-600">
                                      Monthly Fee: {formatCurrency(suggestedStudent.monthlyFee)}
                                    </p>
                                  </div>
                                  <Button
                                    onClick={() => handleMatch(transaction.id.toString(), suggestedStudent.id)}
                                    disabled={matching && selectedTransaction === transaction.id.toString()}
                                  >
                                    {matching && selectedTransaction === transaction.id.toString()
                                      ? 'Matching...'
                                      : 'Match'}
                                  </Button>
                                </div>
                              </div>
                            )}

                            {/* Student Selector */}
                            <div>
                              <label className="block text-sm font-medium text-gray-700 mb-2">
                                Select Student to Match
                              </label>
                              <select
                                className="w-full border border-gray-300 rounded-lg p-2 mb-3"
                                onChange={(e) => {
                                  if (e.target.value) {
                                    handleMatch(transaction.id.toString(), e.target.value)
                                  }
                                }}
                                disabled={matching && selectedTransaction === transaction.id.toString()}
                                value=""
                              >
                                <option value="">-- Choose a student --</option>
                                {students.map((student) => (
                                  <option key={student.id} value={student.id}>
                                    {student.studentNumber} - {student.firstName} {student.lastName} (
                                    {formatCurrency(student.monthlyFee)})
                                  </option>
                                ))}
                              </select>
                              <p className="text-xs text-gray-500">
                                Or select from the dropdown to manually assign this transaction
                              </p>
                            </div>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  )
                })}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
