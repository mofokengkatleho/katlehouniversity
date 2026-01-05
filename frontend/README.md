# ECD Payment Reconciliation System - Frontend

React + TypeScript frontend application for the ECD Payment Reconciliation System.

## Technology Stack

- **Framework**: React 18
- **Language**: TypeScript
- **Build Tool**: Vite
- **Styling**: TailwindCSS
- **Routing**: React Router v6
- **HTTP Client**: Axios
- **State Management**: React Context API

## Project Structure

```
src/
├── components/           # Reusable UI components
├── contexts/
│   └── AuthContext.tsx  # Authentication state management
├── pages/
│   ├── Login.tsx        # Login page
│   └── Dashboard.tsx    # Main dashboard
├── services/
│   └── api.ts           # API client & endpoints
├── types/
│   └── index.ts         # TypeScript type definitions
├── utils/               # Utility functions
├── App.tsx              # Main app component with routing
├── main.tsx             # Application entry point
└── index.css            # Global styles & Tailwind imports
```

## Getting Started

### Prerequisites

- Node.js 20+
- npm or yarn

### Installation

1. Install dependencies:
   ```bash
   npm install
   ```

2. Create `.env` file (or use existing one):
   ```bash
   VITE_API_URL=http://localhost:8080/api
   ```

3. Start development server:
   ```bash
   npm run dev
   ```

4. Open browser to `http://localhost:5173`

## Available Scripts

### Development
```bash
npm run dev          # Start development server with hot reload
```

### Build
```bash
npm run build        # Build for production (output in dist/)
npm run preview      # Preview production build locally
```

### Linting
```bash
npm run lint         # Run ESLint
```

## Features Implemented

### Authentication
- JWT-based authentication
- Persistent login (localStorage)
- Protected routes
- Auto-redirect on session expiry

### Dashboard
- Monthly payment overview
- Summary cards (total children, paid, owing, collected)
- Paid children list with payment details
- Owing children list with outstanding amounts
- Real-time data from backend API

### API Integration
- Centralized API client with axios
- Automatic JWT token attachment
- Request/response interceptors
- Error handling

## Environment Variables

Create `.env` file in root directory:

```env
# Backend API URL
VITE_API_URL=http://localhost:8080/api

# Optional: Enable debug logging
VITE_DEBUG=true
```

Access in code:
```typescript
const apiUrl = import.meta.env.VITE_API_URL;
```

## Building for Production

### Build
```bash
npm run build
```

Output in `dist/` directory.

### Preview Build
```bash
npm run preview
```

### Deployment Options

#### Static Hosting (Vercel, Netlify, etc.)
1. Build: `npm run build`
2. Deploy `dist/` folder
3. Configure environment variables in hosting platform
4. Set rewrites for SPA routing

#### Nginx
```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /var/www/ecd-frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080/api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## Troubleshooting

### Vite not starting
- Check Node version: `node -v` (should be 20+)
- Delete `node_modules` and reinstall: `rm -rf node_modules && npm install`
- Check port 5173 is available

### API calls failing
- Verify backend is running on port 8080
- Check `.env` file exists with correct `VITE_API_URL`
- Check browser console for CORS errors
- Verify JWT token in localStorage

### Build errors
- Clear Vite cache: `rm -rf node_modules/.vite`
- Check TypeScript errors: `npx tsc --noEmit`
- Ensure all dependencies are installed

### Styling not working
- Verify Tailwind is configured correctly
- Check `tailwind.config.js` content paths
- Ensure `index.css` imports Tailwind directives

## License

Proprietary - Katlego University
