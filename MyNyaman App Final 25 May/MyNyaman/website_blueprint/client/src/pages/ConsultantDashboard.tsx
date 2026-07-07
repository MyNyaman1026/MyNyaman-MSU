import { useState, useEffect } from "react";
import { useLocation } from "wouter";
import { useAuth } from "@/_core/hooks/useAuth";
import Sidebar from "@/components/Sidebar";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { MessageSquare, Users, CheckCircle2, Clock, Bell, Loader2 } from "lucide-react";

interface Consultation {
  id: string;
  parentName: string;
  childName: string;
  status: "pending" | "active" | "completed";
  lastMessage: string;
  timestamp: string;
  unreadCount: number;
}

const initialConsultations: Consultation[] = [
  {
    id: "1",
    parentName: "John Parent",
    childName: "Sarah (12)",
    status: "active",
    lastMessage: "Thank you for the advice!",
    timestamp: "2 hours ago",
    unreadCount: 0,
  },
  {
    id: "2",
    parentName: "Jane Smith",
    childName: "Alex (10)",
    status: "pending",
    lastMessage: "Need help with screen time limits",
    timestamp: "30 minutes ago",
    unreadCount: 1,
  },
  {
    id: "3",
    parentName: "Mike Johnson",
    childName: "Tom (14)",
    status: "completed",
    lastMessage: "Consultation completed successfully",
    timestamp: "1 day ago",
    unreadCount: 0,
  },
];

export default function ConsultantDashboard() {
  const { user, loading } = useAuth();
  const [, navigate] = useLocation();
  const [consultations, setConsultations] = useState(initialConsultations);
  const [selectedConsultation, setSelectedConsultation] = useState<string | null>(null);

  useEffect(() => {
    if (!loading && !user) {
      navigate("/signin");
    }
  }, [user, navigate, loading]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-purple-50 to-pink-100">
        <Loader2 className="w-8 h-8 animate-spin text-purple-600" />
      </div>
    );
  }

  const stats = {
    active: consultations.filter((c) => c.status === "active").length,
    pending: consultations.filter((c) => c.status === "pending").length,
    completed: consultations.filter((c) => c.status === "completed").length,
    totalUnread: consultations.reduce((sum, c) => sum + c.unreadCount, 0),
  };

  const handleAcceptConsultation = (id: string) => {
    setConsultations(
      consultations.map((c) =>
        c.id === id ? { ...c, status: "active" as const, unreadCount: 0 } : c
      )
    );
  };

  const handleCompleteConsultation = (id: string) => {
    setConsultations(
      consultations.map((c) =>
        c.id === id ? { ...c, status: "completed" as const } : c
      )
    );
  };

  return (
    <div className="flex min-h-screen bg-gradient-to-br from-purple-50 via-white to-indigo-50">
      {/* Sidebar */}
      <Sidebar role="consultant" />

      {/* Main Content */}
      <main className="flex-1 md:ml-64 pb-24">
        {/* Header */}
        <div className="bg-white border-b border-slate-200 sticky top-0 z-30">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 md:py-6">
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-2xl md:text-3xl font-bold text-slate-900">
                  Welcome, {user?.email?.split("@")[0] || "Consultant"}!
                </h1>
                <p className="text-slate-600 mt-1">
                  Manage consultations and provide guidance
                </p>
              </div>
              <button className="p-2 hover:bg-slate-100 rounded-lg transition relative">
                <Bell className="w-6 h-6 text-slate-600" />
                {stats.totalUnread > 0 && (
                  <span className="absolute top-1 right-1 bg-red-600 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
                    {stats.totalUnread}
                  </span>
                )}
              </button>
            </div>
          </div>
        </div>

        {/* Stats */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
            <Card className="p-6 bg-gradient-to-br from-blue-50 to-blue-100">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-slate-600 text-sm">Active Consultations</p>
                  <p className="text-3xl font-bold text-blue-600">{stats.active}</p>
                </div>
                <MessageSquare className="w-8 h-8 text-blue-600 opacity-20" />
              </div>
            </Card>

            <Card className="p-6 bg-gradient-to-br from-yellow-50 to-yellow-100">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-slate-600 text-sm">Pending Requests</p>
                  <p className="text-3xl font-bold text-yellow-600">{stats.pending}</p>
                </div>
                <Clock className="w-8 h-8 text-yellow-600 opacity-20" />
              </div>
            </Card>

            <Card className="p-6 bg-gradient-to-br from-green-50 to-green-100">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-slate-600 text-sm">Completed</p>
                  <p className="text-3xl font-bold text-green-600">{stats.completed}</p>
                </div>
                <CheckCircle2 className="w-8 h-8 text-green-600 opacity-20" />
              </div>
            </Card>

            <Card className="p-6 bg-gradient-to-br from-red-50 to-red-100">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-slate-600 text-sm">Unread Messages</p>
                  <p className="text-3xl font-bold text-red-600">{stats.totalUnread}</p>
                </div>
                <Bell className="w-8 h-8 text-red-600 opacity-20" />
              </div>
            </Card>
          </div>

          {/* Consultations List */}
          <div className="space-y-4">
            <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
              <Users className="w-6 h-6" /> All Consultations
            </h2>

            {consultations.map((consultation) => (
              <Card
                key={consultation.id}
                className={`p-6 cursor-pointer transition-all hover:shadow-lg ${
                  selectedConsultation === consultation.id
                    ? "border-purple-500 bg-purple-50"
                    : "border-slate-200"
                }`}
                onClick={() => setSelectedConsultation(consultation.id)}
              >
                <div className="flex items-start justify-between mb-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="font-semibold text-slate-900">{consultation.parentName}</h3>
                      <span
                        className={`px-3 py-1 rounded-full text-xs font-semibold ${
                          consultation.status === "active"
                            ? "bg-green-100 text-green-700"
                            : consultation.status === "pending"
                            ? "bg-yellow-100 text-yellow-700"
                            : "bg-slate-100 text-slate-700"
                        }`}
                      >
                        {consultation.status.charAt(0).toUpperCase() + consultation.status.slice(1)}
                      </span>
                      {consultation.unreadCount > 0 && (
                        <span className="bg-red-600 text-white text-xs rounded-full w-6 h-6 flex items-center justify-center">
                          {consultation.unreadCount}
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-slate-600">Child: {consultation.childName}</p>
                  </div>
                  <p className="text-xs text-slate-500">{consultation.timestamp}</p>
                </div>

                <p className="text-slate-700 mb-4 line-clamp-2">{consultation.lastMessage}</p>

                <div className="flex gap-2">
                  {consultation.status === "pending" && (
                    <Button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleAcceptConsultation(consultation.id);
                      }}
                      className="bg-green-600 hover:bg-green-700 text-white text-sm"
                    >
                      Accept
                    </Button>
                  )}
                  {consultation.status === "active" && (
                    <>
                      <Button
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate("/chat");
                        }}
                        className="bg-blue-600 hover:bg-blue-700 text-white text-sm flex-1"
                      >
                        <MessageSquare className="w-4 h-4 mr-2" /> Reply
                      </Button>
                      <Button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleCompleteConsultation(consultation.id);
                        }}
                        variant="outline"
                        className="text-sm"
                      >
                        Complete
                      </Button>
                    </>
                  )}
                </div>
              </Card>
            ))}
          </div>
        </div>
      </main>
    </div>
  );
}
