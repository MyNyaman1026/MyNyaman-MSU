import { useState, useEffect } from "react";
import { useLocation } from "wouter";
import { useAuth } from "@/_core/hooks/useAuth";
import Sidebar from "@/components/Sidebar";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  Plus,
  Copy,
  Check,
  Trash2,
  RefreshCw,
  Clock,
  Loader2,
  AlertCircle,
  ArrowLeft,
} from "lucide-react";
import { toast } from "sonner";
import AddChildModal from "@/components/AddChildModal";
import { useChild } from "@/contexts/ChildContext";

export default function ManageChildren() {
  const { user, loading } = useAuth();
  const [, navigate] = useLocation();
  const { children, addChild, removeChild } = useChild();
  const [isAddChildModalOpen, setIsAddChildModalOpen] = useState(false);
  const [copiedCode, setCopiedCode] = useState<string | null>(null);
  const [generatedCodes, setGeneratedCodes] = useState<
    Array<{
      childId: string;
      childName: string;
      code: string;
      expiresAt: Date;
      used: boolean;
    }>
  >([]);

  useEffect(() => {
    if (!loading && !user) {
      navigate("/signin");
    }
  }, [user, navigate, loading]);

  // Load generated codes from localStorage
  useEffect(() => {
    const savedCodes = localStorage.getItem("generatedCodes");
    if (savedCodes) {
      try {
        const parsed = JSON.parse(savedCodes);
        setGeneratedCodes(
          parsed.map((code: any) => ({
            ...code,
            expiresAt: new Date(code.expiresAt),
          }))
        );
      } catch (error) {
        console.error("Failed to parse codes:", error);
      }
    }
  }, []);

  const copyToClipboard = (code: string) => {
    navigator.clipboard.writeText(code);
    setCopiedCode(code);
    toast.success("Code copied to clipboard!");
    setTimeout(() => setCopiedCode(null), 2000);
  };

  const revokeCode = (code: string) => {
    setGeneratedCodes((prev) => prev.filter((c) => c.code !== code));
    toast.success("Code revoked successfully");
  };

  const regenerateCode = (childId: string, childName: string) => {
    // Remove old code
    setGeneratedCodes((prev) => prev.filter((c) => c.childId !== childId));
    // Open modal to generate new code
    setIsAddChildModalOpen(true);
    toast.info("Please generate a new code for " + childName);
  };

  const isCodeExpired = (expiresAt: Date) => {
    return new Date() > expiresAt;
  };

  const getCodeStatus = (code: {
    used: boolean;
    expiresAt: Date;
  }): "used" | "expired" | "active" => {
    if (code.used) return "used";
    if (isCodeExpired(code.expiresAt)) return "expired";
    return "active";
  };

  const getStatusColor = (
    status: "used" | "expired" | "active"
  ): string => {
    switch (status) {
      case "active":
        return "bg-green-100 text-green-700";
      case "expired":
        return "bg-yellow-100 text-yellow-700";
      case "used":
        return "bg-blue-100 text-blue-700";
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-gradient-to-br from-blue-50 via-white to-indigo-50">
      {/* Sidebar */}
      <Sidebar role="parent" />

      {/* Main Content */}
      <main className="flex-1 md:ml-64 pb-24">
        {/* Header */}
        <div className="bg-white border-b border-slate-200 sticky top-0 z-30">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 md:py-6">
            {/* Back Button */}
            <button
              onClick={() => navigate("/dashboard/parent")}
              className="mb-4 flex items-center gap-2 text-gray-600 hover:text-gray-900 transition-colors"
            >
              <ArrowLeft className="w-5 h-5" />
              <span>Back to Dashboard</span>
            </button>
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-2xl md:text-3xl font-bold text-slate-900">
                  Manage Children
                </h1>
                <p className="text-slate-600 mt-1">
                  View and manage your children's accounts and linking codes
                </p>
              </div>
              <Button
                onClick={() => setIsAddChildModalOpen(true)}
                className="bg-blue-600 hover:bg-blue-700 text-white font-semibold px-4 py-2 rounded-lg flex items-center gap-2"
              >
                <Plus className="w-5 h-5" />
                Add Child
              </Button>
            </div>
          </div>
        </div>

        {/* Content Area */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {/* Children List */}
          {children.length > 0 ? (
            <div className="space-y-6">
              <h2 className="text-xl font-semibold text-slate-900">
                Your Children ({children.length})
              </h2>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {children.map((child) => {
                  const childCode = generatedCodes.find(
                    (c) => c.childId === child.id
                  );
                  const status = childCode
                    ? getCodeStatus(childCode)
                    : "no-code";

                  return (
                    <Card
                      key={child.id}
                      className="p-6 border-slate-200 hover:shadow-lg transition"
                    >
                      {/* Child Info */}
                      <div className="mb-4">
                        <h3 className="text-lg font-semibold text-slate-900">
                          {child.name}
                        </h3>
                        <p className="text-sm text-slate-600">
                          {child.email}
                        </p>
                        <p className="text-sm text-slate-600">
                          Age: {child.ageGroup}
                        </p>
                      </div>

                      {/* Code Section */}
                      {childCode ? (
                        <div className="mb-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
                          <div className="flex items-center justify-between mb-2">
                            <p className="text-xs font-medium text-slate-600 uppercase">
                              Linking Code
                            </p>
                            <span
                              className={`text-xs font-medium px-2 py-1 rounded-full ${getStatusColor(
                                status as "used" | "expired" | "active"
                              )}`}
                            >
                              {status === "active"
                                ? "Active"
                                : status === "expired"
                                  ? "Expired"
                                  : "Used"}
                            </span>
                          </div>
                          <p className="text-2xl font-bold text-blue-600 tracking-widest mb-2">
                            {childCode.code}
                          </p>
                          <div className="flex items-center gap-2 text-xs text-slate-600 mb-3">
                            <Clock className="w-4 h-4" />
                            Expires:{" "}
                            {childCode.expiresAt.toLocaleString()}
                          </div>

                          {/* Code Actions */}
                          <div className="flex gap-2">
                            <Button
                              onClick={() =>
                                copyToClipboard(childCode.code)
                              }
                              className="flex-1 bg-blue-600 hover:bg-blue-700 text-white text-sm py-2 rounded flex items-center justify-center gap-2"
                            >
                              {copiedCode === childCode.code ? (
                                <>
                                  <Check className="w-4 h-4" />
                                  Copied
                                </>
                              ) : (
                                <>
                                  <Copy className="w-4 h-4" />
                                  Copy
                                </>
                              )}
                            </Button>
                            {status === "expired" && (
                              <Button
                                onClick={() =>
                                  regenerateCode(child.id, child.name)
                                }
                                className="flex-1 bg-green-600 hover:bg-green-700 text-white text-sm py-2 rounded flex items-center justify-center gap-2"
                              >
                                <RefreshCw className="w-4 h-4" />
                                Regenerate
                              </Button>
                            )}
                            {!childCode.used && (
                              <Button
                                onClick={() => revokeCode(childCode.code)}
                                className="flex-1 bg-red-600 hover:bg-red-700 text-white text-sm py-2 rounded flex items-center justify-center gap-2"
                              >
                                <Trash2 className="w-4 h-4" />
                                Revoke
                              </Button>
                            )}
                          </div>
                        </div>
                      ) : (
                        <div className="mb-4 p-4 bg-yellow-50 rounded-lg border border-yellow-200 flex items-start gap-3">
                          <AlertCircle className="w-5 h-5 text-yellow-600 mt-0.5 flex-shrink-0" />
                          <div>
                            <p className="text-sm font-medium text-yellow-700">
                              No code generated
                            </p>
                            <p className="text-xs text-yellow-600 mt-1">
                              Generate a code to link this child's account
                            </p>
                          </div>
                        </div>
                      )}

                      {/* Child Actions */}
                      <div className="flex gap-2 pt-4 border-t border-slate-200">
                        <Button
                          onClick={() =>
                            removeChild(child.id)
                          }
                          className="flex-1 bg-red-100 hover:bg-red-200 text-red-700 text-sm py-2 rounded flex items-center justify-center gap-2"
                        >
                          <Trash2 className="w-4 h-4" />
                          Remove
                        </Button>
                      </div>
                    </Card>
                  );
                })}
              </div>
            </div>
          ) : (
            <div className="text-center py-12">
              <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Plus className="w-8 h-8 text-blue-600" />
              </div>
              <h3 className="text-lg font-semibold text-slate-900 mb-2">
                No children added yet
              </h3>
              <p className="text-slate-600 mb-6">
                Start by adding your first child to manage their digital wellness
              </p>
              <Button
                onClick={() => setIsAddChildModalOpen(true)}
                className="bg-blue-600 hover:bg-blue-700 text-white font-semibold px-6 py-2 rounded-lg"
              >
                Add Your First Child
              </Button>
            </div>
          )}
        </div>

        {/* Add Child Modal */}
        <AddChildModal
          isOpen={isAddChildModalOpen}
          onClose={() => setIsAddChildModalOpen(false)}
        />
      </main>
    </div>
  );
}
