import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'
import { Button } from '@/components/ui/button'

export default function Navigation() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <nav className="bg-white shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16 items-center">
          <div className="flex items-center">
            <h1
              className="text-2xl font-bold text-gray-900 cursor-pointer hover:text-blue-600"
              onClick={() => navigate('/dashboard')}
            >
              ECD Payment System
            </h1>
          </div>
          <div className="flex gap-2">
            <Button
              variant="ghost"
              onClick={() => navigate('/dashboard')}
              className="hover:bg-gray-100"
            >
              Dashboard
            </Button>
            <Button
              variant="ghost"
              onClick={() => navigate('/register')}
              className="hover:bg-gray-100"
            >
              Register Student
            </Button>
            <Button
              variant="ghost"
              onClick={() => navigate('/upload')}
              className="hover:bg-gray-100"
            >
              Upload Statement
            </Button>
            <Button
              variant="ghost"
              onClick={() => navigate('/students')}
              className="hover:bg-gray-100"
            >
              Students
            </Button>
            <Button
              variant="ghost"
              onClick={() => navigate('/transactions')}
              className="hover:bg-gray-100"
            >
              Transactions
            </Button>
          </div>
          <div className="flex items-center gap-4">
            <span className="text-sm text-gray-600">
              Welcome, {user?.username || 'Admin'}
            </span>
            <Button
              variant="outline"
              onClick={handleLogout}
              size="sm"
            >
              Logout
            </Button>
          </div>
        </div>
      </div>
    </nav>
  )
}
