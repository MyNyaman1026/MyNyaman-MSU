import { useState, useEffect } from "react";
import { useLocation } from "wouter";
import { useRole } from "@/contexts/RoleContext";
import { useChild } from "@/contexts/ChildContext";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  LayoutDashboard,
  CheckSquare,
  Heart,
  MessageSquare,
  User,
  LogOut,
  Menu,
  X,
  Plus,
  Trash2,
  Clock,
  Zap,
  AlertCircle,
  ChevronRight,
  TrendingUp,
  ArrowLeft,
} from "lucide-react";

interface Task {
  id: string;
  title: string;
  priority: "high" | "medium" | "low";
  completed: boolean;
  dueDate?: string;
}

interface WellnessLog {
  id: string;
  activity: string;
  duration: number; // in minutes
  timestamp: string;
}

interface AppUsage {
  name: string;
  duration: number; // in minutes
  category: string;
}

export default function ChildDashboardProper() {
  const [, navigate] = useLocation();
  const { role, setRole } = useRole();
  const { selectedChild } = useChild();
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [activeTab, setActiveTab] = useState("dashboard");
  const [tasks, setTasks] = useState<Task[]>([
    {
      id: "1",
      title: "Complete homework",
      priority: "high",
      completed: false,
      dueDate: "2026-04-02",
    },
    {
      id: "2",
      title: "Practice piano",
      priority: "medium",
      completed: true,
      dueDate: "2026-04-01",
    },
    {
      id: "3",
      title: "Read a chapter",
      priority: "low",
      completed: false,
      dueDate: "2026-04-03",
    },
  ]);
  const [wellnessLogs, setWellnessLogs] = useState<WellnessLog[]>([
    {
      id: "1",
      activity: "Meditation",
      duration: 10,
      timestamp: "2026-04-02T08:00:00",
    },
    {
      id: "2",
      activity: "Yoga",
      duration: 20,
      timestamp: "2026-04-02T17:30:00",
    },
  ]);
  const [newTaskTitle, setNewTaskTitle] = useState("");
  const [newWellnessActivity, setNewWellnessActivity] = useState("");
  const [wellnessDuration, setWellnessDuration] = useState("10");

  // Redirect if not child
  useEffect(() => {
    if (role !== "child") {
      navigate("/");
    }
  }, [role, navigate]);

  const handleLogout = () => {
    localStorage.removeItem("childSession");
    setRole(null);
    navigate("/");
  };

  const handleAddTask = () => {
    if (newTaskTitle.trim()) {
      const newTask: Task = {
        id: Date.now().toString(),
        title: newTaskTitle,
        priority: "medium",
        completed: false,
      };
      setTasks([...tasks, newTask]);
      setNewTaskTitle("");
    }
  };

  const handleToggleTask = (taskId: string) => {
    setTasks(
      tasks.map((task) =>
        task.id === taskId ? { ...task, completed: !task.completed } : task
      )
    );
  };

  const handleDeleteTask = (taskId: string) => {
    setTasks(tasks.filter((task) => task.id !== taskId));
  };

  const handleLogWellness = () => {
    if (newWellnessActivity.trim()) {
      const newLog: WellnessLog = {
        id: Date.now().toString(),
        activity: newWellnessActivity,
        duration: parseInt(wellnessDuration),
        timestamp: new Date().toISOString(),
      };
      setWellnessLogs([...wellnessLogs, newLog]);
      setNewWellnessActivity("");
      setWellnessDuration("10");
    }
  };

  const completedTasks = tasks.filter((t) => t.completed).length;
  const screenTimeUsed = 150; // minutes
  const screenTimeLimit = 240; // minutes
  const screenTimeRemaining = screenTimeLimit - screenTimeUsed;

  const menuItems = [
    { id: "dashboard", label: "Dashboard", icon: LayoutDashboard },
    { id: "tasks", label: "Tasks", icon: CheckSquare },
    { id: "wellness", label: "Wellness", icon: Heart },
    { id: "chat", label: "Chat", icon: MessageSquare },
    { id: "profile", label: "Profile", icon: User },
  ];

  return (
    <div className="flex min-h-screen bg-slate-50">
      {/* Sidebar */}
      <div
        className={`${
          sidebarOpen ? "w-64" : "w-0"
        } bg-gradient-to-b from-blue-600 to-blue-700 text-white transition-all duration-300 overflow-hidden fixed h-screen z-40 md:relative md:w-64`}
      >
        <div className="p-6">
          <h1 className="text-2xl font-bold">MyNyaman</h1>
          <p className="text-blue-100 text-sm mt-1">Child Dashboard</p>
        </div>

        {/* Navigation */}
        <nav className="mt-8 space-y-2 px-4">
          {menuItems.map((item) => {
            const Icon = item.icon;
            return (
              <button
                key={item.id}
                onClick={() => {
                  setActiveTab(item.id);
                  setSidebarOpen(false);
                }}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition ${
                  activeTab === item.id
                    ? "bg-white text-blue-600 font-semibold"
                    : "text-blue-100 hover:bg-blue-500"
                }`}
              >
                <Icon className="w-5 h-5" />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>

        {/* Logout Button */}
        <div className="absolute bottom-6 left-4 right-4">
          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-3 px-4 py-3 bg-red-500 hover:bg-red-600 rounded-lg transition text-white font-semibold"
          >
            <LogOut className="w-5 h-5" />
            <span>Logout</span>
          </button>
        </div>
      </div>

      {/* Main Content */}
      <main className="flex-1">
        {/* Header */}
        <div className="bg-white border-b border-slate-200 sticky top-0 z-30">
          <div className="px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => window.history.back()}
                className="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-gray-100 transition-colors"
              >
                <ArrowLeft size={20} className="text-gray-700" />
                <span className="text-gray-700 font-medium">Back</span>
              </button>
              <button
                onClick={() => setSidebarOpen(!sidebarOpen)}
                className="md:hidden p-2 hover:bg-slate-100 rounded-lg"
              >
                {sidebarOpen ? (
                  <X className="w-6 h-6" />
                ) : (
                  <Menu className="w-6 h-6" />
                )}
              </button>
              <div>
                <h2 className="text-2xl font-bold text-slate-900">
                  Welcome, {selectedChild?.name || "Child"}!
                </h2>
                <p className="text-slate-600 text-sm">
                  {new Date().toLocaleDateString("en-US", {
                    weekday: "long",
                    year: "numeric",
                    month: "long",
                    day: "numeric",
                  })}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Content */}
        <div className="p-4 sm:p-6 lg:p-8">
          {/* Dashboard Tab */}
          {activeTab === "dashboard" && (
            <div className="space-y-6">
              {/* Screen Time Card */}
              <Card className="p-6 bg-gradient-to-br from-blue-50 to-blue-100 border border-blue-200">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-xl font-bold text-slate-900">
                    Today's Screen Time
                  </h3>
                  <Clock className="w-6 h-6 text-blue-600" />
                </div>
                <div className="space-y-4">
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-slate-700 font-semibold">
                        Used: {Math.floor(screenTimeUsed / 60)}h{" "}
                        {screenTimeUsed % 60}m
                      </span>
                      <span className="text-slate-600">
                        Limit: {Math.floor(screenTimeLimit / 60)}h{" "}
                        {screenTimeLimit % 60}m
                      </span>
                    </div>
                    <div className="w-full bg-slate-300 rounded-full h-3">
                      <div
                        className="bg-gradient-to-r from-blue-500 to-blue-600 h-3 rounded-full transition-all"
                        style={{
                          width: `${(screenTimeUsed / screenTimeLimit) * 100}%`,
                        }}
                      />
                    </div>
                  </div>
                  <div className="bg-white rounded-lg p-3">
                    <p className="text-sm text-slate-700">
                      <strong>Time Remaining:</strong> {screenTimeRemaining}
                      minutes
                    </p>
                  </div>
                </div>
              </Card>

              {/* App Usage */}
              <Card className="p-6">
                <h3 className="text-xl font-bold text-slate-900 mb-4 flex items-center gap-2">
                  <Zap className="w-6 h-6 text-orange-500" />
                  App Usage Today
                </h3>
                <div className="space-y-3">
                  {[
                    { name: "YouTube", duration: 45, category: "Entertainment" },
                    { name: "TikTok", duration: 30, category: "Social" },
                    { name: "Games", duration: 40, category: "Gaming" },
                    { name: "Study App", duration: 35, category: "Education" },
                  ].map((app, idx) => (
                    <div key={idx} className="flex items-center justify-between">
                      <div className="flex-1">
                        <p className="font-semibold text-slate-900">
                          {app.name}
                        </p>
                        <p className="text-xs text-slate-600">{app.category}</p>
                      </div>
                      <span className="text-slate-700 font-semibold">
                        {app.duration}m
                      </span>
                    </div>
                  ))}
                </div>
              </Card>

              {/* Blocked Apps */}
              <Card className="p-6 bg-red-50 border border-red-200">
                <h3 className="text-xl font-bold text-slate-900 mb-4 flex items-center gap-2">
                  <AlertCircle className="w-6 h-6 text-red-600" />
                  Blocked Apps
                </h3>
                <div className="space-y-2">
                  {["Instagram", "Snapchat", "Dating Apps"].map((app, idx) => (
                    <div
                      key={idx}
                      className="flex items-center gap-2 p-2 bg-white rounded-lg"
                    >
                      <div className="w-2 h-2 bg-red-600 rounded-full" />
                      <span className="text-slate-700">{app}</span>
                    </div>
                  ))}
                </div>
              </Card>

              {/* Tasks Summary */}
              <Card className="p-6 bg-green-50 border border-green-200">
                <h3 className="text-xl font-bold text-slate-900 mb-4">
                  Tasks Progress
                </h3>
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-3xl font-bold text-green-600">
                      {completedTasks}/{tasks.length}
                    </p>
                    <p className="text-slate-600">Tasks Completed</p>
                  </div>
                  <div className="w-20 h-20">
                    <div className="relative w-full h-full">
                      <svg
                        className="transform -rotate-90"
                        width="80"
                        height="80"
                      >
                        <circle
                          cx="40"
                          cy="40"
                          r="35"
                          fill="none"
                          stroke="#e2e8f0"
                          strokeWidth="4"
                        />
                        <circle
                          cx="40"
                          cy="40"
                          r="35"
                          fill="none"
                          stroke="#16a34a"
                          strokeWidth="4"
                          strokeDasharray={`${(completedTasks / tasks.length) * 220} 220`}
                          strokeLinecap="round"
                        />
                      </svg>
                      <div className="absolute inset-0 flex items-center justify-center">
                        <span className="text-lg font-bold text-green-600">
                          {Math.round((completedTasks / tasks.length) * 100)}%
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </Card>
            </div>
          )}

          {/* Tasks Tab */}
          {activeTab === "tasks" && (
            <div className="space-y-6">
              <Card className="p-6">
                <h3 className="text-2xl font-bold text-slate-900 mb-4">
                  My Tasks
                </h3>

                {/* Add Task */}
                <div className="mb-6 p-4 bg-slate-50 rounded-lg">
                  <label className="block text-sm font-semibold text-slate-900 mb-2">
                    Add New Task
                  </label>
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={newTaskTitle}
                      onChange={(e) => setNewTaskTitle(e.target.value)}
                      onKeyPress={(e) =>
                        e.key === "Enter" && handleAddTask()
                      }
                      placeholder="Enter task title..."
                      className="flex-1 px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                    <Button
                      onClick={handleAddTask}
                      className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center gap-2"
                    >
                      <Plus className="w-4 h-4" />
                      Add
                    </Button>
                  </div>
                </div>

                {/* Tasks List */}
                <div className="space-y-2">
                  {tasks.map((task) => (
                    <div
                      key={task.id}
                      className="flex items-center gap-3 p-4 bg-slate-50 rounded-lg hover:bg-slate-100 transition"
                    >
                      <input
                        type="checkbox"
                        checked={task.completed}
                        onChange={() => handleToggleTask(task.id)}
                        className="w-5 h-5 text-blue-600 rounded cursor-pointer"
                      />
                      <div className="flex-1">
                        <p
                          className={`font-semibold ${
                            task.completed
                              ? "line-through text-slate-500"
                              : "text-slate-900"
                          }`}
                        >
                          {task.title}
                        </p>
                        {task.dueDate && (
                          <p className="text-xs text-slate-600">
                            Due: {new Date(task.dueDate).toLocaleDateString()}
                          </p>
                        )}
                      </div>
                      <span
                        className={`px-2 py-1 rounded text-xs font-semibold ${
                          task.priority === "high"
                            ? "bg-red-100 text-red-700"
                            : task.priority === "medium"
                            ? "bg-yellow-100 text-yellow-700"
                            : "bg-green-100 text-green-700"
                        }`}
                      >
                        {task.priority}
                      </span>
                      <button
                        onClick={() => handleDeleteTask(task.id)}
                        className="p-2 hover:bg-red-100 rounded-lg transition text-red-600"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              </Card>
            </div>
          )}

          {/* Wellness Tab */}
          {activeTab === "wellness" && (
            <div className="space-y-6">
              <Card className="p-6">
                <h3 className="text-2xl font-bold text-slate-900 mb-4">
                  Wellness Activities
                </h3>

                {/* Log Activity */}
                <div className="mb-6 p-4 bg-slate-50 rounded-lg">
                  <label className="block text-sm font-semibold text-slate-900 mb-2">
                    Log Activity
                  </label>
                  <div className="flex gap-2 mb-3">
                    <select
                      value={newWellnessActivity}
                      onChange={(e) => setNewWellnessActivity(e.target.value)}
                      className="flex-1 px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="">Select activity...</option>
                      <option value="Meditation">Meditation</option>
                      <option value="Yoga">Yoga</option>
                      <option value="Walking">Walking</option>
                      <option value="Reading">Reading</option>
                      <option value="Sports">Sports</option>
                      <option value="Drawing">Drawing</option>
                    </select>
                    <input
                      type="number"
                      value={wellnessDuration}
                      onChange={(e) => setWellnessDuration(e.target.value)}
                      placeholder="Minutes"
                      min="1"
                      max="180"
                      className="w-24 px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                    <Button
                      onClick={handleLogWellness}
                      className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg flex items-center gap-2"
                    >
                      <Plus className="w-4 h-4" />
                      Log
                    </Button>
                  </div>
                </div>

                {/* Wellness History */}
                <div className="space-y-2">
                  {wellnessLogs.map((log) => (
                    <div
                      key={log.id}
                      className="flex items-center gap-3 p-4 bg-slate-50 rounded-lg"
                    >
                      <div className="p-2 bg-green-100 rounded-lg">
                        <Heart className="w-5 h-5 text-green-600" />
                      </div>
                      <div className="flex-1">
                        <p className="font-semibold text-slate-900">
                          {log.activity}
                        </p>
                        <p className="text-xs text-slate-600">
                          {new Date(log.timestamp).toLocaleTimeString()}
                        </p>
                      </div>
                      <span className="text-slate-700 font-semibold">
                        {log.duration}m
                      </span>
                    </div>
                  ))}
                </div>
              </Card>
            </div>
          )}

          {/* Chat Tab */}
          {activeTab === "chat" && (
            <Card className="p-6">
              <h3 className="text-2xl font-bold text-slate-900 mb-4">
                Chat with Consultant
              </h3>
              <div className="h-96 bg-slate-50 rounded-lg flex items-center justify-center">
                <p className="text-slate-600">Chat feature coming soon...</p>
              </div>
            </Card>
          )}

          {/* Profile Tab */}
          {activeTab === "profile" && (
            <Card className="p-6">
              <h3 className="text-2xl font-bold text-slate-900 mb-4">
                My Profile
              </h3>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-semibold text-slate-900">
                    Name
                  </label>
                  <p className="text-slate-700">{selectedChild?.name}</p>
                </div>
                <div>
                  <label className="block text-sm font-semibold text-slate-900">
                    Email
                  </label>
                  <p className="text-slate-700">{selectedChild?.email}</p>
                </div>
                <div>
                  <label className="block text-sm font-semibold text-slate-900">
                    Age Group
                  </label>
                  <p className="text-slate-700">{selectedChild?.ageGroup}</p>
                </div>
              </div>
            </Card>
          )}
        </div>
      </main>
    </div>
  );
}
