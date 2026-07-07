import { useLocation } from "wouter";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ArrowLeft, Home, BarChart3, Lock, Clock, MessageSquare, User, Settings, HelpCircle } from "lucide-react";

export default function Menu() {
  const [, navigate] = useLocation();

  const menuItems = [
    { icon: Home, label: "Dashboard", path: "/dashboard/parent" },
    { icon: BarChart3, label: "Usage Reports", path: "/usage-reports" },
    { icon: Lock, label: "App Blocking", path: "/app-blocking" },
    { icon: Clock, label: "Daily Limits", path: "/daily-limits" },
    { icon: MessageSquare, label: "Chat", path: "/chat" },
    { icon: User, label: "Profile", path: "/profile" },
    { icon: Settings, label: "Settings", path: "/settings" },
    { icon: HelpCircle, label: "Help & Support", path: "#" },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-100">
      <div className="bg-white border-b border-slate-200 sticky top-0 z-40">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center gap-4">
            <button onClick={() => navigate("/dashboard/parent")} className="p-2 hover:bg-slate-100 rounded-lg transition">
              <ArrowLeft className="w-6 h-6 text-slate-600" />
            </button>
            <h1 className="text-2xl font-bold text-slate-900">Menu</h1>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {menuItems.map((item, idx) => {
            const Icon = item.icon;
            return (
              <Card key={idx} className="p-6 hover:shadow-lg transition-all cursor-pointer">
                <button onClick={() => navigate(item.path)} className="w-full text-left">
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
                      <Icon className="w-6 h-6 text-blue-600" />
                    </div>
                    <span className="font-semibold text-slate-900">{item.label}</span>
                  </div>
                </button>
              </Card>
            );
          })}
        </div>
      </div>
    </div>
  );
}
