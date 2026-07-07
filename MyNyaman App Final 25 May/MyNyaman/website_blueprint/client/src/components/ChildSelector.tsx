import { useState } from "react";
import { ChevronDown, User } from "lucide-react";
import { useChild, type Child } from "@/contexts/ChildContext";

export default function ChildSelector() {
  const { selectedChild, setSelectedChild, children } = useChild();
  const [isOpen, setIsOpen] = useState(false);

  if (children.length === 0) {
    return null;
  }

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="w-full px-4 py-3 bg-white border border-slate-200 rounded-lg hover:bg-slate-50 transition flex items-center justify-between"
      >
        <div className="flex items-center gap-2 text-left">
          <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
            <User className="w-4 h-4 text-blue-600" />
          </div>
          <div>
            <p className="text-xs text-slate-500">Viewing</p>
            <p className="text-sm font-semibold text-slate-900">
              {selectedChild?.name || "Select Child"}
            </p>
          </div>
        </div>
        <ChevronDown
          className={`w-4 h-4 text-slate-600 transition ${
            isOpen ? "rotate-180" : ""
          }`}
        />
      </button>

      {/* Dropdown */}
      {isOpen && (
        <div className="absolute top-full left-0 right-0 mt-2 bg-white border border-slate-200 rounded-lg shadow-lg z-50">
          {children.map((child) => (
            <button
              key={child.id}
              onClick={() => {
                setSelectedChild(child);
                setIsOpen(false);
              }}
              className={`w-full px-4 py-3 text-left hover:bg-blue-50 transition border-b border-slate-100 last:border-b-0 ${
                selectedChild?.id === child.id ? "bg-blue-50" : ""
              }`}
            >
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
                  <User className="w-4 h-4 text-blue-600" />
                </div>
                <div>
                  <p className="font-medium text-slate-900">{child.name}</p>
                  <p className="text-xs text-slate-500">{child.ageGroup}</p>
                </div>
                {selectedChild?.id === child.id && (
                  <div className="ml-auto w-2 h-2 bg-blue-600 rounded-full" />
                )}
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
