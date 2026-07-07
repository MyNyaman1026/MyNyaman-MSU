# MyNyaman - Digital Wellness Platform

A comprehensive parental control and digital wellness platform built with React, Express, tRPC, and MySQL. MyNyaman helps families build healthy device habits through intelligent monitoring, time management, and expert guidance.

## 🎯 Overview

MyNyaman is a modern web application designed to help parents manage their children's digital wellness. The platform provides three distinct user roles:

- **Parents**: Monitor children's device usage, set time limits, block apps, and receive alerts
- **Children**: View their own usage statistics and understand digital wellness guidelines
- **Consultants**: Provide expert guidance and parenting advice

## ✨ Key Features

### Parent Dashboard
- Child management and monitoring
- Usage analytics and reports
- App blocking and time limit controls
- Alert center for notifications
- Consultant chat for expert advice

### Child Dashboard
- Personal usage statistics
- Active time limits and blocked apps
- Consultant chat access
- Profile management

### Consultant Dashboard
- Active consultations management
- Parenting advice library
- AI-powered recommendation generation
- Parent consultation chat

### Core Capabilities
- Real-time chat system between parents, children, and consultants
- Comprehensive usage reporting with interactive charts
- App-specific time limits and blocking
- Multi-type alert system (time limit exceeded, blocked app attempts, consultant responses)
- AI-powered parenting advice generation
- Role-based access control with permanent role selection

## 🏗️ Architecture

### Tech Stack

**Frontend:**
- React 19
- Tailwind CSS 4
- shadcn/ui components
- TanStack Query (React Query)
- Wouter routing
- Recharts for data visualization

**Backend:**
- Express.js
- tRPC for type-safe APIs
- Drizzle ORM
- MySQL/TiDB database
- Manus OAuth integration

**Testing:**
- Vitest for unit tests
- 23+ tests covering all routers

### Project Structure

```
mynyaman-web/
├── client/                 # React frontend
│   ├── src/
│   │   ├── pages/         # Page components
│   │   ├── components/    # Reusable components
│   │   ├── contexts/      # React contexts
│   │   ├── hooks/         # Custom hooks
│   │   ├── lib/           # Utilities
│   │   └── App.tsx        # Main app component
│   └── index.html
├── server/                # Express backend
│   ├── routers/           # tRPC routers
│   ├── db.ts              # Database helpers
│   ├── routers.ts         # Main router
│   └── _core/             # Core infrastructure
├── drizzle/               # Database schema & migrations
├── shared/                # Shared types & constants
├── storage/               # S3 storage helpers
└── package.json
```

## 🚀 Getting Started

### Prerequisites

- Node.js 22.13.0+
- MySQL 8.0+ or TiDB
- Manus OAuth credentials

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd mynyaman-web
   ```

2. **Install dependencies**
   ```bash
   pnpm install
   ```

3. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

4. **Set up database**
   ```bash
   # Apply migrations
   pnpm db:push
   ```

5. **Start development server**
   ```bash
   pnpm dev
   ```

   The app will be available at `http://localhost:3000`

## 📖 Documentation

- **[FEATURES.md](./FEATURES.md)** - Comprehensive feature documentation
- **[DEPLOYMENT.md](./DEPLOYMENT.md)** - Deployment guide and production setup
- **[todo.md](./todo.md)** - Development progress and task tracking

## 🧪 Testing

Run the test suite:

```bash
# Run all tests
pnpm test

# Run tests in watch mode
pnpm test --watch
```

**Test Coverage:**
- Authentication and authorization
- Role-based access control
- Profile management
- Chat system
- Usage tracking and analytics
- Alert management
- Parent-child relationships

## 🔐 Security Features

- **Role-Based Access Control**: Procedures enforce user role requirements
- **Permanent Role Selection**: Users cannot change roles after initial selection
- **Parent-Child Verification**: Ensure parents can only manage their own children
- **Ownership Checks**: Users can only access their own data
- **Secure Authentication**: Manus OAuth integration
- **Input Validation**: All inputs validated via Zod schemas

## 🎨 Design

MyNyaman features an elegant, modern design with:

- **OKLCH Color Palette**: Perceptually uniform colors for better accessibility
- **Responsive Layout**: Works seamlessly on desktop, tablet, and mobile
- **Consistent Components**: Built on shadcn/ui for consistency
- **Smooth Animations**: Subtle transitions and hover effects
- **Accessible Navigation**: WCAG compliant with keyboard support

## 📊 Database Schema

The application uses 10 tables to manage all functionality:

| Table | Purpose |
|-------|---------|
| users | Core user data with roles |
| parent_child_relationships | Links parents to children |
| usage_reports | Screen time and app usage tracking |
| app_time_limits | Per-app daily time limits |
| blocked_apps | App blocking management |
| chat_messages | Message storage |
| conversations | Multi-party chat management |
| alerts | Parent notifications |
| consultant_sessions | Consultation tracking |
| parenting_advice | Advice articles |

## 🔄 User Workflows

### Parent Workflow
1. Sign up and select "Parent" role
2. Add children by email
3. Set time limits and blocked apps
4. Monitor usage reports
5. Receive and manage alerts
6. Chat with consultants

### Child Workflow
1. Sign up and select "Child" role
2. View personal usage statistics
3. See active limits and blocked apps
4. Chat with parents or consultants
5. Understand digital wellness

### Consultant Workflow
1. Sign up and select "Consultant" role
2. Browse parenting advice library
3. Respond to parent consultations
4. Provide personalized guidance
5. Generate AI recommendations

## 🛠️ Development

### Available Scripts

```bash
# Development
pnpm dev              # Start dev server with hot reload
pnpm build           # Build for production
pnpm start           # Start production server
pnpm test            # Run tests
pnpm format          # Format code with Prettier
pnpm check           # Type check with TypeScript
```

### Database Management

```bash
# Generate migrations
pnpm drizzle-kit generate

# Apply migrations
pnpm db:push

# View database
pnpm drizzle-kit studio
```

## 📈 Performance

- Optimized React components with proper memoization
- Efficient database queries with proper indexing
- Lazy loading for routes and components
- CDN-ready asset structure
- Responsive images and media

## 🔗 API Documentation

The application uses tRPC for type-safe APIs. All procedures are defined in `server/routers/`:

- **profile**: User profile management
- **chat**: Messaging system
- **usage**: Usage tracking and app management
- **alerts**: Notification system
- **children**: Parent-child relationship management
- **advice**: Parenting advice and recommendations
- **auth**: Authentication and authorization

## 🐛 Troubleshooting

### Database Connection Error
```
Error: Unknown column 'profilePicture'
```
**Solution**: Run `pnpm db:push` to apply migrations

### OAuth Callback Error
**Solution**: Verify OAuth credentials and redirect URI configuration

### Port Already in Use
**Solution**: Change port with `PORT=3001 pnpm dev`

## 📝 License

MIT License - See LICENSE file for details

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Create a feature branch
2. Make your changes
3. Write/update tests
4. Submit a pull request

## 📞 Support

For support and questions:
- Check [FEATURES.md](./FEATURES.md) for feature documentation
- Review [DEPLOYMENT.md](./DEPLOYMENT.md) for deployment help
- Check test files for usage examples
- Open an issue for bugs

## 🎓 Learning Resources

- [React Documentation](https://react.dev/)
- [tRPC Documentation](https://trpc.io/)
- [Drizzle ORM](https://orm.drizzle.team/)
- [Express.js Guide](https://expressjs.com/)
- [Tailwind CSS](https://tailwindcss.com/)

---

**MyNyaman** - Building healthy digital habits for modern families 🏡
