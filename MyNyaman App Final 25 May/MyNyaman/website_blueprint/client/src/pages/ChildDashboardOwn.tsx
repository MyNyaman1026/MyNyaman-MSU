import { useState } from "react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Clock, AlertCircle, CheckCircle2, Zap, Activity, Plus, X } from "lucide-react";
import ChildSidebar from "@/components/ChildSidebar";

// Mock data for today's screen time
const screenTimeData = {
  today: "2h 30m",
  dailyLimit: "4h 0m",
  remaining: "1h 30m",
  percentage: 62.5,
};

// Mock data for blocked apps
const blockedApps = [
  { id: "1", name: "TikTok", icon: "📱", reason: "Social media limit reached" },
  { id: "2", name: "Instagram", icon: "📷", reason: "Social media limit reached" },
  { id: "3", name: "YouTube", icon: "📺", reason: "Scheduled bedtime" },
];

// Initial mock tasks
const initialTasks = [
  { id: "1", title: "Complete Math Homework", status: "completed", dueDate: "Today", priority: "high" },
  { id: "2", title: "Read Chapter 5", status: "completed", dueDate: "Today", priority: "medium" },
  { id: "3", title: "Practice Piano", status: "pending", dueDate: "Today", priority: "medium" },
];

// Initial mock wellness activities
const initialWellness = [
  { id: "1", name: "Morning Meditation", duration: "10 min", time: "7:30 AM", completed: true, icon: "🧘" },
  { id: "2", name: "Yoga Session", duration: "20 min", time: "4:00 PM", completed: true, icon: "🤸" },
  { id: "3", name: "Evening Walk", duration: "30 min", time: "6:00 PM", completed: false, icon: "🚶" },
];

export default function ChildDashboardOwn() {
  const [tasks, setTasks] = useState(initialTasks);
  const [wellness, setWellness] = useState(initialWellness);
  const [showAddTask, setShowAddTask] = useState(false);
  const [showAddWellness, setShowAddWellness] = useState(false);
  const [newTask, setNewTask] = useState({ title: "", priority: "medium", dueDate: "Today" });
  const [newWellness, setNewWellness] = useState({ name: "", duration: "15 min" });

  const handleAddTask = () => {
    if (newTask.title.trim()) {
      setTasks([
        ...tasks,
        {
          id: Date.now().toString(),
          title: newTask.title,
          status: "pending",
          dueDate: newTask.dueDate,
          priority: newTask.priority,
        },
      ]);
      setNewTask({ title: "", priority: "medium", dueDate: "Today" });
      setShowAddTask(false);
    }
  };

  const handleToggleTask = (id: string) => {
    setTasks(
      tasks.map((task) =>
        task.id === id
          ? { ...task, status: task.status === "completed" ? "pending" : "completed" }
          : task
      )
    );
  };

  const handleDeleteTask = (id: string) => {
    setTasks(tasks.filter((task) => task.id !== id));
  };

  const handleAddWellness = () => {
    if (newWellness.name.trim()) {
      setWellness([
        ...wellness,
        {
          id: Date.now().toString(),
          name: newWellness.name,
          duration: newWellness.duration,
          time: new Date().toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" }),
          completed: true,
          icon: "✨",
        },
      ]);
      setNewWellness({ name: "", duration: "15 min" });
      setShowAddWellness(false);
    }
  };

  const completedTasks = tasks.filter((t) => t.status === "completed").length;
  const completedWellness = wellness.filter((w) => w.completed).length;

  return (
    <div className="flex min-h-screen bg-gradient-to-br from-slate-50 to-slate-100">
      {/* Sidebar */}
      <ChildSidebar />

      {/* Main Content */}
      <main className="flex-1 md:ml-64 pb-24">
        {/* Header */}
        <div className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-sm">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
            <h1 className="text-3xl font-bold text-slate-900">Your Dashboard</h1>
            <p className="text-slate-600 mt-1">Track your progress and manage your activities</p>
          </div>
        </div>

        {/* Content Area */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {/* Screen Time Section */}
          <div className="mb-8">
            <h2 className="text-2xl font-bold text-slate-900 mb-4">Today's Screen Time</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
              <Card className="p-6 bg-gradient-to-br from-blue-50 to-blue-100">
                <div className="flex items-center justify-between mb-2">
                  <p className="text-sm font-semibold text-slate-700">Used Today</p>
                  <Clock className="w-5 h-5 text-blue-600" />
                </div>
                <p className="text-3xl font-bold text-blue-600">{screenTimeData.today}</p>
                <p className="text-xs text-slate-600 mt-2">of {screenTimeData.dailyLimit} daily limit</p>
              </Card>

              <Card className="p-6 bg-gradient-to-br from-green-50 to-green-100">
                <div className="flex items-center justify-between mb-2">
                  <p className="text-sm font-semibold text-slate-700">Time Remaining</p>
                  <Zap className="w-5 h-5 text-green-600" />
                </div>
                <p className="text-3xl font-bold text-green-600">{screenTimeData.remaining}</p>
                <p className="text-xs text-slate-600 mt-2">until daily limit reached</p>
              </Card>

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
              </Card>
            )}
          </div>

          {/* Tasks Section */}
          <div className="mb-8">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-2xl font-bold text-slate-900">My Tasks</h2>
                <p className="text-sm text-slate-600 mt-1">
                  {completedTasks} of {tasks.length} completed
                </p>
              </div>
              <Button
                onClick={() => setShowAddTask(true)}
                className="bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-4 rounded-lg flex items-center gap-2"
              >
                <Plus className="w-5 h-5" />
                Add Task
              </Button>
            </div>

            {/* Add Task Modal */}
            {showAddTask && (
              <Card className="p-6 mb-6 border-2 border-blue-500 bg-blue-50">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="font-semibold text-slate-900">Add New Task</h3>
                  <button
                    onClick={() => setShowAddTask(false)}
                    className="text-slate-600 hover:text-slate-900"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
                <div className="space-y-4">
                  <input
                    type="text"
                    placeholder="Task title..."
                    value={newTask.title}
                    onChange={(e) => setNewTask({ ...newTask, title: e.target.value })}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                  <div className="grid grid-cols-2 gap-4">
                    <select
                      value={newTask.priority}
                      onChange={(e) => setNewTask({ ...newTask, priority: e.target.value })}
                      className="px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="low">Low Priority</option>
                      <option value="medium">Medium Priority</option>
                      <option value="high">High Priority</option>
                    </select>
                    <select
                      value={newTask.dueDate}
                      onChange={(e) => setNewTask({ ...newTask, dueDate: e.target.value })}
                      className="px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="Today">Today</option>
                      <option value="Tomorrow">Tomorrow</option>
                      <option value="This Week">This Week</option>
                    </select>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      onClick={handleAddTask}
                      className="flex-1 bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 rounded-lg"
                    >
                      Add Task
                    </Button>
                    <Button
                      onClick={() => setShowAddTask(false)}
                      className="flex-1 bg-slate-300 hover:bg-slate-400 text-slate-900 font-semibold py-2 rounded-lg"
                    >
                      Cancel
                    </Button>
                  </div>
                </div>
              </Card>
            )}

            {/* Tasks List */}
            <div className="space-y-3">
              {tasks.map((task) => (
                <Card
                  key={task.id}
                  className={`p-4 border-l-4 ${
                    task.status === "completed"
                      ? "border-l-green-500 bg-green-50"
                      : "border-l-orange-500 bg-orange-50"
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <button
                      onClick={() => handleToggleTask(task.id)}
                      className="flex-shrink-0"
                    >
                      {task.status === "completed" ? (
                        <CheckCircle2 className="w-6 h-6 text-green-600" />
                      ) : (
                        <div className="w-6 h-6 rounded-full border-2 border-orange-400" />
                      )}
                    </button>
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
                              : task.priority === "medium"
                              ? "bg-yellow-200 text-yellow-700"
                              : "bg-blue-200 text-blue-700"
                          }`}
                        >
                          {task.priority}
                        </span>
                      </div>
                    </div>
                    <button
                      onClick={() => handleDeleteTask(task.id)}
                      className="text-slate-400 hover:text-red-600 flex-shrink-0"
                    >
                      <X className="w-5 h-5" />
                    </button>
                  </div>
                </Card>
              ))}
            </div>
          </div>

          {/* Wellness Activities Section */}
          <div className="mb-8">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-2xl font-bold text-slate-900">Wellness Activities</h2>
                <p className="text-sm text-slate-600 mt-1">
                  {completedWellness} activities logged today
                </p>
              </div>
              <Button
                onClick={() => setShowAddWellness(true)}
                className="bg-green-600 hover:bg-green-700 text-white font-semibold py-2 px-4 rounded-lg flex items-center gap-2"
              >
                <Plus className="w-5 h-5" />
                Log Activity
              </Button>
            </div>

            {/* Add Wellness Modal */}
            {showAddWellness && (
              <Card className="p-6 mb-6 border-2 border-green-500 bg-green-50">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="font-semibold text-slate-900">Log Wellness Activity</h3>
                  <button
                    onClick={() => setShowAddWellness(false)}
                    className="text-slate-600 hover:text-slate-900"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
                <div className="space-y-4">
                  <select
                    value={newWellness.name}
                    onChange={(e) => setNewWellness({ ...newWellness, name: e.target.value })}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
                  >
                    <option value="">Select an activity...</option>
                    <option value="Meditation">🧘 Meditation</option>
                    <option value="Yoga">🤸 Yoga</option>
                    <option value="Walking">🚶 Walking</option>
                    <option value="Reading">📚 Reading</option>
                    <option value="Sports">⚽ Sports</option>
                    <option value="Drawing">🎨 Drawing</option>
                  </select>
                  <select
                    value={newWellness.duration}
                    onChange={(e) => setNewWellness({ ...newWellness, duration: e.target.value })}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
                  >
                    <option value="5 min">5 minutes</option>
                    <option value="10 min">10 minutes</option>
                    <option value="15 min">15 minutes</option>
                    <option value="20 min">20 minutes</option>
                    <option value="30 min">30 minutes</option>
                    <option value="45 min">45 minutes</option>
                    <option value="1 hour">1 hour</option>
                  </select>
                  <div className="flex gap-2">
                    <Button
                      onClick={handleAddWellness}
                      className="flex-1 bg-green-600 hover:bg-green-700 text-white font-semibold py-2 rounded-lg"
                    >
                      Log Activity
                    </Button>
                    <Button
                      onClick={() => setShowAddWellness(false)}
                      className="flex-1 bg-slate-300 hover:bg-slate-400 text-slate-900 font-semibold py-2 rounded-lg"
                    >
                      Cancel
                    </Button>
                  </div>
                </div>
              </Card>
            )}

            {/* Wellness List */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {wellness.map((activity) => (
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
      </main>
    </div>
  );
}
