import { useState } from "react";
import { useLocation } from "wouter";
import {
  Clock,
  BarChart3,
  MessageSquare,
  Lock,
  User,
  Settings,
  Menu,
  X,
  LogOut,
  Home,
} from "lucide-react";
import { useAuth } from "@/_core/hooks/useAuth";
import ChildSelector from "./ChildSelector";
import { useChild } from "@/contexts/ChildContext";

interface SidebarProps {
  role: "parent" | "child" | "consultant";
  onLogout?: () => void;
}

export default function Sidebar({ role, onLogout }: SidebarProps) {
  const [isOpen, setIsOpen] = useState(true);
  const [location] = useLocation();
  const [, navigate] = useLocation();
  const { user } = useAuth();
  const { children } = useChild();

  const parentMenuItems = [
    { icon: Home, label: "Dashboard", path: "/dashboard/parent" },
    { icon: Home, label: "View Child", path: "/dashboard/child-view" },
    { icon: Home, label: "Manage Children", path: "/manage-children" },
    { icon: Clock, label: "Daily Limits", path: "/daily-limits" },
    { icon: BarChart3, label: "Usage Reports", path: "/usage-reports" },
    { icon: MessageSquare, label: "Chat", path: "/chat" },
    { icon: Lock, label: "App Blocking", path: "/app-blocking" },
    { icon: User, label: "Profile", path: "/profile" },
    { icon: Settings, label: "Settings", path: "/settings" },
  ];

  const childMenuItems = [
    { icon: Home, label: "Dashboard", path: "/dashboard/child" },
    { icon: BarChart3, label: "Usage Stats", path: "/usage-reports" },
    { icon: MessageSquare, label: "Chat", path: "/chat" },
    { icon: User, label: "Profile", path: "/profile" },
    { icon: Settings, label: "Settings", path: "/settings" },
  ];

  const consultantMenuItems = [
    { icon: Home, label: "Dashboard", path: "/consultant-view" },
    { icon: MessageSquare, label: "Chat", path: "/chat" },
    { icon: User, label: "Profile", path: "/profile" },
    { icon: Settings, label: "Settings", path: "/settings" },
  ];

  const menuItems =
    role === "parent"
      ? parentMenuItems
      : role === "child"
        ? childMenuItems
        : consultantMenuItems;

  const handleLogout = () => {
    if (onLogout) {
      onLogout();
    }
    localStorage.removeItem("userRole");
    navigate("/signin");
  };

  return (
    <>
      {/* Mobile Toggle Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="fixed top-4 left-4 z-50 md:hidden bg-blue-600 text-white p-2 rounded-lg hover:bg-blue-700 transition"
        aria-label="Toggle sidebar"
      >
        {isOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
      </button>

      {/* Sidebar */}
      <aside
        className={`fixed left-0 top-0 h-screen bg-gradient-to-b from-blue-600 to-blue-700 text-white transition-all duration-300 z-40 ${
          isOpen ? "w-64" : "w-0"
        } md:w-64 overflow-hidden`}
      >
        <div className="p-6">
          {/* Logo */}
          <div className="flex items-center gap-2 mb-8">
            <img src="https://d2xsxph8kpxj0f.cloudfront.net/310519663413690608/N2GZiDPfbafCV4EzEdoGhQ/MyNyamanPrimaryLogo_6cbc355f.webp" alt="MyNyaman" className="h-12 w-auto" />
          </div>

          {/* User Info */}
          <div className="mb-8 pb-6 border-b border-blue-500">
            <p className="text-sm text-blue-100">Welcome,</p>
            <p className="font-semibold truncate">{user?.email || "User"}</p>
            <p className="text-xs text-blue-200 capitalize mt-1">{role} Account</p>
          </div>

          {/* Child Selector for Parents */}
          {role === "parent" && children.length > 0 && (
            <div className="mb-6">
              <ChildSelector />
            </div>
          )}

          {/* Navigation Menu */}
          <nav className="space-y-2">
            {menuItems.map((item) => (
              <button
                key={item.path}
                onClick={() => {
                  navigate(item.path);
                  setIsOpen(false); // Close sidebar on mobile after click
                }}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition text-left text-sm font-medium ${
                  location === item.path
                    ? "bg-blue-500 font-bold"
                    : "hover:bg-blue-500"
                }`}
              >
                <item.icon className="w-5 h-5" />
                <span>{item.label}</span>
              </button>
            ))}
          </nav>

          {/* Logout Button */}
          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-3 px-4 py-3 rounded-lg hover:bg-red-500 transition text-left text-sm font-medium mt-8 border-t border-blue-500 pt-6"
          >
            <LogOut className="w-5 h-5" />
            <span>Logout</span>
          </button>
        </div>
      </aside>

      {/* Overlay for mobile */}
      {isOpen && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 z-30 md:hidden"
          onClick={() => setIsOpen(false)}
          aria-hidden="true"
        />
      )}
    </>
  );
}
