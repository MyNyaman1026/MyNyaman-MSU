import { useState } from "react";
import { useLocation } from "wouter";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ArrowLeft, BarChart3, TrendingUp, Clock } from "lucide-react";
import { useChild } from "@/contexts/ChildContext";
import { LineChart, Line, BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts";

const dailyData = [
  { day: "Mon", usage: 180 },
  { day: "Tue", usage: 220 },
  { day: "Wed", usage: 150 },
  { day: "Thu", usage: 280 },
  { day: "Fri", usage: 200 },
  { day: "Sat", usage: 320 },
  { day: "Sun", usage: 250 },
];

const appData = [
  { name: "YouTube", value: 120, color: "#ef4444" },
  { name: "TikTok", value: 95, color: "#3b82f6" },
  { name: "Instagram", value: 80, color: "#ec4899" },
  { name: "Gaming", value: 60, color: "#f59e0b" },
  { name: "Others", value: 45, color: "#10b981" },
];

const appUsage = [
  { app: "YouTube", time: "2h 15m", percentage: 45 },
  { app: "TikTok", time: "1h 45m", percentage: 35 },
  { app: "Instagram", time: "1h 00m", percentage: 20 },
];

export default function UsageReports() {
  const [, navigate] = useLocation();
  const [period, setPeriod] = useState("week");
  const { selectedChild } = useChild();

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-50 via-white to-emerald-50 pb-8">
      {/* Header */}
      <div className="bg-white border-b border-slate-200 sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => navigate("/dashboard/parent")}
                className="p-2 hover:bg-slate-100 rounded-lg transition"
              >
                <ArrowLeft className="w-6 h-6 text-slate-600" />
              </button>
              <div>
                <h1 className="text-2xl font-bold text-slate-900">
                  Usage Reports {selectedChild && `- ${selectedChild.name}`}
                </h1>
                <p className="text-sm text-slate-600">View app usage and screen time analytics</p>
              </div>
            </div>
            <div className="flex gap-2">
              {["day", "week", "month"].map((p) => (
                <Button
                  key={p}
                  variant={period === p ? "default" : "outline"}
                  onClick={() => setPeriod(p)}
                  className="capitalize"
                >
                  {p}
                </Button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Summary Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
          <Card className="p-6 bg-gradient-to-br from-blue-50 to-blue-100">
            <div className="flex items-center justify-between mb-2">
              <h3 className="font-semibold text-slate-900">Total Screen Time</h3>
              <Clock className="w-5 h-5 text-blue-600" />
            </div>
            <p className="text-3xl font-bold text-blue-600">15h 30m</p>
            <p className="text-sm text-slate-600 mt-2">This week</p>
          </Card>

          <Card className="p-6 bg-gradient-to-br from-green-50 to-green-100">
            <div className="flex items-center justify-between mb-2">
              <h3 className="font-semibold text-slate-900">Daily Average</h3>
              <TrendingUp className="w-5 h-5 text-green-600" />
            </div>
            <p className="text-3xl font-bold text-green-600">2h 12m</p>
            <p className="text-sm text-slate-600 mt-2">Per day</p>
          </Card>

          <Card className="p-6 bg-gradient-to-br from-purple-50 to-purple-100">
            <div className="flex items-center justify-between mb-2">
              <h3 className="font-semibold text-slate-900">Most Used App</h3>
              <BarChart3 className="w-5 h-5 text-purple-600" />
            </div>
            <p className="text-3xl font-bold text-purple-600">YouTube</p>
            <p className="text-sm text-slate-600 mt-2">2h 15m today</p>
          </Card>
        </div>

        {/* Charts */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
          {/* Daily Usage Chart */}
          <Card className="p-6">
            <h3 className="font-semibold text-slate-900 mb-4">Daily Usage</h3>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={dailyData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="day" />
                <YAxis />
                <Tooltip />
                <Line
                  type="monotone"
                  dataKey="usage"
                  stroke="#3b82f6"
                  strokeWidth={2}
                  dot={{ fill: "#3b82f6" }}
                />
              </LineChart>
            </ResponsiveContainer>
          </Card>

          {/* App Usage Pie Chart */}
          <Card className="p-6">
            <h3 className="font-semibold text-slate-900 mb-4">App Distribution</h3>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={appData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {appData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </Card>
        </div>

        {/* App Usage Table */}
        <Card className="p-6">
          <h3 className="font-semibold text-slate-900 mb-4">Top Apps</h3>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-slate-200">
                  <th className="text-left py-3 px-4 font-semibold text-slate-900">App Name</th>
                  <th className="text-left py-3 px-4 font-semibold text-slate-900">Usage Time</th>
                  <th className="text-left py-3 px-4 font-semibold text-slate-900">Percentage</th>
                  <th className="text-left py-3 px-4 font-semibold text-slate-900">Progress</th>
                </tr>
              </thead>
              <tbody>
                {appUsage.map((app, idx) => (
                  <tr key={idx} className="border-b border-slate-100 hover:bg-slate-50">
                    <td className="py-3 px-4 text-slate-900">{app.app}</td>
                    <td className="py-3 px-4 text-slate-600">{app.time}</td>
                    <td className="py-3 px-4 text-slate-600">{app.percentage}%</td>
                    <td className="py-3 px-4">
                      <div className="w-full bg-slate-200 rounded-full h-2">
                        <div
                          className="bg-blue-600 h-2 rounded-full"
                          style={{ width: `${app.percentage}%` }}
                        ></div>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      </div>
    </div>
  );
}
