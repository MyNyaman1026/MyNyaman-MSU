# MyNyaman Deployment Guide

## Prerequisites

Before deploying MyNyaman, ensure you have:

- Node.js 22.13.0 or higher
- MySQL 8.0+ or TiDB database
- Manus OAuth credentials (VITE_APP_ID, OAUTH_SERVER_URL)
- Environment variables configured

## Environment Variables

Create a `.env` file in the project root with the following variables:

```bash
# Database
DATABASE_URL=mysql://user:password@host:port/database

# OAuth
VITE_APP_ID=your_app_id
OAUTH_SERVER_URL=https://api.manus.im
VITE_OAUTH_PORTAL_URL=https://manus.im/login

# JWT
JWT_SECRET=your_jwt_secret_key

# Owner Info
OWNER_OPEN_ID=your_owner_open_id
OWNER_NAME=Your Name

# Manus APIs
BUILT_IN_FORGE_API_URL=https://api.manus.im
BUILT_IN_FORGE_API_KEY=your_api_key
VITE_FRONTEND_FORGE_API_URL=https://api.manus.im
VITE_FRONTEND_FORGE_API_KEY=your_frontend_api_key

# Analytics (optional)
VITE_ANALYTICS_ENDPOINT=https://analytics.example.com
VITE_ANALYTICS_WEBSITE_ID=your_website_id

# App Configuration
VITE_APP_TITLE=MyNyaman
VITE_APP_LOGO=https://cdn.example.com/logo.png
```

## Database Setup

### 1. Create Database

```bash
mysql -u root -p
CREATE DATABASE mynyaman;
USE mynyaman;
```

### 2. Apply Migrations

The database migrations are located in `drizzle/` directory. Execute the migration SQL files in order:

```bash
# First migration - creates initial users table
mysql -u user -p database < drizzle/0000_round_doorman.sql

# Second migration - creates all feature tables
mysql -u user -p database < drizzle/0001_little_songbird.sql
```

Alternatively, use Drizzle Kit to manage migrations:

```bash
pnpm db:push
```

### 3. Verify Database

Verify all tables are created:

```bash
mysql -u user -p database -e "SHOW TABLES;"
```

Expected tables:
- users
- parent_child_relationships
- usage_reports
- app_time_limits
- blocked_apps
- chat_messages
- conversations
- alerts
- consultant_sessions
- parenting_advice

## Build & Deployment

### Development

```bash
# Install dependencies
pnpm install

# Start development server
pnpm dev
```

The dev server will run on `http://localhost:3000`

### Production Build

```bash
# Build the application
pnpm build

# Start production server
pnpm start
```

The production server will run on `http://localhost:3000` (or configured port)

## Testing

Run the test suite to verify everything is working:

```bash
# Run all tests
pnpm test

# Run tests in watch mode
pnpm test --watch
```

Expected output: 23+ tests passing

## Deployment Checklist

- [ ] Environment variables configured
- [ ] Database created and migrations applied
- [ ] OAuth credentials set up
- [ ] All tests passing (`pnpm test`)
- [ ] Build successful (`pnpm build`)
- [ ] Start script works (`pnpm start`)
- [ ] Landing page loads correctly
- [ ] OAuth login flow works
- [ ] Role selection works
- [ ] Dashboards load for all roles

## Production Considerations

### Security

1. **HTTPS Only**: Ensure all traffic is encrypted with HTTPS
2. **Environment Variables**: Never commit `.env` files; use secure secret management
3. **Database**: Use strong passwords and restrict database access
4. **CORS**: Configure CORS properly for your domain
5. **Rate Limiting**: Implement rate limiting on API endpoints
6. **Input Validation**: All inputs are validated via Zod schemas

### Performance

1. **Database Indexing**: Ensure indexes are created on frequently queried columns
2. **Caching**: Implement caching for frequently accessed data
3. **CDN**: Serve static assets through a CDN
4. **Monitoring**: Set up error tracking and performance monitoring

### Monitoring

1. **Error Tracking**: Use Sentry or similar service
2. **Logs**: Configure centralized logging
3. **Uptime Monitoring**: Monitor application availability
4. **Database Monitoring**: Monitor database performance and backups

## Troubleshooting

### Database Connection Error

```
Error: Unknown column 'profilePicture' in 'field list'
```

**Solution**: Run migrations to create missing columns:
```bash
pnpm db:push
```

### OAuth Callback Error

**Solution**: Verify OAuth credentials and redirect URI:
- Check VITE_APP_ID is correct
- Verify OAUTH_SERVER_URL is accessible
- Ensure redirect URI matches configured OAuth app

### Port Already in Use

**Solution**: Change the port or kill the process using it:
```bash
# Change port
PORT=3001 pnpm start

# Or kill the process
lsof -ti:3000 | xargs kill -9
```

## Scaling Considerations

### Horizontal Scaling

1. Use a load balancer (nginx, HAProxy)
2. Run multiple instances of the application
3. Use a shared database (not local SQLite)
4. Use Redis for session management

### Database Scaling

1. Implement database replication
2. Use read replicas for reporting queries
3. Archive old usage reports
4. Implement database sharding if needed

## Backup & Recovery

### Database Backups

```bash
# Create backup
mysqldump -u user -p database > backup.sql

# Restore backup
mysql -u user -p database < backup.sql
```

### Regular Backup Schedule

- Daily automated backups
- Weekly full backups
- Monthly archive backups
- Test restore procedures monthly

## Support & Maintenance

- Monitor application logs regularly
- Keep dependencies updated
- Review security updates
- Test disaster recovery procedures
- Document any customizations

## Additional Resources

- [Manus Documentation](https://manus.im/docs)
- [Express.js Documentation](https://expressjs.com/)
- [tRPC Documentation](https://trpc.io/)
- [Drizzle ORM Documentation](https://orm.drizzle.team/)
- [React Documentation](https://react.dev/)
