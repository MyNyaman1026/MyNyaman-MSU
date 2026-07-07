import { useState } from "react";
import { ChevronDown, ArrowLeft } from "lucide-react";
import { useLocation } from "wouter";
import { useChild } from "@/contexts/ChildContext";
import ChildDashboard from "./ChildDashboard";
import ChildSelectorCards from "@/components/ChildSelectorCards";
import { Card } from "@/components/ui/card";

export default function ChildDashboardView() {
  const { selectedChild, children } = useChild();
  const [, navigate] = useLocation();
  const [showChildSelector, setShowChildSelector] = useState(false);

  if (!selectedChild) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 flex items-center justify-center p-4">
        <Card className="p-8 text-center max-w-md">
          <h2 className="text-xl font-semibold text-slate-900 mb-2">No Child Selected</h2>
          <p className="text-slate-600">Please select a child to view their dashboard.</p>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100">
      {/* Parent Monitoring Header */}
      <div className="bg-blue-600 text-white sticky top-0 z-50 shadow-lg">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => navigate("/dashboard/parent")}
                className="bg-white text-blue-600 hover:bg-blue-50 font-semibold py-2 px-4 rounded-lg transition flex items-center gap-2"
              >
                <ArrowLeft className="w-4 h-4" />
                <span>Back</span>
              </button>
              <div>
                <h1 className="text-2xl font-bold">Parent Monitoring View</h1>
                <p className="text-blue-100 text-sm">Viewing {selectedChild?.name}'s Dashboard</p>
              </div>
            </div>
            {children.length > 1 && (
              <button
                onClick={() => setShowChildSelector(!showChildSelector)}
                className="bg-white text-blue-600 hover:bg-blue-50 font-semibold py-2 px-4 rounded-lg transition flex items-center gap-2"
              >
                <span>Switch Child</span>
                <ChevronDown className="w-4 h-4" />
              </button>
            )}
          </div>

          {/* Child Selector - Only shown when Switch Child button is clicked */}
          {showChildSelector && children.length > 1 && (
            <div className="mt-4 pt-4 border-t border-blue-500">
              <ChildSelectorCards />
            </div>
          )}
        </div>
      </div>

      {/* Child Dashboard Content */}
      <ChildDashboard />
    </div>
  );
}
