import {
  createContext,
  useContext,
  ReactNode,
  useState,
  useEffect,
} from "react";
import { trpc } from "@/lib/trpc";
import { useFirebaseAuth } from "./FirebaseAuthContext";
export type UserRole = "parent" | "child" | "consultant" | null;

export interface AuthUser {
  id: number;
  openId: string;
  name: string | null;
  email: string | null;
  role: Exclude<UserRole, null>;
}

interface RoleContextType {
  user: AuthUser | null;
  role: UserRole;
  loading: boolean;

  setUser: (user: AuthUser | null) => void;
  clearUser: () => void;

  isChild: boolean;
  isParent: boolean;
  isConsultant: boolean;
}

const RoleContext = createContext<RoleContextType | undefined>(undefined);

export function RoleProvider({ children }: { children: ReactNode }) {
  // Get role from localStorage or session


  const [user, setUser] = useState<AuthUser | null>(null);



  const { user: firebaseUser } = useFirebaseAuth();

  const { data, isLoading } =
    trpc.registration.getUserByFirebaseUid.useQuery(
      {
        firebaseUid: firebaseUser?.uid ?? "",
      },
      {
        enabled: !!firebaseUser,
      }
    );

  const loading = !!firebaseUser && isLoading;

  useEffect(() => {
    console.log("[RoleContext] Firebase user:", firebaseUser?.uid);
    console.log("[RoleContext] Loading:", isLoading);
    console.log("[RoleContext] Database user:", data);

    if (!firebaseUser) {
      setUser(null);
      return;
    }

    if (data) {
      setUser({
        id: data.id,
        openId: data.openId,
        name: data.name,
        email: data.email,
        role: data.role as "parent" | "child" | "consultant",
      });
    } else if (!isLoading) {
      setUser(null);
    }
  }, [firebaseUser, data, isLoading]);


  const role: UserRole = user?.role ?? null;

  const value: RoleContextType = {
    user,
    role,
    loading,

    setUser,

    clearUser: () => {
      setUser(null);
    },

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
