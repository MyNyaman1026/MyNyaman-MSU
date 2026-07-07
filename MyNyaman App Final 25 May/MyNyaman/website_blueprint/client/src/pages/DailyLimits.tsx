import { useState } from "react";
import { useLocation } from "wouter";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ArrowLeft, Clock, Save } from "lucide-react";
import { useChild } from "@/contexts/ChildContext";

export default function DailyLimits() {
  const [, navigate] = useLocation();
  const { selectedChild } = useChild();
  const [limits, setLimits] = useState({
    weekday: "03:00",
    weekend: "04:00",
    bedtime: "22:00",
  });

  const handleSave = () => {
    alert("Daily limits saved successfully!");
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-indigo-50 pb-8">
      {/* Header */}
      <div className="bg-white border-b border-slate-200 sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate("/dashboard/parent")}
              className="p-2 hover:bg-slate-100 rounded-lg transition"
            >
              <ArrowLeft className="w-6 h-6 text-slate-600" />
            </button>
            <div>
              <h1 className="text-2xl font-bold text-slate-900">
                Daily Limits {selectedChild && `- ${selectedChild.name}`}
              </h1>
              <p className="text-sm text-slate-600">Set screen time limits for your children</p>
            </div>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Weekday Limit */}
          <Card className="p-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                <Clock className="w-5 h-5 text-blue-600" />
              </div>
              <h3 className="font-semibold text-slate-900">Weekday Limit</h3>
            </div>
            <div className="space-y-4">
              <div>
                <label className="text-sm font-medium text-slate-700">Monday - Friday</label>
                <input
                  type="time"
                  value={limits.weekday}
                  onChange={(e) => setLimits({ ...limits, weekday: e.target.value })}
                  className="w-full mt-2 px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-600 focus:border-transparent"
                />
              </div>
              <p className="text-sm text-slate-600">Maximum screen time on school days</p>
            </div>
          </Card>

          {/* Weekend Limit */}
          <Card className="p-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
                <Clock className="w-5 h-5 text-green-600" />
              </div>
              <h3 className="font-semibold text-slate-900">Weekend Limit</h3>
            </div>
            <div className="space-y-4">
              <div>
                <label className="text-sm font-medium text-slate-700">Saturday - Sunday</label>
                <input
                  type="time"
                  value={limits.weekend}
                  onChange={(e) => setLimits({ ...limits, weekend: e.target.value })}
                  className="w-full mt-2 px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-green-600 focus:border-transparent"
                />
              </div>
              <p className="text-sm text-slate-600">Maximum screen time on weekends</p>
            </div>
          </Card>

          {/* Bedtime */}
          <Card className="p-6 md:col-span-2">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
                <Clock className="w-5 h-5 text-purple-600" />
              </div>
              <h3 className="font-semibold text-slate-900">Bedtime</h3>
            </div>
            <div className="space-y-4">
              <div>
                <label className="text-sm font-medium text-slate-700">Device-free time</label>
                <input
                  type="time"
                  value={limits.bedtime}
                  onChange={(e) => setLimits({ ...limits, bedtime: e.target.value })}
                  className="w-full mt-2 px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-purple-600 focus:border-transparent"
                />
              </div>
              <p className="text-sm text-slate-600">Time when devices are automatically locked</p>
            </div>
          </Card>
        </div>

        {/* Save Button */}
        <div className="mt-8 flex justify-end gap-4">
          <Button
            variant="outline"
            onClick={() => navigate("/dashboard/parent")}
          >
            Cancel
          </Button>
          <Button
            onClick={handleSave}
            className="bg-blue-600 hover:bg-blue-700 text-white flex items-center gap-2"
          >
            <Save className="w-4 h-4" />
            Save Changes
          </Button>
        </div>
      </div>
    </div>
  );
}
