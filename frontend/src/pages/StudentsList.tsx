import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import type { Child } from '@/types'
import { getChildren } from '@/services/api'

export default function StudentsList() {
  const [students, setStudents] = useState<Child[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [filter, setFilter] = useState<'ALL' | 'ACTIVE' | 'GRADUATED' | 'WITHDRAWN'>('ACTIVE')
  const navigate = useNavigate()

  useEffect(() => {
    loadStudents()
  }, [filter])

  const loadStudents = async () => {
    try {
      setLoading(true)
      const active = filter === 'ACTIVE'
      const data = await getChildren(active)

      // Apply additional filtering if needed
      const filteredData = filter === 'ALL'
        ? data
        : data.filter((s: Child) => s.status === filter)

      setStudents(filteredData)
      setError('')
    } catch (err) {
      setError('Failed to load students')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const getStatusBadge = (status: string) => {
    const variants: Record<string, 'success' | 'secondary' | 'destructive' | 'outline' | 'warning'> = {
      ACTIVE: 'success',
      GRADUATED: 'secondary',
      WITHDRAWN: 'destructive',
      SUSPENDED: 'warning',
    }
    return <Badge variant={variants[status] || 'outline'}>{status}</Badge>
  }

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-ZA', {
      style: 'currency',
      currency: 'ZAR',
    }).format(amount)
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
            <div className="flex justify-between items-center">
              <CardTitle>All Students</CardTitle>
              <Button onClick={() => navigate('/register')}>
                + Register New Student
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {/* Filter Tabs */}
            <div className="flex gap-2 mb-6">
              <Button
                variant={filter === 'ACTIVE' ? 'default' : 'outline'}
                onClick={() => setFilter('ACTIVE')}
              >
                Active
              </Button>
              <Button
                variant={filter === 'ALL' ? 'default' : 'outline'}
                onClick={() => setFilter('ALL')}
              >
                All
              </Button>
              <Button
                variant={filter === 'GRADUATED' ? 'default' : 'outline'}
                onClick={() => setFilter('GRADUATED')}
              >
                Graduated
              </Button>
              <Button
                variant={filter === 'WITHDRAWN' ? 'default' : 'outline'}
                onClick={() => setFilter('WITHDRAWN')}
              >
                Withdrawn
              </Button>
            </div>

            {/* Error Message */}
            {error && (
              <div className="bg-red-100 text-red-700 p-4 rounded mb-4">
                {error}
              </div>
            )}

            {/* Loading State */}
            {loading && (
              <div className="text-center py-8 text-gray-500">
                Loading students...
              </div>
            )}

            {/* Students Table */}
            {!loading && students.length === 0 && (
              <div className="text-center py-8 text-gray-500">
                No students found
              </div>
            )}

            {!loading && students.length > 0 && (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Student Number</TableHead>
                    <TableHead>Name</TableHead>
                    <TableHead>Grade</TableHead>
                    <TableHead>Guardian</TableHead>
                    <TableHead>Contact</TableHead>
                    <TableHead>Monthly Fee</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {students.map((student) => (
                    <TableRow key={student.id}>
                      <TableCell className="font-mono font-semibold">
                        {student.studentNumber}
                      </TableCell>
                      <TableCell>
                        {student.firstName} {student.lastName}
                      </TableCell>
                      <TableCell>{student.gradeClass}</TableCell>
                      <TableCell>{student.parentName || student.guardianName || 'N/A'}</TableCell>
                      <TableCell>{student.parentPhone || student.guardianContact || 'N/A'}</TableCell>
                      <TableCell>{formatCurrency(student.monthlyFee)}</TableCell>
                      <TableCell>{getStatusBadge(student.status)}</TableCell>
                      <TableCell>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => navigate(`/students/${student.id}`)}
                        >
                          View Details
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}

            {/* Summary */}
            {!loading && students.length > 0 && (
              <div className="mt-4 text-sm text-gray-600">
                Showing {students.length} student{students.length !== 1 ? 's' : ''}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
