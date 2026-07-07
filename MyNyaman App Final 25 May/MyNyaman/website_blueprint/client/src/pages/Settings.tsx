import { useLocation } from "wouter";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ArrowLeft, Bell, Lock, Eye, LogOut } from "lucide-react";

export default function Settings() {
  const [, navigate] = useLocation();

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-100 pb-8">
      <div className="bg-white border-b border-slate-200 sticky top-0 z-40">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center gap-4">
            <button onClick={() => navigate("/dashboard/parent")} className="p-2 hover:bg-slate-100 rounded-lg transition">
              <ArrowLeft className="w-6 h-6 text-slate-600" />
            </button>
            <h1 className="text-2xl font-bold text-slate-900">Settings</h1>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="space-y-4">
          <Card className="p-6 hover:shadow-lg transition-all cursor-pointer">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <Bell className="w-6 h-6 text-blue-600" />
                <div>
                  <h3 className="font-semibold text-slate-900">Notifications</h3>
                  <p className="text-sm text-slate-600">Manage notification preferences</p>
                </div>
              </div>
              <Button variant="ghost">→</Button>
            </div>
          </Card>

          <Card className="p-6 hover:shadow-lg transition-all cursor-pointer">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <Lock className="w-6 h-6 text-green-600" />
                <div>
                  <h3 className="font-semibold text-slate-900">Privacy & Security</h3>
                  <p className="text-sm text-slate-600">Control your privacy settings</p>
                </div>
              </div>
              <Button variant="ghost">→</Button>
            </div>
          </Card>

          <Card className="p-6 hover:shadow-lg transition-all cursor-pointer">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <Eye className="w-6 h-6 text-purple-600" />
                <div>
                  <h3 className="font-semibold text-slate-900">Appearance</h3>
                  <p className="text-sm text-slate-600">Customize your theme</p>
                </div>
              </div>
              <Button variant="ghost">→</Button>
            </div>
          </Card>

          <Card className="p-6 hover:shadow-lg transition-all cursor-pointer border-red-200">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <LogOut className="w-6 h-6 text-red-600" />
                <div>
                  <h3 className="font-semibold text-slate-900">Logout</h3>
                  <p className="text-sm text-slate-600">Sign out from your account</p>
                </div>
              </div>
              <Button variant="ghost" className="text-red-600">→</Button>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
