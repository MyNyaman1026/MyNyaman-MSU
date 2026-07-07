import { useState } from "react";
import { useLocation } from "wouter";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ArrowLeft, Bell, AlertCircle, CheckCircle2, Info, Trash2 } from "lucide-react";

interface Notification {
  id: string;
  type: "alert" | "info" | "success" | "warning";
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
  actionUrl?: string;
}

const initialNotifications: Notification[] = [
  {
    id: "1",
    type: "alert",
    title: "Screen Time Limit Exceeded",
    message: "Sarah has exceeded her daily screen time limit by 30 minutes",
    timestamp: "2 hours ago",
    read: false,
    actionUrl: "/usage-reports",
  },
  {
    id: "2",
    type: "warning",
    title: "Blocked App Access Attempt",
    message: "Alex attempted to access YouTube (blocked app)",
    timestamp: "1 hour ago",
    read: false,
    actionUrl: "/app-blocking",
  },
  {
    id: "3",
    type: "info",
    title: "Consultant Response",
    message: "Dr. Smith has replied to your consultation request",
    timestamp: "30 minutes ago",
    read: true,
    actionUrl: "/chat",
  },
  {
    id: "4",
    type: "success",
    title: "Good Day!",
    message: "Sarah stayed within her screen time limit today",
    timestamp: "1 day ago",
    read: true,
  },
  {
    id: "5",
    type: "alert",
    title: "Bedtime Violation",
    message: "Alex is still using device during bedtime (10:30 PM - 7:00 AM)",
    timestamp: "2 days ago",
    read: true,
  },
];

const getIcon = (type: string) => {
  switch (type) {
    case "alert":
      return <AlertCircle className="w-5 h-5 text-red-600" />;
    case "warning":
      return <AlertCircle className="w-5 h-5 text-yellow-600" />;
    case "success":
      return <CheckCircle2 className="w-5 h-5 text-green-600" />;
    case "info":
      return <Info className="w-5 h-5 text-blue-600" />;
    default:
      return <Bell className="w-5 h-5 text-slate-600" />;
  }
};

const getBackgroundColor = (type: string) => {
  switch (type) {
    case "alert":
      return "bg-red-50 border-red-200";
    case "warning":
      return "bg-yellow-50 border-yellow-200";
    case "success":
      return "bg-green-50 border-green-200";
    case "info":
      return "bg-blue-50 border-blue-200";
    default:
      return "bg-slate-50 border-slate-200";
  }
};

export default function NotificationCenter() {
  const [, navigate] = useLocation();
  const [notifications, setNotifications] = useState(initialNotifications);
  const [filter, setFilter] = useState<"all" | "unread">("all");

  const filteredNotifications = notifications.filter((n) =>
    filter === "unread" ? !n.read : true
  );

  const unreadCount = notifications.filter((n) => !n.read).length;

  const handleMarkAsRead = (id: string) => {
    setNotifications(
      notifications.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
  };

  const handleDelete = (id: string) => {
    setNotifications(notifications.filter((n) => n.id !== id));
  };

  const handleMarkAllAsRead = () => {
    setNotifications(notifications.map((n) => ({ ...n, read: true })));
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-indigo-50 pb-8">
      {/* Header */}
      <div className="bg-white border-b border-slate-200 sticky top-0 z-40">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => navigate("/dashboard/parent")}
                className="p-2 hover:bg-slate-100 rounded-lg transition"
              >
                <ArrowLeft className="w-6 h-6 text-slate-600" />
              </button>
              <div>
                <h1 className="text-2xl font-bold text-slate-900">Notifications</h1>
                <p className="text-sm text-slate-600">{unreadCount} unread</p>
              </div>
            </div>
            {unreadCount > 0 && (
              <Button
                onClick={handleMarkAllAsRead}
                variant="outline"
                className="text-sm"
              >
                Mark all as read
              </Button>
            )}
          </div>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
        <div className="flex gap-2">
          <Button
            onClick={() => setFilter("all")}
            variant={filter === "all" ? "default" : "outline"}
            className="text-sm"
          >
            All Notifications
          </Button>
          <Button
            onClick={() => setFilter("unread")}
            variant={filter === "unread" ? "default" : "outline"}
            className="text-sm"
          >
            Unread ({unreadCount})
          </Button>
        </div>
      </div>

      {/* Notifications List */}
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {filteredNotifications.length === 0 ? (
          <Card className="p-12 text-center">
            <Bell className="w-12 h-12 text-slate-300 mx-auto mb-4" />
            <p className="text-slate-600">No notifications</p>
          </Card>
        ) : (
          <div className="space-y-3">
            {filteredNotifications.map((notification) => (
              <Card
                key={notification.id}
                className={`p-4 border-l-4 transition-all ${getBackgroundColor(
                  notification.type
                )} ${!notification.read ? "shadow-md" : ""}`}
              >
                <div className="flex items-start gap-4">
                  <div className="flex-shrink-0 mt-1">{getIcon(notification.type)}</div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <h3
                          className={`font-semibold ${
                            !notification.read
                              ? "text-slate-900"
                              : "text-slate-700"
                          }`}
                        >
                          {notification.title}
                        </h3>
                        <p className="text-sm text-slate-600 mt-1">
                          {notification.message}
                        </p>
                        <p className="text-xs text-slate-500 mt-2">
                          {notification.timestamp}
                        </p>
                      </div>
                      {!notification.read && (
                        <div className="w-2 h-2 bg-blue-600 rounded-full flex-shrink-0 mt-2" />
                      )}
                    </div>

                    <div className="flex gap-2 mt-3">
                      {notification.actionUrl && (
                        <Button
                          onClick={() => navigate(notification.actionUrl!)}
                          size="sm"
                          className="text-xs"
                        >
                          View
                        </Button>
                      )}
                      {!notification.read && (
                        <Button
                          onClick={() => handleMarkAsRead(notification.id)}
                          size="sm"
                          variant="outline"
                          className="text-xs"
                        >
                          Mark as read
                        </Button>
                      )}
                      <Button
                        onClick={() => handleDelete(notification.id)}
                        size="sm"
                        variant="ghost"
                        className="text-xs text-red-600 hover:text-red-700"
                      >
                        <Trash2 className="w-3 h-3" />
                      </Button>
                    </div>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
