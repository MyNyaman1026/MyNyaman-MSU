import { useState } from "react";
import { X, Loader2, AlertCircle, ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { useLocation } from "wouter";
import { toast } from "sonner";
import { trpc } from "@/lib/trpc";
import { useChild } from "@/contexts/ChildContext";

interface ChildAccessModalProps {
  isOpen: boolean;
  onClose: () => void;
}

interface ValidatedChild {
  id: string;
  name: string;
  email: string;
  phone?: string;
  ageGroup: string;
  status: "active" | "inactive";
}

export default function ChildAccessModal({ isOpen, onClose }: ChildAccessModalProps) {
  const [, setLocation] = useLocation();
  const { setSelectedChild } = useChild();
  const [step, setStep] = useState<"code" | "select">("code");
  const [accessCode, setAccessCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [linkedChildren, setLinkedChildren] = useState<ValidatedChild[]>([]);

  const validateCodeMutation = trpc.childLinking.validateCode.useMutation();

  const handleAccessChild = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!accessCode.trim()) {
      setError("Please enter the access code");
      return;
    }

    if (accessCode.length !== 8) {
      setError("Access code must be 8 characters");
      return;
    }

    setLoading(true);
    try {
      // Call backend to validate the code
      const response = await validateCodeMutation.mutateAsync({
        code: accessCode.toUpperCase(),
      });

      // Store parent ID for child dashboard context
      localStorage.setItem("currentChildParentId", response.parentId.toString());
      localStorage.setItem("currentChildCode", accessCode.toUpperCase());

      // Fetch linked children from localStorage (in real app, fetch from backend)
      const storedChildren = JSON.parse(localStorage.getItem("children") || "[]");
      const parentChildren = storedChildren.filter(
        (child: any) => child.parentId === response.parentId
      );

      if (parentChildren.length === 0) {
        // No children linked yet, go directly to dashboard
        setLocation("/dashboard/child");
        handleClose();
        toast.success("Successfully linked to child account!");
      } else {
        // Show child selector
        setLinkedChildren(parentChildren);
        setStep("select");
      }
    } catch (err: any) {
      const errorMessage = err?.message || "Failed to access child dashboard";
      setError(errorMessage);
      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectChild = (child: ValidatedChild) => {
    // Set selected child in context
    setSelectedChild(child);
    
    // Store selection in localStorage
    localStorage.setItem("selectedChildId", child.id);
    localStorage.setItem("selectedChildName", child.name);

    // Redirect to child dashboard view (parent viewing child's perspective)
    setLocation("/dashboard/child-view");
    handleClose();
    toast.success(`Now viewing ${child.name}'s dashboard`);
  };

  const handleClose = () => {
    setAccessCode("");
    setError("");
    setStep("code");
    setLinkedChildren([]);
    onClose();
  };

  const handleBack = () => {
    setStep("code");
    setLinkedChildren([]);
    setError("");
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-2xl w-full p-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <div className="flex items-center gap-3">
            {step === "select" && (
              <button
                onClick={handleBack}
                className="p-2 hover:bg-slate-100 rounded-lg transition"
              >
                <ArrowLeft className="w-5 h-5 text-slate-600" />
              </button>
            )}
            <h2 className="text-2xl font-bold text-gray-900">
              {step === "code" ? "Access Child Dashboard" : "Select Child"}
            </h2>
          </div>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-gray-600 transition"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        {/* Step 1: Code Entry */}
        {step === "code" && (
          <>
            {/* Instructions */}
            <div className="mb-6 p-4 bg-blue-50 rounded-lg">
              <p className="text-sm text-gray-700">
                Enter the 8-character code from your child's account to access their dashboard.
              </p>
            </div>

            {/* Form */}
            <form onSubmit={handleAccessChild} className="space-y-4">
              {/* Access Code Input */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Access Code
                </label>
                <Input
                  type="text"
                  placeholder="Enter 8-character code"
                  value={accessCode.toUpperCase()}
                  onChange={(e) => {
                    setAccessCode(e.target.value.toUpperCase());
                    if (error) setError("");
                  }}
                  maxLength={8}
                  className={`text-center text-lg font-mono tracking-widest ${
                    error ? "border-red-500" : ""
                  }`}
                />
                {error && (
                  <div className="mt-2 p-3 bg-red-50 border border-red-200 rounded-lg flex items-start gap-2">
                    <AlertCircle className="w-4 h-4 text-red-600 mt-0.5 flex-shrink-0" />
                    <p className="text-sm text-red-700">{error}</p>
                  </div>
                )}
              </div>

              {/* Submit Button */}
              <Button
                type="submit"
                disabled={loading}
                className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 rounded-lg transition disabled:opacity-50 flex items-center justify-center gap-2"
              >
                {loading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Validating...
                  </>
                ) : (
                  "Continue"
                )}
              </Button>

              {/* Cancel Button */}
              <Button
                type="button"
                onClick={handleClose}
                className="w-full bg-gray-200 hover:bg-gray-300 text-gray-800 font-semibold py-3 rounded-lg transition"
              >
                Cancel
              </Button>
            </form>

            {/* Help Text */}
            <p className="text-xs text-gray-500 text-center mt-4">
              Don't have a code? Ask your child to share their linking code with you.
            </p>
          </>
        )}

        {/* Step 2: Child Selection */}
        {step === "select" && (
          <>
            <div className="mb-6 p-4 bg-blue-50 rounded-lg">
              <p className="text-sm text-gray-700">
                Select which child's dashboard you want to view:
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
              {linkedChildren.map((child) => (
                <Card
                  key={child.id}
                  className="p-4 cursor-pointer transition-all hover:shadow-lg hover:ring-2 hover:ring-blue-400"
                  onClick={() => handleSelectChild(child)}
                >
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex-1">
                      <h3 className="text-lg font-semibold text-slate-900">{child.name}</h3>
                      <p className="text-sm text-slate-600">{child.ageGroup} years old</p>
                    </div>
                  </div>

                  <div className="space-y-1 mb-3">
                    <p className="text-sm text-slate-600">
                      <span className="font-medium">Email:</span> {child.email}
                    </p>
                    {child.phone && (
                      <p className="text-sm text-slate-600">
                        <span className="font-medium">Phone:</span> {child.phone}
                      </p>
                    )}
                  </div>

                  <div className="pt-3 border-t border-slate-200">
                    <span className="text-xs font-medium px-2 py-1 rounded-full bg-green-100 text-green-700">
                      Active
                    </span>
                  </div>
                </Card>
              ))}
            </div>

            <Button
              type="button"
              onClick={handleBack}
              className="w-full bg-gray-200 hover:bg-gray-300 text-gray-800 font-semibold py-3 rounded-lg transition"
            >
              Back
            </Button>
          </>
        )}
      </div>
    </div>
  );
}
