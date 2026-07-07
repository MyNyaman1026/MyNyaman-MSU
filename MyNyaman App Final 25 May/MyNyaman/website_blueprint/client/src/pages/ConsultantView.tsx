import { Card } from "@/components/ui/card";
import { BarChart3, Users, TrendingUp, AlertCircle, MessageSquare, Clock, ArrowLeft } from "lucide-react";
import { useLocation } from "wouter";

// Mock data for children under consultant's care
const consultingChildren = [
  {
    id: "1",
    name: "Alex Johnson",
    age: 12,
    parentName: "John Johnson",
    status: "active",
    screenTime: "2h 30m",
    focusTime: "1h 45m",
    tasksCompleted: 8,
    wellnessScore: 85,
    concerns: ["High screen time", "Irregular sleep"],
  },
  {
    id: "2",
    name: "Emma Smith",
    age: 10,
    parentName: "Sarah Smith",
    status: "active",
    screenTime: "1h 50m",
    focusTime: "2h 15m",
    tasksCompleted: 10,
    wellnessScore: 92,
    concerns: [],
  },
  {
    id: "3",
    name: "Liam Brown",
    age: 13,
    parentName: "Michael Brown",
    status: "inactive",
    screenTime: "3h 20m",
    focusTime: "1h 30m",
    tasksCompleted: 6,
    wellnessScore: 72,
    concerns: ["Excessive screen time", "Low focus time", "Skipped wellness activities"],
  },
];

// Mock aggregated statistics
const aggregatedStats = {
  totalChildren: 3,
  activeChildren: 2,
  averageScreenTime: "2h 33m",
  averageFocusTime: "1h 50m",
  averageWellnessScore: 83,
  childrenWithConcerns: 2,
};

export default function ConsultantView() {
  const [, navigate] = useLocation();
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 pb-8">
      {/* Header */}
      <div className="bg-white border-b border-slate-200 sticky top-0 z-40 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex items-center gap-4 mb-4">
            <button
              onClick={() => window.history.back()}
              className="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-gray-100 transition-colors"
            >
              <ArrowLeft size={20} className="text-gray-700" />
              <span className="text-gray-700 font-medium">Back</span>
            </button>
          </div>
          <h1 className="text-3xl font-bold text-slate-900">Consultant Dashboard</h1>
          <p className="text-slate-600 mt-1">Monitor and manage all children under your consultation</p>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Aggregated Statistics */}
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
          <Card className="p-4 bg-gradient-to-br from-blue-50 to-blue-100">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-slate-600 mb-1">Total Children</p>
                <p className="text-2xl font-bold text-blue-600">{aggregatedStats.totalChildren}</p>
              </div>
              <Users className="w-8 h-8 text-blue-400" />
            </div>
          </Card>

          <Card className="p-4 bg-gradient-to-br from-green-50 to-green-100">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-slate-600 mb-1">Active</p>
                <p className="text-2xl font-bold text-green-600">{aggregatedStats.activeChildren}</p>
              </div>
              <TrendingUp className="w-8 h-8 text-green-400" />
            </div>
          </Card>

          <Card className="p-4 bg-gradient-to-br from-purple-50 to-purple-100">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-slate-600 mb-1">Avg Screen Time</p>
                <p className="text-lg font-bold text-purple-600">{aggregatedStats.averageScreenTime}</p>
              </div>
              <Clock className="w-8 h-8 text-purple-400" />
            </div>
          </Card>

          <Card className="p-4 bg-gradient-to-br from-orange-50 to-orange-100">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-slate-600 mb-1">Avg Focus Time</p>
                <p className="text-lg font-bold text-orange-600">{aggregatedStats.averageFocusTime}</p>
              </div>
              <BarChart3 className="w-8 h-8 text-orange-400" />
            </div>
          </Card>

          <Card className="p-4 bg-gradient-to-br from-pink-50 to-pink-100">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-slate-600 mb-1">Wellness Score</p>
                <p className="text-2xl font-bold text-pink-600">{aggregatedStats.averageWellnessScore}</p>
              </div>
              <TrendingUp className="w-8 h-8 text-pink-400" />
            </div>
          </Card>

          <Card className="p-4 bg-gradient-to-br from-red-50 to-red-100">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-slate-600 mb-1">With Concerns</p>
                <p className="text-2xl font-bold text-red-600">{aggregatedStats.childrenWithConcerns}</p>
              </div>
              <AlertCircle className="w-8 h-8 text-red-400" />
            </div>
          </Card>
        </div>

        {/* Children Overview */}
        <div className="mb-8">
          <h2 className="text-2xl font-bold text-slate-900 mb-4">Children Under Consultation</h2>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {consultingChildren.map((child) => (
              <Card key={child.id} className="p-6 border-l-4 border-l-blue-500 hover:shadow-lg transition">
                {/* Header */}
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <h3 className="text-lg font-semibold text-slate-900">{child.name}</h3>
                    <p className="text-sm text-slate-600">
                      Age {child.age} • Parent: {child.parentName}
                    </p>
                  </div>
                  <span
                    className={`text-xs font-semibold px-3 py-1 rounded-full ${
                      child.status === "active"
                        ? "bg-green-100 text-green-700"
                        : "bg-slate-100 text-slate-700"
                    }`}
                  >
                    {child.status === "active" ? "Active" : "Inactive"}
                  </span>
                </div>

                {/* Stats Grid */}
                <div className="grid grid-cols-2 gap-3 mb-4 pb-4 border-b border-slate-200">
                  <div>
                    <p className="text-xs text-slate-600 mb-1">Screen Time</p>
                    <p className="text-lg font-semibold text-slate-900">{child.screenTime}</p>
                  </div>
                  <div>
                    <p className="text-xs text-slate-600 mb-1">Focus Time</p>
                    <p className="text-lg font-semibold text-slate-900">{child.focusTime}</p>
                  </div>
                  <div>
                    <p className="text-xs text-slate-600 mb-1">Tasks Completed</p>
                    <p className="text-lg font-semibold text-slate-900">{child.tasksCompleted}</p>
                  </div>
                  <div>
                    <p className="text-xs text-slate-600 mb-1">Wellness Score</p>
                    <p className={`text-lg font-semibold ${
                      child.wellnessScore >= 80 ? "text-green-600" : "text-orange-600"
                    }`}>
                      {child.wellnessScore}%
                    </p>
                  </div>
                </div>

                {/* Concerns */}
                {child.concerns.length > 0 && (
                  <div className="bg-red-50 rounded-lg p-3">
                    <div className="flex items-start gap-2">
                      <AlertCircle className="w-4 h-4 text-red-600 mt-0.5 flex-shrink-0" />
                      <div>
                        <p className="text-xs font-semibold text-red-700 mb-1">Concerns:</p>
                        <ul className="text-xs text-red-600 space-y-1">
                          {child.concerns.map((concern, idx) => (
                            <li key={idx}>• {concern}</li>
                          ))}
                        </ul>
                      </div>
                    </div>
                  </div>
                )}

                {/* Action Button */}
                {child.concerns.length > 0 && (
                  <button className="mt-4 w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 rounded-lg transition flex items-center justify-center gap-2">
                    <MessageSquare className="w-4 h-4" />
                    Contact Parent
                  </button>
                )}
              </Card>
            ))}
          </div>
        </div>

        {/* Recommendations Section */}
        <Card className="p-6 bg-gradient-to-r from-blue-50 to-indigo-50 border-l-4 border-l-blue-600">
          <h3 className="text-lg font-semibold text-slate-900 mb-4 flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-blue-600" />
            Recommendations
          </h3>
          <div className="space-y-3">
            <div className="flex gap-3">
              <span className="text-blue-600 font-bold">1.</span>
              <p className="text-slate-700">
                <span className="font-semibold">Liam Brown</span> - High screen time detected. Recommend discussing app limits with parent.
              </p>
            </div>
            <div className="flex gap-3">
              <span className="text-blue-600 font-bold">2.</span>
              <p className="text-slate-700">
                <span className="font-semibold">Alex Johnson</span> - Irregular sleep patterns. Suggest wellness activities before bedtime.
              </p>
            </div>
            <div className="flex gap-3">
              <span className="text-blue-600 font-bold">3.</span>
              <p className="text-slate-700">
                <span className="font-semibold">Emma Smith</span> - Excellent progress! Maintain current routine and celebrate achievements.
              </p>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
