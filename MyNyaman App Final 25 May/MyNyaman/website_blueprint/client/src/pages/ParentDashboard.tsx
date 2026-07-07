import { useState, useEffect } from "react";
import { useLocation } from "wouter";
import { useAuth } from "@/_core/hooks/useAuth";
import Sidebar from "@/components/Sidebar";
import AddChildModal from "@/components/AddChildModal";
import ChildAccessModal from "@/components/ChildAccessModal";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  Plus,
  Loader2,
  KeyRound,
  Users,
  ArrowRight,
  AlertCircle,
  Mail,
  Check,
  Clock,
  Shield,
  CheckSquare,
  Heart,
  MessageSquare,
  BarChart3,
  Zap,
  LogOut,
  ArrowLeft,
} from "lucide-react";
import { useChild } from "@/contexts/ChildContext";
import { Child } from "@/contexts/ChildContext";

export default function ParentDashboard() {
  const { user, loading } = useAuth();
  const [, navigate] = useLocation();
  const { children, addChild } = useChild();
  const [isAddChildModalOpen, setIsAddChildModalOpen] = useState(false);
  const [isChildAccessModalOpen, setIsChildAccessModalOpen] = useState(false);
  const [isEmailVerified, setIsEmailVerified] = useState(() => {
    if (!user) return false;
    const verified = localStorage.getItem(`emailVerified_${user.id}`);
    return verified === "true";
  });

  useEffect(() => {
    if (!loading && !user) {
      navigate("/signin");
    }
  }, [user, navigate, loading]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  const handleVerifyEmail = () => {
    if (user) {
      localStorage.setItem(`emailVerified_${user.id}`, "true");
      setIsEmailVerified(true);
    }
  };

  const handleAccessChild = (child: Child) => {
    navigate(`/dashboard/child-view/${child.id}`);
  };

  // Show email verification screen if not verified
  if (!isEmailVerified) {
    return (
      <div className="flex min-h-screen bg-gradient-to-br from-blue-50 via-white to-indigo-50">
        <Sidebar role="parent" />
        <main className="flex-1 md:ml-64 flex items-center justify-center p-4">
          <Card className="p-8 max-w-md text-center border-2 border-blue-200">
            <div className="inline-flex items-center justify-center w-16 h-16 bg-blue-100 rounded-full mb-6">
              <Mail className="w-8 h-8 text-blue-600" />
            </div>
            <h2 className="text-2xl font-bold text-slate-900 mb-2">
              Verify Your Email
            </h2>
            <p className="text-slate-600 mb-6">
              Please verify your email address to access your dashboard. We've
              sent a verification link to{" "}
              <strong className="text-slate-900">{user?.email}</strong>
            </p>
            <Button
              onClick={handleVerifyEmail}
              className="w-full bg-gradient-to-r from-blue-500 to-green-500 hover:from-blue-600 hover:to-green-600 text-white font-semibold py-3 rounded-lg transition mb-4"
            >
              <Check className="w-5 h-5 mr-2" />
              Verify Email
            </Button>
            <p className="text-xs text-slate-500">
              Didn't receive an email? Check your spam folder or contact
              support.
            </p>
          </Card>
        </main>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-white">
      {/* Sidebar */}
      <Sidebar role="parent" />

      {/* Main Content */}
      <main className="flex-1 md:ml-64 pb-24">
        {/* Header */}
        <div className="bg-white border-b border-gray-100 sticky top-0 z-30 shadow-sm">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 md:py-8">
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-3xl md:text-4xl font-bold text-gray-900 mb-2">
                  Welcome back, {user?.email?.split("@")[0] || "Parent"}!
                </h1>
                <p className="text-gray-600 font-medium">
                  Manage your children's digital wellness
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Content Area */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 bg-white">
          {/* Empty State with Feature Overview */}
          {children.length === 0 ? (
            <div className="space-y-12">
              {/* Welcome Section */}
              <div className="text-center mb-16">
                <h2 className="text-4xl md:text-5xl font-bold text-gray-900 mb-6">
                  Welcome to MyNyaman!
                </h2>
                <p className="text-xl text-gray-600 max-w-3xl mx-auto leading-relaxed">
                  Manage your children's digital wellness with powerful monitoring and control features.
                </p>
              </div>

              {/* Feature Overview Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 mb-16">
                {/* Screen Time Tracking */}
                <Card className="p-8 hover:shadow-xl transition-all duration-300 border border-gray-200 bg-white">
                  <div className="flex items-start gap-4">
                    <div className="p-3 bg-blue-100 rounded-xl flex-shrink-0">
                      <Clock className="w-6 h-6 text-blue-600" />
                    </div>
                    <div className="text-left">
                      <h3 className="font-bold text-lg text-gray-900 mb-3">
                        Screen Time Tracking
                      </h3>
                      <p className="text-gray-600 leading-relaxed">
                        Monitor daily device usage and set healthy screen time limits.
                      </p>
                    </div>
                  </div>
                </Card>

                {/* App Blocking */}
                <Card className="p-8 hover:shadow-xl transition-all duration-300 border border-gray-200 bg-white">
                  <div className="flex items-start gap-4">
                    <div className="p-3 bg-yellow-100 rounded-xl flex-shrink-0">
                      <Shield className="w-6 h-6 text-yellow-600" />
                    </div>
                    <div className="text-left">
                      <h3 className="font-bold text-lg text-gray-900 mb-3">
                        App Blocking & Management
                      </h3>
                      <p className="text-gray-600 leading-relaxed">
                        Block inappropriate apps and manage access by time or category.
                      </p>
                    </div>
                  </div>
                </Card>

                {/* Task Management */}
                <Card className="p-8 hover:shadow-xl transition-all duration-300 border border-gray-200 bg-white">
                  <div className="flex items-start gap-4">
                    <div className="p-3 bg-blue-100 rounded-xl flex-shrink-0">
                      <CheckSquare className="w-6 h-6 text-blue-600" />
                    </div>
                    <div className="text-left">
                      <h3 className="font-bold text-lg text-gray-900 mb-3">
                        Task Management
                      </h3>
                      <p className="text-gray-600 leading-relaxed">
                        Create and track tasks to help children stay organized and productive.
                      </p>
                    </div>
                  </div>
                </Card>

                {/* Wellness Activities */}
                <Card className="p-6 hover:shadow-lg transition border border-slate-200">
                  <div className="flex items-start gap-4">
                    <div className="p-3 bg-purple-100 rounded-lg flex-shrink-0">
                      <Heart className="w-6 h-6 text-purple-600" />
                    </div>
                    <div className="text-left">
                      <h3 className="font-semibold text-slate-900 mb-2">
                        Wellness Activities
                      </h3>
                      <p className="text-sm text-slate-600">
                        Log meditation, yoga, reading, and other healthy
                        activities.
                      </p>
                    </div>
                  </div>
                </Card>

                {/* Chat with Consultant */}
                <Card className="p-6 hover:shadow-lg transition border border-slate-200">
                  <div className="flex items-start gap-4">
                    <div className="p-3 bg-indigo-100 rounded-lg flex-shrink-0">
                      <MessageSquare className="w-6 h-6 text-indigo-600" />
                    </div>
                    <div className="text-left">
                      <h3 className="font-semibold text-slate-900 mb-2">
                        Chat with Consultant
                      </h3>
                      <p className="text-sm text-slate-600">
                        Get expert guidance and support from digital wellness
                        consultants.
                      </p>
                    </div>
                  </div>
                </Card>

                {/* Usage Reports */}
                <Card className="p-6 hover:shadow-lg transition border border-slate-200">
                  <div className="flex items-start gap-4">
                    <div className="p-3 bg-orange-100 rounded-lg flex-shrink-0">
                      <BarChart3 className="w-6 h-6 text-orange-600" />
                    </div>
                    <div className="text-left">
                      <h3 className="font-semibold text-slate-900 mb-2">
                        Usage Reports
                      </h3>
                      <p className="text-sm text-slate-600">
                        View detailed analytics and insights about your
                        child's device usage.
                      </p>
                    </div>
                  </div>
                </Card>
              </div>

              {/* CTA Section */}
              <div className="text-center">
                <Button
                  onClick={() => setIsAddChildModalOpen(true)}
                  className="bg-gradient-to-r from-blue-500 to-green-500 hover:from-blue-600 hover:to-green-600 text-white font-semibold py-4 px-8 rounded-lg transition flex items-center justify-center gap-2 mx-auto text-lg"
                >
                  <Plus className="w-6 h-6" />
                  Add Your First Child
                </Button>
                <p className="text-slate-600 mt-4">
                  Start managing your child's digital wellness today
                </p>
              </div>
            </div>
          ) : (
            <>
              {/* Children Cards */}
              <div className="mb-8">
                <div className="flex items-center justify-between mb-6">
                  <h2 className="text-2xl font-bold text-slate-900">
                    Your Children ({children.length})
                  </h2>
                  <Button
                    onClick={() => setIsAddChildModalOpen(true)}
                    className="bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-4 rounded-lg transition flex items-center gap-2"
                  >
                    <Plus className="w-5 h-5" />
                    Add Child
                  </Button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {children.map((child) => (
                    <Card
                      key={child.id}
                      className="p-6 hover:shadow-lg transition cursor-pointer border border-slate-200"
                    >
                      {/* Child Header */}
                      <div className="flex items-start justify-between mb-4">
                        <div>
                          <h3 className="text-xl font-bold text-slate-900">
                            {child.name}
                          </h3>
                          <p className="text-sm text-slate-600">
                            {child.ageGroup}
                          </p>
                        </div>
                        <div
                          className={`px-3 py-1 rounded-full text-xs font-semibold ${
                            child.status === "active"
                              ? "bg-green-100 text-green-700"
                              : "bg-slate-100 text-slate-700"
                          }`}
                        >
                          {child.status === "active" ? "Active" : "Inactive"}
                        </div>
                      </div>

                      {/* Child Email */}
                      <p className="text-sm text-slate-600 mb-4">
                        {child.email}
                      </p>

                      {/* Divider */}
                      <div className="border-t border-slate-200 my-4" />

                      {/* Actions */}
                      <div className="space-y-2">
                        <button
                          onClick={() => handleAccessChild(child)}
                          className="w-full flex items-center justify-between p-3 bg-blue-50 hover:bg-blue-100 rounded-lg transition text-blue-600 font-semibold"
                        >
                          <span>View Dashboard</span>
                          <ArrowRight className="w-4 h-4" />
                        </button>
                      </div>
                    </Card>
                  ))}
                </div>
              </div>

              {/* Quick Actions */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <Card className="p-6 bg-gradient-to-br from-blue-50 to-blue-100 border border-blue-200">
                  <div className="flex items-center gap-4">
                    <div className="p-3 bg-blue-600 rounded-lg">
                      <KeyRound className="w-6 h-6 text-white" />
                    </div>
                    <div className="flex-1">
                      <h3 className="font-semibold text-slate-900">
                        Access Child Dashboard
                      </h3>
                      <p className="text-sm text-slate-600">
                        Use a verification code to access
                      </p>
                    </div>
                    <Button
                      onClick={() => setIsChildAccessModalOpen(true)}
                      className="bg-blue-600 hover:bg-blue-700 text-white"
                    >
                      Access
                    </Button>
                  </div>
                </Card>

                <Card className="p-6 bg-gradient-to-br from-green-50 to-green-100 border border-green-200">
                  <div className="flex items-center gap-4">
                    <div className="p-3 bg-green-600 rounded-lg">
                      <Users className="w-6 h-6 text-white" />
                    </div>
                    <div className="flex-1">
                      <h3 className="font-semibold text-slate-900">
                        Manage Children
                      </h3>
                      <p className="text-sm text-slate-600">
                        View codes and manage access
                      </p>
                    </div>
                    <Button
                      onClick={() => navigate("/manage-children")}
                      className="bg-green-600 hover:bg-green-700 text-white"
                    >
                      Manage
                    </Button>
                  </div>
                </Card>
              </div>
            </>
          )}
        </div>
      </main>

      {/* Modals */}
      <AddChildModal
        isOpen={isAddChildModalOpen}
        onClose={() => setIsAddChildModalOpen(false)}
      />
      <ChildAccessModal
        isOpen={isChildAccessModalOpen}
        onClose={() => setIsChildAccessModalOpen(false)}
      />
    </div>
  );
}
