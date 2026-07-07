import { useState } from "react";
import { X, Copy, Check, Loader2, Eye, EyeOff, Share2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { toast } from "sonner";
import { trpc } from "@/lib/trpc";
import { useChild } from "@/contexts/ChildContext";

interface AddChildModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function AddChildModal({ isOpen, onClose }: AddChildModalProps) {
  const [step, setStep] = useState<"details" | "code">("details");
  const [childName, setChildName] = useState("");
  const [childEmail, setChildEmail] = useState("");
  const [childPhone, setChildPhone] = useState("");
  const [ageGroup, setAgeGroup] = useState("");
  const [childPassword, setChildPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [generatedCode, setGeneratedCode] = useState<string | null>(null);
  const [expiryTime, setExpiryTime] = useState<Date | null>(null);
  const [copied, setCopied] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const generateCodeMutation = trpc.childLinking.generateCode.useMutation();
  const { addChild } = useChild();

  const validateDetails = () => {
    const newErrors: Record<string, string> = {};

    if (!childName.trim()) newErrors.childName = "Child name is required";
    if (!childEmail.trim()) newErrors.childEmail = "Email is required";
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(childEmail)) newErrors.childEmail = "Invalid email format";
    if (!childPhone.trim()) newErrors.childPhone = "Phone number is required";
    if (!ageGroup) newErrors.ageGroup = "Age group is required";
    if (!childPassword) newErrors.childPassword = "Password is required";
    if (childPassword.length < 8) newErrors.childPassword = "Password must be at least 8 characters";
    if (childPassword !== confirmPassword) newErrors.confirmPassword = "Passwords do not match";

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const generateCode = async () => {
    if (!validateDetails()) return;

    setLoading(true);
    try {
      // Call backend to generate code
      const response = await generateCodeMutation.mutateAsync();
      setGeneratedCode(response.code);
      setExpiryTime(new Date(response.expiresAt));
      
      // Add child to context
      const childId = `child_${Date.now()}`;
      addChild({
        id: childId,
        name: childName,
        email: childEmail,
        phone: childPhone,
        ageGroup: ageGroup,
        code: response.code,
        expiresAt: new Date(response.expiresAt),
        status: "active",
      });
      
      // Store code details for Manage Children page
      const existingCodes = JSON.parse(localStorage.getItem("generatedCodes") || "[]");
      existingCodes.push({
        childId,
        childName,
        code: response.code,
        expiresAt: response.expiresAt,
        used: false,
      });
      localStorage.setItem("generatedCodes", JSON.stringify(existingCodes));
      
      setStep("code");
      toast.success("Child linking code generated!");
    } catch (error) {
      toast.error("Failed to generate code");
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const copyToClipboard = () => {
    if (generatedCode) {
      navigator.clipboard.writeText(generatedCode);
      setCopied(true);
      toast.success("Code copied to clipboard!");
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const shareCode = async () => {
    if (!generatedCode) return;

    const shareText = `I've created a MyNyaman account for ${childName}. Use this code to link: ${generatedCode}`;

    // Try Web Share API first
    if (navigator.share) {
      try {
        await navigator.share({
          title: "MyNyaman Child Linking Code",
          text: shareText,
        });
        toast.success("Code shared successfully!");
      } catch (error) {
        // User cancelled share
      }
    } else {
      // Fallback: copy to clipboard
      navigator.clipboard.writeText(shareText);
      toast.success("Share text copied to clipboard!");
    }
  };

  const handleClose = () => {
    setStep("details");
    setChildName("");
    setChildEmail("");
    setChildPhone("");
    setAgeGroup("");
    setChildPassword("");
    setConfirmPassword("");
    setGeneratedCode(null);
    setExpiryTime(null);
    setCopied(false);
    setErrors({});
    onClose();
  };

  const handleBack = () => {
    setStep("details");
    setGeneratedCode(null);
    setExpiryTime(null);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full p-8 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold text-gray-900">
            {step === "details" ? "Add Child" : "Child Added Successfully!"}
          </h2>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-gray-600 transition"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        {step === "details" ? (
          <>
            {/* Child Details Form */}
            <div className="space-y-4">
              {/* Child Name */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Child Name
                </label>
                <Input
                  type="text"
                  placeholder="Enter child's name"
                  value={childName}
                  onChange={(e) => {
                    setChildName(e.target.value);
                    if (errors.childName) {
                      setErrors({ ...errors, childName: "" });
                    }
                  }}
                  className={errors.childName ? "border-red-500" : ""}
                />
                {errors.childName && (
                  <p className="text-red-500 text-xs mt-1">{errors.childName}</p>
                )}
              </div>

              {/* Child Email */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Email
                </label>
                <Input
                  type="email"
                  placeholder="Enter child's email"
                  value={childEmail}
                  onChange={(e) => {
                    setChildEmail(e.target.value);
                    if (errors.childEmail) {
                      setErrors({ ...errors, childEmail: "" });
                    }
                  }}
                  className={errors.childEmail ? "border-red-500" : ""}
                />
                {errors.childEmail && (
                  <p className="text-red-500 text-xs mt-1">{errors.childEmail}</p>
                )}
              </div>

              {/* Phone Number */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Phone Number
                </label>
                <Input
                  type="tel"
                  placeholder="Enter phone number"
                  value={childPhone}
                  onChange={(e) => {
                    setChildPhone(e.target.value);
                    if (errors.childPhone) {
                      setErrors({ ...errors, childPhone: "" });
                    }
                  }}
                  className={errors.childPhone ? "border-red-500" : ""}
                />
                {errors.childPhone && (
                  <p className="text-red-500 text-xs mt-1">{errors.childPhone}</p>
                )}
              </div>

              {/* Age Group */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Age Group
                </label>
                <select
                  value={ageGroup}
                  onChange={(e) => {
                    setAgeGroup(e.target.value);
                    if (errors.ageGroup) {
                      setErrors({ ...errors, ageGroup: "" });
                    }
                  }}
                  className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                    errors.ageGroup ? "border-red-500" : "border-gray-300"
                  }`}
                >
                  <option value="">Select age group</option>
                  <option value="5-8">5-8 years</option>
                  <option value="9-12">9-12 years</option>
                  <option value="13-15">13-15 years</option>
                  <option value="16-18">16-18 years</option>
                </select>
                {errors.ageGroup && (
                  <p className="text-red-500 text-xs mt-1">{errors.ageGroup}</p>
                )}
              </div>

              {/* Password */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Password
                </label>
                <div className="relative">
                  <Input
                    type={showPassword ? "text" : "password"}
                    placeholder="Enter password"
                    value={childPassword}
                    onChange={(e) => {
                      setChildPassword(e.target.value);
                      if (errors.childPassword) {
                        setErrors({ ...errors, childPassword: "" });
                      }
                    }}
                    className={errors.childPassword ? "border-red-500 pr-10" : "pr-10"}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    {showPassword ? (
                      <EyeOff className="w-4 h-4" />
                    ) : (
                      <Eye className="w-4 h-4" />
                    )}
                  </button>
                </div>
                {errors.childPassword && (
                  <p className="text-red-500 text-xs mt-1">{errors.childPassword}</p>
                )}
              </div>

              {/* Confirm Password */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Confirm Password
                </label>
                <Input
                  type={showPassword ? "text" : "password"}
                  placeholder="Confirm password"
                  value={confirmPassword}
                  onChange={(e) => {
                    setConfirmPassword(e.target.value);
                    if (errors.confirmPassword) {
                      setErrors({ ...errors, confirmPassword: "" });
                    }
                  }}
                  className={errors.confirmPassword ? "border-red-500" : ""}
                />
                {errors.confirmPassword && (
                  <p className="text-red-500 text-xs mt-1">{errors.confirmPassword}</p>
                )}
              </div>
            </div>

            {/* Generate Button */}
            <Button
              onClick={generateCode}
              disabled={loading}
              className="w-full mt-6 bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 rounded-lg transition disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Generating Code...
                </>
              ) : (
                "Generate Code"
              )}
            </Button>
          </>
        ) : (
          <>
            {/* Success Message */}
            <div className="mb-6 p-4 bg-green-50 rounded-lg border border-green-200 flex items-start gap-3">
              <div className="text-green-600 text-2xl">✓</div>
              <div>
                <p className="text-sm text-green-700 font-medium">
                  Pairing code generated successfully!
                </p>
                <p className="text-xs text-green-600 mt-1">
                  Share this code with {childName} to link their account.
                </p>
              </div>
            </div>

            {/* Code Display */}
            <div className="mb-6 p-6 bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl border-2 border-blue-200">
              <p className="text-xs text-gray-600 uppercase tracking-wider mb-2">
                Pairing Code
              </p>
              <p className="text-4xl font-bold text-blue-600 tracking-widest text-center mb-2">
                {generatedCode}
              </p>
              <p className="text-xs text-gray-500 text-center">
                Expires: {expiryTime?.toLocaleString() || "Loading..."}
              </p>
            </div>

            {/* Instructions */}
            <div className="mb-6 p-4 bg-blue-50 rounded-lg">
              <p className="text-sm text-gray-700">
                <strong>Next Step:</strong> Your child should open the MyNyaman app and enter this code to connect to your account.
              </p>
            </div>

            {/* Action Buttons */}
            <div className="space-y-3">
              <Button
                onClick={copyToClipboard}
                className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 rounded-lg transition flex items-center justify-center gap-2"
              >
                {copied ? (
                  <>
                    <Check className="w-4 h-4" />
                    Code Copied!
                  </>
                ) : (
                  <>
                    <Copy className="w-4 h-4" />
                    Copy Code
                  </>
                )}
              </Button>

              <Button
                onClick={shareCode}
                className="w-full bg-green-600 hover:bg-green-700 text-white font-semibold py-3 rounded-lg transition flex items-center justify-center gap-2"
              >
                <Share2 className="w-4 h-4" />
                Share Code
              </Button>

              <Button
                onClick={handleClose}
                className="w-full bg-gray-200 hover:bg-gray-300 text-gray-800 font-semibold py-3 rounded-lg transition"
              >
                Done
              </Button>
            </div>

            {/* Back Button */}
            <button
              onClick={handleBack}
              className="w-full mt-3 text-sm text-blue-600 hover:text-blue-700 font-medium transition"
            >
              ← Back to Edit Details
            </button>
          </>
        )}
      </div>
    </div>
  );
}
