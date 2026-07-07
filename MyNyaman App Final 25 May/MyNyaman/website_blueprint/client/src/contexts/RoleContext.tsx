import { createContext, useContext, ReactNode, useState } from "react";

export type UserRole = "parent" | "child" | "consultant" | null;

interface RoleContextType {
  role: UserRole;
  setRole: (role: UserRole) => void;
  isChild: boolean;
  isParent: boolean;
  isConsultant: boolean;
}

const RoleContext = createContext<RoleContextType | undefined>(undefined);

export function RoleProvider({ children }: { children: ReactNode }) {
  // Get role from localStorage or session
  const getInitialRole = (): UserRole => {
    try {
      const childSession = localStorage.getItem("childSession");
      if (childSession) {
        return "child";
      }
      const parentSession = localStorage.getItem("parentSession");
      if (parentSession) {
        return "parent";
      }
      const consultantSession = localStorage.getItem("consultantSession");
      if (consultantSession) {
        return "consultant";
      }
    } catch (err) {
      console.error("Error reading session:", err);
    }
    return null;
  };

  const [role, setRole] = useState<UserRole>(getInitialRole());

  const value: RoleContextType = {
    role,
    setRole,
    isChild: role === "child",
    isParent: role === "parent",
    isConsultant: role === "consultant",
  };

  return <RoleContext.Provider value={value}>{children}</RoleContext.Provider>;
}

export function useRole() {
  const context = useContext(RoleContext);
  if (context === undefined) {
    throw new Error("useRole must be used within RoleProvider");
  }
  return context;
}

// Helper hook for route guards
export function useRequireRole(allowedRoles: UserRole[]) {
  const { role } = useRole();
  return allowedRoles.includes(role);
}

// Helper hook to check if user can access a route
export function useCanAccessRoute(requiredRole: UserRole | UserRole[]) {
  const { role } = useRole();
  const roles = Array.isArray(requiredRole) ? requiredRole : [requiredRole];
  return roles.includes(role);
}
