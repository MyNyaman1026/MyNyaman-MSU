# MyNyaman - Digital Wellness Platform Features

## Overview

MyNyaman is a comprehensive parental control and digital wellness platform designed to help families build healthy device habits through intelligent monitoring, time management, and expert guidance.

## Core Features

### 1. **Role-Based Access Control**
- **Three User Roles**: Parent, Child, Consultant
- **Permanent Role Selection**: Users choose their role during signup and cannot change it
- **Role-Specific Dashboards**: Each role has a customized interface with relevant features
- **Secure Authorization**: Backend procedures enforce role-based access controls

### 2. **Parent Dashboard**
The parent dashboard provides comprehensive tools for managing children's digital wellness:

- **Child Management**
  - Link multiple children to your account via email
  - Customize nicknames for each child
  - View all linked children and their status
  - Remove children from monitoring

- **Usage Reports**
  - View detailed analytics for each child's device usage
  - Track total screen time and daily averages
  - Analyze app usage breakdown with pie charts
  - Filter reports by time period (7, 30, or 90 days)
  - Download and export usage reports

- **Daily Time Limits**
  - Set maximum daily usage time for specific apps
  - Create custom limits or use preset options
  - Enable/disable limits without deletion
  - Quick preset buttons (15 min, 30 min, 1 hour, etc.)

- **App Blocking**
  - Block or unblock specific applications
  - Manage blocked app list with easy toggle controls
  - Quick-add popular apps (TikTok, Instagram, YouTube, etc.)
  - View all blocked apps for each child

- **Alerts Center**
  - Receive notifications when children exceed time limits
  - Get alerts for blocked app access attempts
  - Receive notifications when consultants respond
  - Filter alerts (all/unread)
  - Mark alerts as read or delete them

- **Consultant Chat**
  - Connect with digital wellness experts
  - Get personalized parenting advice
  - Discuss concerns about your child's device usage
  - Access expert guidance on digital wellness

### 3. **Child Dashboard**
The child dashboard provides transparency and engagement:

- **Usage Statistics**
  - View your own screen time and daily usage
  - See app usage breakdown
  - Track time spent on each app
  - Understand your digital wellness metrics

- **Active Limits**
  - View daily time limits set by parents
  - See which apps are blocked
  - Understand restrictions and why they exist
  - Monitor remaining time for limited apps

- **Consultant Chat**
  - Message consultants for support and advice
  - Ask questions about digital wellness
  - Receive guidance on healthy device habits
  - Access resources and recommendations

- **Profile Management**
  - Edit personal information
  - Add a bio or description
  - Manage account settings
  - View account details

### 4. **Consultant Dashboard**
The consultant dashboard enables expert support:

- **Active Consultations**
  - View all ongoing consultations with parents
  - Track consultation status (active/pending/closed)
  - Manage multiple consultations simultaneously
  - Take notes on each consultation

- **Parenting Advice Library**
  - Access curated and AI-generated parenting advice
  - Browse advice by category:
    - Screen Time Management
    - Digital Wellness
    - Communication
    - Technology Balance
  - Share advice with parents
  - Bookmark important articles

- **Consultant Chat**
  - Respond to parent inquiries
  - Provide personalized recommendations
  - Share resources and guidance
  - Maintain conversation history

### 5. **Real-Time Chat System**
- **Multi-Party Conversations**
  - Parent-to-Consultant chat
  - Parent-to-Child chat
  - Child-to-Consultant chat
  - Message history and persistence

- **Message Management**
  - Send and receive text messages
  - Mark messages as read
  - View conversation history
  - Real-time message notifications

### 6. **Usage Analytics & Reporting**
- **Comprehensive Tracking**
  - Screen time monitoring
  - App-specific usage statistics
  - Daily, weekly, and monthly reports
  - Historical data visualization

- **Data Visualization**
  - Line charts for trend analysis
  - Bar charts for comparison
  - Pie charts for app breakdown
  - Interactive charts with hover details

### 7. **App Management**
- **App Blocking**
  - Block apps by package name
  - Maintain blocked app list
  - Quick toggle blocking status
  - Popular app suggestions

- **Time Limits**
  - Set per-app daily limits
  - Enable/disable limits flexibly
  - Preset time options
  - Limit management interface

### 8. **Alerts & Notifications**
- **Alert Types**
  - Time Limit Exceeded: When child exceeds daily limit
  - Blocked App Attempt: When child tries to access blocked app
  - Consultant Response: When consultant replies to inquiry
  - System Alerts: General platform notifications

- **Alert Management**
  - View all alerts in dedicated center
  - Filter by read/unread status
  - Mark individual or all alerts as read
  - Delete alerts
  - Unread count tracking

### 9. **AI-Powered Parenting Advice**
- **Advice Categories**
  - Screen Time Management
  - Digital Wellness
  - Communication
  - Technology Balance

- **Advice Features**
  - Curated expert advice
  - AI-generated recommendations
  - Categorized for easy browsing
  - Shareable with parents
  - Bookmarking functionality

### 10. **Profile Management**
- **User Profiles**
  - Edit name and email
  - Add bio/description
  - Profile picture support (URL-based)
  - Account settings management
  - Role-specific profile views

## Technical Architecture

### Backend
- **Framework**: Express.js + tRPC
- **Database**: MySQL/TiDB with Drizzle ORM
- **Authentication**: Manus OAuth
- **API**: Type-safe tRPC procedures

### Frontend
- **Framework**: React 19
- **Styling**: Tailwind CSS 4 + shadcn/ui
- **State Management**: TanStack Query (React Query)
- **Routing**: Wouter
- **Charts**: Recharts

### Database Schema
- **users**: Core user table with role-based access
- **parent_child_relationships**: Link parents to children
- **usage_reports**: Track screen time and app usage
- **app_time_limits**: Store app-specific time limits
- **blocked_apps**: Manage blocked applications
- **chat_messages**: Store conversation messages
- **conversations**: Manage multi-party conversations
- **alerts**: Store parent notifications
- **consultant_sessions**: Track consultant consultations
- **parenting_advice**: Store advice articles

## Security Features

- **Role-Based Access Control**: Procedures enforce user role requirements
- **Parent-Child Relationship Verification**: Ensure parents can only manage their own children
- **Ownership Checks**: Users can only access their own data
- **Secure Authentication**: Manus OAuth integration
- **Data Encryption**: Sensitive data handled securely

## User Workflows

### Parent Workflow
1. Sign up and select "Parent" role
2. Add children by email address
3. Set time limits and blocked apps
4. Monitor usage reports
5. Receive and manage alerts
6. Chat with consultants for advice
7. Manage profile and settings

### Child Workflow
1. Sign up and select "Child" role
2. View personal usage statistics
3. See active limits and blocked apps
4. Chat with parents or consultants
5. Understand digital wellness recommendations
6. Manage profile

### Consultant Workflow
1. Sign up and select "Consultant" role
2. Browse parenting advice library
3. Respond to parent consultations
4. Provide personalized guidance
5. Share resources and recommendations
6. Manage consultation sessions

## Design Philosophy

- **Elegant & Intuitive**: Clean, modern interface with intuitive navigation
- **Role-Specific**: Each role sees only relevant features
- **Responsive**: Works seamlessly on desktop, tablet, and mobile
- **Accessible**: WCAG compliant with proper contrast and keyboard navigation
- **Fast**: Optimized performance with lazy loading and caching
- **Secure**: Privacy-first design with data protection

## Future Enhancements

- Mobile app integration with device monitoring
- Advanced AI recommendations
- Family group management
- Scheduled downtime automation
- Integration with popular apps
- Advanced analytics and insights
- Multi-language support
- Two-factor authentication
- Family wellness reports
