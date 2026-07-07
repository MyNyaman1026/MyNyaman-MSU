import { useLocation } from "wouter";
import { LayoutDashboard, CheckSquare, Heart, MessageSquare, User, LogOut } from "lucide-react";

export default function ChildSidebar() {
  const [location, navigate] = useLocation();

  const menuItems = [
    { icon: LayoutDashboard, label: "Dashboard", path: "/dashboard/child" },
    { icon: CheckSquare, label: "Tasks", path: "/child/tasks" },
    { icon: Heart, label: "Wellness", path: "/child/wellness" },
    { icon: MessageSquare, label: "Chat", path: "/chat" },
    { icon: User, label: "Profile", path: "/child/profile" },
  ];

  const handleLogout = () => {
    localStorage.removeItem("childSession");
    navigate("/child/signin");
  };

  return (
    <aside className="fixed left-0 top-0 h-screen w-64 bg-gradient-to-b from-blue-600 to-green-600 text-white shadow-lg hidden md:flex flex-col z-40">
      {/* Logo */}
      <div className="p-6 border-b border-white/20">
        <div className="flex items-center gap-2">
          <img src="https://d2xsxph8kpxj0f.cloudfront.net/310519663413690608/N2GZiDPfbafCV4EzEdoGhQ/MyNyamanPrimaryLogo_6cbc355f.webp" alt="MyNyaman" className="h-10 w-auto" />
        </div>
      </div>

      {/* Navigation Menu */}
      <nav className="flex-1 px-4 py-6 space-y-2">
        {menuItems.map((item) => (
          <button
            key={item.path}
            onClick={() => navigate(item.path)}
            className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition ${
              location === item.path
                ? "bg-white/20 text-white font-semibold"
                : "text-blue-100 hover:bg-white/10"
            }`}
          >
            <item.icon className="w-5 h-5" />
            <span>{item.label}</span>
          </button>
        ))}
      </nav>

      {/* Logout Button */}
      <div className="p-4 border-t border-white/20">
        <button
          onClick={handleLogout}
          className="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-blue-100 hover:bg-white/10 transition"
        >
          <LogOut className="w-5 h-5" />
          <span>Sign Out</span>
        </button>
      </div>
    </aside>
  );
}
