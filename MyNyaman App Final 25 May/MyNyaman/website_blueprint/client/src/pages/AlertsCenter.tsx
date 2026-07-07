import { useAuth } from "@/_core/hooks/useAuth";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import DashboardLayout from "@/components/DashboardLayout";
import { trpc } from "@/lib/trpc";
import { AlertCircle, CheckCircle, Trash2, MailOpen, Mail, Loader2 } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

interface Alert {
  id: number;
  alertType: "limit_exceeded" | "blocked_app_attempt" | "consultant_response" | "system";
  title: string;
  message: string;
  isRead: boolean;
  createdAt: Date;
  childId: number;
}

export default function AlertsCenter() {
  const { user } = useAuth();
  const [filter, setFilter] = useState<"all" | "unread">("all");

  const alertsQuery = trpc.alerts.getAlerts.useQuery({
    limit: 100,
    unreadOnly: filter === "unread",
  });

  const markAsReadMutation = trpc.alerts.markAsRead.useMutation({
    onSuccess: () => {
      alertsQuery.refetch();
      toast.success("Alert marked as read");
    },
  });

  const markAllAsReadMutation = trpc.alerts.markAllAsRead.useMutation({
    onSuccess: () => {
      alertsQuery.refetch();
      toast.success("All alerts marked as read");
    },
  });

  const deleteAlertMutation = trpc.alerts.deleteAlert.useMutation({
    onSuccess: () => {
      alertsQuery.refetch();
      toast.success("Alert deleted");
    },
  });

  if (!user || user.role !== "parent") {
    return null;
  }

  const alerts = (alertsQuery.data || []) as Alert[];
  const unreadCount = alerts.filter(a => !a.isRead).length;

  const getAlertIcon = (type: string) => {
    switch (type) {
      case "limit_exceeded":
        return <AlertCircle className="w-5 h-5 text-red-600" />;
      case "blocked_app_attempt":
        return <AlertCircle className="w-5 h-5 text-orange-600" />;
      case "consultant_response":
        return <Mail className="w-5 h-5 text-blue-600" />;
      default:
        return <AlertCircle className="w-5 h-5 text-slate-600" />;
    }
  };

  const getAlertColor = (type: string) => {
    switch (type) {
      case "limit_exceeded":
        return "bg-red-50 border-l-red-600";
      case "blocked_app_attempt":
        return "bg-orange-50 border-l-orange-600";
      case "consultant_response":
        return "bg-blue-50 border-l-blue-600";
      default:
        return "bg-slate-50 border-l-slate-600";
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-slate-900">Alerts Center</h1>
            <p className="text-slate-600 mt-1">
              {unreadCount > 0 ? `You have ${unreadCount} unread alert${unreadCount !== 1 ? "s" : ""}` : "All alerts read"}
            </p>
          </div>
          {unreadCount > 0 && (
            <Button
              onClick={() => markAllAsReadMutation.mutate()}
              disabled={markAllAsReadMutation.isPending}
              variant="outline"
            >
              {markAllAsReadMutation.isPending ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  Marking...
                </>
              ) : (
                <>
                  <MailOpen className="w-4 h-4 mr-2" />
                  Mark All as Read
                </>
              )}
            </Button>
          )}
        </div>

        {/* Filter Tabs */}
        <div className="flex gap-2">
          <Button
            variant={filter === "all" ? "default" : "outline"}
            onClick={() => setFilter("all")}
          >
            All Alerts
          </Button>
          <Button
            variant={filter === "unread" ? "default" : "outline"}
            onClick={() => setFilter("unread")}
          >
            Unread ({unreadCount})
          </Button>
        </div>

        {/* Alerts List */}
        <div className="space-y-3">
          {alertsQuery.isLoading ? (
            <Card className="p-8 flex items-center justify-center">
              <Loader2 className="w-6 h-6 animate-spin text-slate-400" />
            </Card>
          ) : alerts.length === 0 ? (
            <Card className="p-8 text-center">
              <CheckCircle className="w-12 h-12 text-green-600 mx-auto mb-3" />
              <p className="text-slate-900 font-medium">No alerts</p>
              <p className="text-slate-600 text-sm">You're all caught up!</p>
            </Card>
          ) : (
            alerts.map(alert => (
              <Card
                key={alert.id}
                className={`p-4 border-l-4 flex items-start justify-between hover:shadow-md transition-shadow ${getAlertColor(alert.alertType)}`}
              >
                <div className="flex items-start gap-4 flex-1">
                  <div className="mt-1">{getAlertIcon(alert.alertType)}</div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="font-semibold text-slate-900">{alert.title}</h3>
                      {!alert.isRead && (
                        <span className="inline-block w-2 h-2 bg-blue-600 rounded-full"></span>
                      )}
                    </div>
                    <p className="text-slate-600 text-sm mb-2">{alert.message}</p>
                    <p className="text-xs text-slate-500">
                      {new Date(alert.createdAt).toLocaleString()}
                    </p>
                  </div>
                </div>

                <div className="flex gap-2 ml-4">
                  {!alert.isRead && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => markAsReadMutation.mutate({ alertId: alert.id })}
                      disabled={markAsReadMutation.isPending}
                    >
                      <MailOpen className="w-4 h-4" />
                    </Button>
                  )}
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => deleteAlertMutation.mutate({ alertId: alert.id })}
                    disabled={deleteAlertMutation.isPending}
                  >
                    <Trash2 className="w-4 h-4 text-red-600" />
                  </Button>
                </div>
              </Card>
            ))
          )}
        </div>

        {/* Alert Types Legend */}
        <Card className="p-6 bg-slate-50">
          <h3 className="font-semibold text-slate-900 mb-4">Alert Types</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="flex items-start gap-3">
              <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
              <div>
                <p className="font-medium text-slate-900">Time Limit Exceeded</p>
                <p className="text-sm text-slate-600">Child exceeded daily screen time limit</p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <AlertCircle className="w-5 h-5 text-orange-600 flex-shrink-0 mt-0.5" />
              <div>
                <p className="font-medium text-slate-900">Blocked App Attempt</p>
                <p className="text-sm text-slate-600">Child tried to access a blocked app</p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <Mail className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
              <div>
                <p className="font-medium text-slate-900">Consultant Response</p>
                <p className="text-sm text-slate-600">Consultant replied to your inquiry</p>
              </div>
            </div>
          </div>
        </Card>
      </div>
    </DashboardLayout>
  );
}
