import { useChild } from "@/contexts/ChildContext";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Users, ArrowRight } from "lucide-react";

export default function ChildSelectorCards() {
  const { children, selectedChild, setSelectedChild } = useChild();

  if (children.length === 0) {
    return (
      <div className="text-center py-12">
        <Users className="w-12 h-12 text-slate-300 mx-auto mb-4" />
        <p className="text-slate-600">No children linked yet</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      {children.map((child) => (
        <Card
          key={child.id}
          className={`p-6 cursor-pointer transition-all ${
            selectedChild?.id === child.id
              ? "ring-2 ring-blue-500 bg-blue-50"
              : "hover:shadow-lg"
          }`}
          onClick={() => setSelectedChild(child)}
        >
          <div className="flex items-start justify-between mb-4">
            <div className="flex-1">
              <h3 className="text-lg font-semibold text-slate-900">{child.name}</h3>
              <p className="text-sm text-slate-600">{child.ageGroup} years old</p>
            </div>
            {selectedChild?.id === child.id && (
              <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
            )}
          </div>

          <div className="space-y-2 mb-4">
            <p className="text-sm text-slate-600">
              <span className="font-medium">Email:</span> {child.email}
            </p>
            {child.phone && (
              <p className="text-sm text-slate-600">
                <span className="font-medium">Phone:</span> {child.phone}
              </p>
            )}
          </div>

          <div className="flex items-center justify-between pt-4 border-t border-slate-200">
            <span
              className={`text-xs font-medium px-2 py-1 rounded-full ${
                child.status === "active"
                  ? "bg-green-100 text-green-700"
                  : "bg-slate-100 text-slate-700"
              }`}
            >
              {child.status === "active" ? "Active" : "Inactive"}
            </span>
            {selectedChild?.id === child.id && (
              <Button
                size="sm"
                variant="ghost"
                className="text-blue-600 hover:text-blue-700"
              >
                <ArrowRight className="w-4 h-4" />
              </Button>
            )}
          </div>
        </Card>
      ))}
    </div>
  );
}
