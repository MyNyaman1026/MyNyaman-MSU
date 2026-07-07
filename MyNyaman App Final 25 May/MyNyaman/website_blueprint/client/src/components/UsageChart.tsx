import { BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell } from "recharts";
import { Card } from "@/components/ui/card";

interface UsageData {
  date: string;
  screenTime: number;
  appCount: number;
}

interface AppUsageData {
  appName: string;
  usageTime: number;
}

interface UsageChartProps {
  data?: UsageData[];
  appData?: AppUsageData[];
  title?: string;
  type?: "line" | "bar" | "pie";
}

const COLORS = ["#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899"];

export default function UsageChart({
  data = [],
  appData = [],
  title = "Usage Statistics",
  type = "line",
}: UsageChartProps) {
  if (data.length === 0 && appData.length === 0) {
    return (
      <Card className="p-6">
        <h3 className="text-lg font-semibold text-slate-900 mb-4">{title}</h3>
        <div className="flex items-center justify-center h-64 bg-slate-50 rounded-lg">
          <p className="text-slate-500">No data available</p>
        </div>
      </Card>
    );
  }

  return (
    <Card className="p-6">
      <h3 className="text-lg font-semibold text-slate-900 mb-4">{title}</h3>

      {type === "line" && data.length > 0 && (
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Line
              type="monotone"
              dataKey="screenTime"
              stroke="#3b82f6"
              name="Screen Time (min)"
              dot={{ fill: "#3b82f6", r: 4 }}
            />
          </LineChart>
        </ResponsiveContainer>
      )}

      {type === "bar" && data.length > 0 && (
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Bar dataKey="screenTime" fill="#3b82f6" name="Screen Time (min)" />
            <Bar dataKey="appCount" fill="#10b981" name="Apps Used" />
          </BarChart>
        </ResponsiveContainer>
      )}

      {type === "pie" && appData.length > 0 && (
        <ResponsiveContainer width="100%" height={300}>
          <PieChart>
            <Pie
              data={appData}
              cx="50%"
              cy="50%"
              labelLine={false}
              label={({ appName, usageTime }) => `${appName}: ${usageTime}m`}
              outerRadius={80}
              fill="#8884d8"
              dataKey="usageTime"
            >
              {appData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip formatter={(value) => `${value} min`} />
          </PieChart>
        </ResponsiveContainer>
      )}

      {appData.length > 0 && type !== "pie" && (
        <div className="mt-6">
          <h4 className="font-semibold text-slate-900 mb-3">App Usage Breakdown</h4>
          <div className="space-y-2">
            {appData.map((app, index) => (
              <div key={app.appName} className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div
                    className="w-3 h-3 rounded-full"
                    style={{ backgroundColor: COLORS[index % COLORS.length] }}
                  />
                  <span className="text-sm text-slate-700">{app.appName}</span>
                </div>
                <span className="text-sm font-medium text-slate-900">
                  {app.usageTime} min
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </Card>
  );
}
