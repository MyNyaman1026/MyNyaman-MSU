import { useState } from "react";
import { useLocation } from "wouter";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ArrowLeft, Lock, Unlock, Plus, Search } from "lucide-react";
import { useChild } from "@/contexts/ChildContext";

interface BlockedApp {
  id: string;
  name: string;
  icon: string;
  isBlocked: boolean;
  category: string;
}

const initialApps: BlockedApp[] = [
  { id: "1", name: "YouTube", icon: "▶️", isBlocked: true, category: "Social Media" },
  { id: "2", name: "TikTok", icon: "🎵", isBlocked: true, category: "Social Media" },
  { id: "3", name: "Instagram", icon: "📷", isBlocked: false, category: "Social Media" },
  { id: "4", name: "Snapchat", icon: "👻", isBlocked: true, category: "Social Media" },
  { id: "5", name: "Discord", icon: "💬", isBlocked: false, category: "Communication" },
  { id: "6", name: "Gaming App", icon: "🎮", isBlocked: false, category: "Gaming" },
  { id: "7", name: "Reddit", icon: "🔴", isBlocked: true, category: "Social Media" },
  { id: "8", name: "Twitch", icon: "📺", isBlocked: false, category: "Entertainment" },
];

export default function AppBlocking() {
  const [, navigate] = useLocation();
  const { selectedChild } = useChild();
  const [apps, setApps] = useState(initialApps);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("All");

  const categories = ["All", ...Array.from(new Set(apps.map((app) => app.category)))];

  const filteredApps = apps.filter((app) => {
    const matchesSearch = app.name.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory = selectedCategory === "All" || app.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  const toggleBlock = (id: string) => {
    setApps(apps.map((app) => (app.id === id ? { ...app, isBlocked: !app.isBlocked } : app)));
  };

  const blockedCount = apps.filter((app) => app.isBlocked).length;

  return (
    <div className="min-h-screen bg-gradient-to-br from-red-50 via-white to-pink-50 pb-8">
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
                App Blocking {selectedChild && `- ${selectedChild.name}`}
              </h1>
              <p className="text-sm text-slate-600">{blockedCount} apps blocked</p>
            </div>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Search and Filters */}
        <Card className="p-6 mb-8">
          <div className="space-y-4">
            {/* Search Bar */}
            <div className="relative">
              <Search className="absolute left-3 top-3 w-5 h-5 text-slate-400" />
              <input
                type="text"
                placeholder="Search apps..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-red-600 focus:border-transparent"
              />
            </div>

            {/* Category Filter */}
            <div className="flex flex-wrap gap-2">
              {(categories as string[]).map((cat) => (
                <Button
                  key={cat}
                  variant={selectedCategory === cat ? "default" : "outline"}
                  onClick={() => setSelectedCategory(cat)}
                  className="text-sm"
                >
                  {cat}
                </Button>
              ))}
            </div>
          </div>
        </Card>

        {/* Apps Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredApps.map((app) => (
            <Card
              key={app.id}
              className={`p-6 transition-all ${
                app.isBlocked
                  ? "bg-gradient-to-br from-red-50 to-red-100 border-red-200"
                  : "bg-gradient-to-br from-green-50 to-green-100 border-green-200"
              }`}
            >
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center gap-3">
                  <div className="text-3xl">{app.icon}</div>
                  <div>
                    <h3 className="font-semibold text-slate-900">{app.name}</h3>
                    <p className="text-xs text-slate-600">{app.category}</p>
                  </div>
                </div>
                <button
                  onClick={() => toggleBlock(app.id)}
                  className={`p-2 rounded-lg transition ${
                    app.isBlocked
                      ? "bg-red-200 hover:bg-red-300 text-red-700"
                      : "bg-green-200 hover:bg-green-300 text-green-700"
                  }`}
                >
                  {app.isBlocked ? (
                    <Lock className="w-5 h-5" />
                  ) : (
                    <Unlock className="w-5 h-5" />
                  )}
                </button>
              </div>

              <div className="flex items-center justify-between">
                <span
                  className={`text-sm font-semibold ${
                    app.isBlocked ? "text-red-600" : "text-green-600"
                  }`}
                >
                  {app.isBlocked ? "Blocked" : "Allowed"}
                </span>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => toggleBlock(app.id)}
                  className="text-xs"
                >
                  {app.isBlocked ? "Unblock" : "Block"}
                </Button>
              </div>
            </Card>
          ))}
        </div>

        {/* Add App Button */}
        <div className="mt-8 flex justify-center">
          <Button className="bg-blue-600 hover:bg-blue-700 text-white flex items-center gap-2">
            <Plus className="w-4 h-4" />
            Add App to Block
          </Button>
        </div>

        {/* Summary Card */}
        <Card className="mt-8 p-6 bg-gradient-to-r from-slate-50 to-slate-100">
          <h3 className="font-semibold text-slate-900 mb-3">Blocking Summary</h3>
          <div className="grid grid-cols-3 gap-4">
            <div>
              <p className="text-2xl font-bold text-red-600">{blockedCount}</p>
              <p className="text-sm text-slate-600">Apps Blocked</p>
            </div>
            <div>
              <p className="text-2xl font-bold text-green-600">{apps.length - blockedCount}</p>
              <p className="text-sm text-slate-600">Apps Allowed</p>
            </div>
            <div>
              <p className="text-2xl font-bold text-blue-600">{apps.length}</p>
              <p className="text-sm text-slate-600">Total Apps</p>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
