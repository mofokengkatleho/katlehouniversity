import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import type { Child, Payment } from '@/types'
import { getChildById } from '@/services/api'

export default function StudentDetail() {
  const { id } = useParams<{ id: string }>()
  const [student, setStudent] = useState<Child | null>(null)
  const [payments, setPayments] = useState<Payment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    loadStudentData()
  }, [id])

  const loadStudentData = async () => {
    if (!id) return

    try {
      setLoading(true)
      const data = await getChildById(parseInt(id))
      setStudent(data)
      // In a real app, you'd fetch payments separately
      setPayments(data.payments || [])
      setError('')
    } catch (err) {
      setError('Failed to load student details')
      console.error(err)
    } finally {
      setLoading(false)
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
      month: 'long',
      day: 'numeric',
    })
  }

  const getPaymentStatusBadge = (status: string) => {
    const variants: Record<string, 'success' | 'warning' | 'destructive' | 'secondary'> = {
      PAID: 'success',
      PARTIAL: 'warning',
      PENDING: 'destructive',
      OVERPAID: 'secondary',
    }
    return <Badge variant={variants[status] || 'outline'}>{status}</Badge>
  }

  const calculateTotalPaid = () => {
    return payments.reduce((sum, payment) => sum + payment.amountPaid, 0)
  }

  const calculateOutstanding = () => {
    const currentMonth = new Date().getMonth() + 1
    const expectedTotal = student ? student.monthlyFee * currentMonth : 0
    return expectedTotal - calculateTotalPaid()
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-gray-500">Loading student details...</div>
      </div>
    )
  }

  if (error || !student) {
    return (
      <div className="min-h-screen bg-gray-50">
        <nav className="bg-white shadow-sm">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between h-16 items-center">
              <h1 className="text-2xl font-bold text-gray-900">ECD Payment System</h1>
              <Button onClick={() => navigate('/students')}>Back to Students</Button>
            </div>
          </div>
        </nav>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="bg-red-100 text-red-700 p-4 rounded">
            {error || 'Student not found'}
          </div>
        </div>
      </div>
    )
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
              <Button variant="ghost" onClick={() => navigate('/students')}>
                Students
              </Button>
            </div>
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Button variant="outline" onClick={() => navigate('/students')} className="mb-4">
          ← Back to Students List
        </Button>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Student Information Card */}
          <Card className="lg:col-span-1">
            <CardHeader>
              <CardTitle>Student Information</CardTitle>
              <CardDescription>{student.studentNumber}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <p className="text-sm text-gray-500">Full Name</p>
                <p className="font-semibold">{student.firstName} {student.lastName}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Gender</p>
                <p>{student.gender}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Date of Birth</p>
                <p>{student.dateOfBirth ? formatDate(student.dateOfBirth) : 'N/A'}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Grade/Class</p>
                <p>{student.gradeClass}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Academic Year</p>
                <p>{student.academicYear}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Status</p>
                <Badge variant={student.status === 'ACTIVE' ? 'success' : 'secondary'}>
                  {student.status}
                </Badge>
              </div>
              <hr />
              <div>
                <p className="text-sm text-gray-500">Guardian Name</p>
                <p>{student.parentName || student.guardianName || 'N/A'}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Guardian Contact</p>
                <p>{student.parentPhone || student.guardianContact || 'N/A'}</p>
              </div>
              {(student.guardianEmail || student.parentEmail) && (
                <div>
                  <p className="text-sm text-gray-500">Guardian Email</p>
                  <p className="text-sm">{student.guardianEmail || student.parentEmail}</p>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Payment Information */}
          <div className="lg:col-span-2 space-y-6">
            {/* Payment Summary Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <Card>
                <CardHeader className="pb-3">
                  <CardDescription>Monthly Fee</CardDescription>
                  <CardTitle className="text-2xl">
                    {formatCurrency(student.monthlyFee)}
                  </CardTitle>
                </CardHeader>
              </Card>
              <Card>
                <CardHeader className="pb-3">
                  <CardDescription>Total Paid</CardDescription>
                  <CardTitle className="text-2xl text-green-600">
                    {formatCurrency(calculateTotalPaid())}
                  </CardTitle>
                </CardHeader>
              </Card>
              <Card>
                <CardHeader className="pb-3">
                  <CardDescription>Outstanding</CardDescription>
                  <CardTitle className={`text-2xl ${calculateOutstanding() > 0 ? 'text-red-600' : 'text-green-600'}`}>
                    {formatCurrency(Math.max(0, calculateOutstanding()))}
                  </CardTitle>
                </CardHeader>
              </Card>
            </div>

            {/* Payment History Table */}
            <Card>
              <CardHeader>
                <CardTitle>Payment History</CardTitle>
                <CardDescription>
                  All payments recorded for this student
                </CardDescription>
              </CardHeader>
              <CardContent>
                {payments.length === 0 ? (
                  <div className="text-center py-8 text-gray-500">
                    No payments recorded yet
                  </div>
                ) : (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Date</TableHead>
                        <TableHead>Month/Year</TableHead>
                        <TableHead>Amount</TableHead>
                        <TableHead>Expected</TableHead>
                        <TableHead>Method</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead>Auto-Matched</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {payments.map((payment) => (
                        <TableRow key={payment.id}>
                          <TableCell>{formatDate(payment.paymentDate)}</TableCell>
                          <TableCell>
                            {new Date(2024, payment.paymentMonth - 1).toLocaleString('default', { month: 'long' })} {payment.paymentYear}
                          </TableCell>
                          <TableCell className="font-semibold">
                            {formatCurrency(payment.amountPaid)}
                          </TableCell>
                          <TableCell className="text-gray-500">
                            {formatCurrency(payment.expectedAmount)}
                          </TableCell>
                          <TableCell>
                            <Badge variant="outline">{payment.paymentMethod}</Badge>
                          </TableCell>
                          <TableCell>
                            {getPaymentStatusBadge(payment.status)}
                          </TableCell>
                          <TableCell>
                            {payment.matchedAutomatically ? (
                              <Badge variant="success">Auto</Badge>
                            ) : (
                              <Badge variant="secondary">Manual</Badge>
                            )}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  )
}
