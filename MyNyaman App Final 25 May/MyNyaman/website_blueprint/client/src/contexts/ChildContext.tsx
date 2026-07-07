import React, { createContext, useContext, useState, useEffect } from "react";

export interface Child {
  id: string;
  name: string;
  email: string;
  phone?: string;
  ageGroup: string;
  code?: string;
  expiresAt?: Date;
  status?: "active" | "inactive" | "expired" | "used";
}

interface ChildContextType {
  selectedChild: Child | null;
  setSelectedChild: (child: Child | null) => void;
  children: Child[];
  setChildren: (children: Child[]) => void;
  addChild: (child: Child) => void;
  removeChild: (childId: string) => void;
}

const ChildContext = createContext<ChildContextType | undefined>(undefined);

export function ChildProvider({ children }: { children: React.ReactNode }) {
  const [selectedChild, setSelectedChild] = useState<Child | null>(null);
  const [childrenList, setChildren] = useState<Child[]>([]);

  // Load from localStorage on mount
  useEffect(() => {
    const savedChild = localStorage.getItem("selectedChild");
    if (savedChild) {
      try {
        setSelectedChild(JSON.parse(savedChild));
      } catch (error) {
        console.error("Failed to parse saved child:", error);
      }
    }

    const savedChildren = localStorage.getItem("childrenList");
    if (savedChildren) {
      try {
        setChildren(JSON.parse(savedChildren));
      } catch (error) {
        console.error("Failed to parse children list:", error);
      }
    }
  }, []);

  // Save selected child to localStorage
  useEffect(() => {
    if (selectedChild) {
      localStorage.setItem("selectedChild", JSON.stringify(selectedChild));
    } else {
      localStorage.removeItem("selectedChild");
    }
  }, [selectedChild]);

  // Save children list to localStorage
  useEffect(() => {
    if (childrenList.length > 0) {
      localStorage.setItem("childrenList", JSON.stringify(childrenList));
    }
  }, [childrenList]);

  const addChild = (child: Child) => {
    setChildren((prev) => {
      const exists = prev.some((c) => c.id === child.id);
      if (exists) return prev;
      return [...prev, child];
    });
  };

  const removeChild = (childId: string) => {
    setChildren((prev) => prev.filter((c) => c.id !== childId));
    if (selectedChild?.id === childId) {
      setSelectedChild(null);
    }
  };

  const value: ChildContextType = {
    selectedChild,
    setSelectedChild,
    children: childrenList,
    setChildren,
    addChild,
    removeChild,
  };

  return (
    <ChildContext.Provider value={value}>{children}</ChildContext.Provider>
  );
}

export function useChild() {
  const context = useContext(ChildContext);
  if (context === undefined) {
    throw new Error("useChild must be used within a ChildProvider");
  }
  return context;
}
