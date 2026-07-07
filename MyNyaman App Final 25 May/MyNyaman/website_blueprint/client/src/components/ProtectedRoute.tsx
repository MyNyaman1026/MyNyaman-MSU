import { useLocation } from "wouter";
import { useEffect } from "react";
import { useRole, UserRole } from "@/contexts/RoleContext";
import { Card } from "@/components/ui/card";
import { AlertCircle } from "lucide-react";

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles: UserRole[];
  redirectTo?: string;
}

export function ProtectedRoute({
  children,
  allowedRoles,
  redirectTo = "/",
}: ProtectedRouteProps) {
  const [, navigate] = useLocation();
  const { role } = useRole();

  useEffect(() => {
    if (role === null || !allowedRoles.includes(role)) {
      // Redirect based on current role
      if (role === "child") {
        navigate("/child/signin");
      } else if (role === "parent") {
        navigate("/dashboard/parent");
      } else if (role === "consultant") {
        navigate("/consultant-view");
      } else {
        navigate(redirectTo);
      }
    }
  }, [role, allowedRoles, navigate, redirectTo]);

  // Show access denied if role doesn't match
  if (role === null || !allowedRoles.includes(role)) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-100 p-4">
        <Card className="p-8 max-w-md">
          <div className="flex items-center justify-center w-12 h-12 bg-red-100 rounded-lg mb-4">
            <AlertCircle className="w-6 h-6 text-red-600" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900 mb-2">Access Denied</h1>
          <p className="text-slate-600 mb-6">
            You don't have permission to access this page. Please log in with the correct account.
          </p>
          <button
            onClick={() => navigate("/")}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 rounded-lg transition"
          >
            Go to Home
          </button>
        </Card>
      </div>
    );
  }

  return <>{children}</>;
}
