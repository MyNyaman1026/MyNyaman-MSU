import { useState } from "react";
import { Card } from "@/components/ui/card";
import { Clock, AlertCircle, CheckCircle2, Zap, BookOpen, Activity, ChevronDown } from "lucide-react";
import { useChild } from "@/contexts/ChildContext";
import ChildSelectorCards from "@/components/ChildSelectorCards";

// Mock data for today's screen time
const screenTimeData = {
  today: "2h 30m",
  dailyLimit: "4h 0m",
  remaining: "1h 30m",
  percentage: 62.5, // 2.5 out of 4 hours
};

// Mock data for blocked apps
const blockedApps = [
  { id: "1", name: "TikTok", icon: "📱", reason: "Social media limit reached" },
  { id: "2", name: "Instagram", icon: "📷", reason: "Social media limit reached" },
  { id: "3", name: "YouTube", icon: "📺", reason: "Scheduled bedtime" },
  { id: "4", name: "Gaming App", icon: "🎮", reason: "Study time" },
];

// Mock data for child's tasks
const childTasks = [
  { id: "1", title: "Complete Math Homework", status: "completed", dueDate: "Today", priority: "high" },
  { id: "2", title: "Read Chapter 5", status: "completed", dueDate: "Today", priority: "medium" },
  { id: "3", title: "Practice Piano", status: "pending", dueDate: "Today", priority: "medium" },
  { id: "4", title: "Science Project", status: "pending", dueDate: "Tomorrow", priority: "high" },
];

// Mock data for wellness activities
const wellnessActivities = [
  { id: "1", name: "Morning Meditation", duration: "10 min", time: "7:30 AM", completed: true, icon: "🧘" },
  { id: "2", name: "Yoga Session", duration: "20 min", time: "4:00 PM", completed: true, icon: "🤸" },
  { id: "3", name: "Evening Walk", duration: "30 min", time: "6:00 PM", completed: false, icon: "🚶" },
  { id: "4", name: "Reading Time", duration: "15 min", time: "8:00 PM", completed: false, icon: "📚" },
];

export default function ChildDashboard() {
  const { selectedChild, children } = useChild();
  const [showChildSelector, setShowChildSelector] = useState(false);

  if (!selectedChild) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 flex items-center justify-center p-4">
        <Card className="p-8 text-center max-w-md">
          <AlertCircle className="w-12 h-12 text-orange-500 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-slate-900 mb-2">No Child Selected</h2>
          <p className="text-slate-600">Please select a child to view their dashboard.</p>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 pb-8">
      {/* Header */}
      <div className="bg-white border-b border-slate-200 sticky top-0 z-40 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h1 className="text-3xl font-bold text-slate-900">
                {selectedChild?.name}'s Dashboard
              </h1>
              <p className="text-slate-600 mt-1">Your Daily Activity</p>
            </div>
            {children.length > 1 && (
              <button
                onClick={() => setShowChildSelector(!showChildSelector)}
                className="bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-4 rounded-lg transition flex items-center gap-2"
              >
                <span>Switch Child</span>
                <ChevronDown className="w-4 h-4" />
              </button>
            )}
          </div>

          {/* Child Selector */}
          {showChildSelector && (
            <div className="mt-4 pt-4 border-t border-slate-200">
              <ChildSelectorCards />
            </div>
          )}
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Screen Time Section */}
        <div className="mb-8">
          <h2 className="text-2xl font-bold text-slate-900 mb-4">Today's Screen Time</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            {/* Used Time */}
            <Card className="p-6 bg-gradient-to-br from-blue-50 to-blue-100">
              <div className="flex items-center justify-between mb-2">
                <p className="text-sm font-semibold text-slate-700">Used Today</p>
                <Clock className="w-5 h-5 text-blue-600" />
              </div>
              <p className="text-3xl font-bold text-blue-600">{screenTimeData.today}</p>
              <p className="text-xs text-slate-600 mt-2">of {screenTimeData.dailyLimit} daily limit</p>
            </Card>

            {/* Remaining Time */}
            <Card className="p-6 bg-gradient-to-br from-green-50 to-green-100">
              <div className="flex items-center justify-between mb-2">
                <p className="text-sm font-semibold text-slate-700">Time Remaining</p>
                <Zap className="w-5 h-5 text-green-600" />
              </div>
              <p className="text-3xl font-bold text-green-600">{screenTimeData.remaining}</p>
              <p className="text-xs text-slate-600 mt-2">until daily limit reached</p>
            </Card>

            {/* Percentage */}
            <Card className="p-6 bg-gradient-to-br from-purple-50 to-purple-100">
              <div className="flex items-center justify-between mb-2">
                <p className="text-sm font-semibold text-slate-700">Usage</p>
                <Activity className="w-5 h-5 text-purple-600" />
              </div>
              <p className="text-3xl font-bold text-purple-600">{screenTimeData.percentage.toFixed(1)}%</p>
              <p className="text-xs text-slate-600 mt-2">of daily limit used</p>
            </Card>
          </div>

          {/* Progress Bar */}
          <Card className="p-6">
            <div className="flex items-center justify-between mb-3">
              <p className="font-semibold text-slate-900">Daily Limit Progress</p>
              <p className="text-sm text-slate-600">{screenTimeData.percentage.toFixed(1)}%</p>
            </div>
            <div className="w-full bg-slate-200 rounded-full h-3 overflow-hidden">
              <div
                className="bg-gradient-to-r from-blue-500 to-purple-500 h-full transition-all duration-500"
                style={{ width: `${screenTimeData.percentage}%` }}
              />
            </div>
            <p className="text-xs text-slate-600 mt-3">
              {screenTimeData.remaining} remaining before limit is reached
            </p>
          </Card>
        </div>

        {/* Blocked Apps Section */}
        <div className="mb-8">
          <h2 className="text-2xl font-bold text-slate-900 mb-4">Blocked Apps</h2>
          {blockedApps.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {blockedApps.map((app) => (
                <Card key={app.id} className="p-4 border-l-4 border-l-red-500 bg-red-50">
                  <div className="flex items-start gap-3">
                    <span className="text-3xl">{app.icon}</span>
                    <div className="flex-1">
                      <h3 className="font-semibold text-slate-900">{app.name}</h3>
                      <p className="text-sm text-red-600 mt-1">{app.reason}</p>
                    </div>
                    <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0" />
                  </div>
                </Card>
              ))}
            </div>
          ) : (
            <Card className="p-6 text-center bg-green-50">
              <CheckCircle2 className="w-12 h-12 text-green-600 mx-auto mb-2" />
              <p className="text-slate-700 font-semibold">No apps blocked</p>
              <p className="text-sm text-slate-600">All apps are available right now</p>
            </Card>
          )}
        </div>

        {/* Tasks Section */}
        <div className="mb-8">
          <h2 className="text-2xl font-bold text-slate-900 mb-4">Tasks</h2>
          <div className="space-y-3">
            {childTasks.map((task) => (
              <Card
                key={task.id}
                className={`p-4 border-l-4 ${
                  task.status === "completed"
                    ? "border-l-green-500 bg-green-50"
                    : "border-l-orange-500 bg-orange-50"
                }`}
              >
                <div className="flex items-center gap-3">
                  {task.status === "completed" ? (
                    <CheckCircle2 className="w-6 h-6 text-green-600 flex-shrink-0" />
                  ) : (
                    <div className="w-6 h-6 rounded-full border-2 border-orange-400 flex-shrink-0" />
                  )}
                  <div className="flex-1">
                    <h3
                      className={`font-semibold ${
                        task.status === "completed"
                          ? "text-slate-600 line-through"
                          : "text-slate-900"
                      }`}
                    >
                      {task.title}
                    </h3>
                    <div className="flex items-center gap-2 mt-1">
                      <span className="text-xs text-slate-600">{task.dueDate}</span>
                      <span
                        className={`text-xs font-semibold px-2 py-1 rounded ${
                          task.priority === "high"
                            ? "bg-red-200 text-red-700"
                            : "bg-yellow-200 text-yellow-700"
                        }`}
                      >
                        {task.priority}
                      </span>
                    </div>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        </div>

        {/* Wellness Activities Section */}
        <div className="mb-8">
          <h2 className="text-2xl font-bold text-slate-900 mb-4">Wellness Activities</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {wellnessActivities.map((activity) => (
              <Card
                key={activity.id}
                className={`p-4 border-l-4 ${
                  activity.completed
                    ? "border-l-green-500 bg-green-50"
                    : "border-l-slate-300 bg-slate-50"
                }`}
              >
                <div className="flex items-start gap-3">
                  <span className="text-3xl">{activity.icon}</span>
                  <div className="flex-1">
                    <h3 className="font-semibold text-slate-900">{activity.name}</h3>
                    <div className="flex items-center gap-2 mt-2">
                      <Clock className="w-4 h-4 text-slate-600" />
                      <span className="text-sm text-slate-600">{activity.time}</span>
                      <span className="text-sm text-slate-600">•</span>
                      <span className="text-sm text-slate-600">{activity.duration}</span>
                    </div>
                    {activity.completed && (
                      <div className="mt-2 flex items-center gap-1">
                        <CheckCircle2 className="w-4 h-4 text-green-600" />
                        <span className="text-xs font-semibold text-green-600">Completed</span>
                      </div>
                    )}
                  </div>
                </div>
              </Card>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
