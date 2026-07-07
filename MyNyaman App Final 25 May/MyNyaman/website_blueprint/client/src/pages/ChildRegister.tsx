import { useState } from "react";
import { useLocation } from "wouter";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { AlertCircle, Mail, Lock, Loader2, ArrowLeft } from "lucide-react";

export default function ChildRegister() {
  const [, navigate] = useLocation();
  const [step, setStep] = useState<"email" | "code" | "password">("email");
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const validateEmail = (email: string) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  const validatePassword = (password: string) => {
    return password.length >= 6;
  };

  const handleEmailSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!email) {
      setError("Please enter your email address");
      return;
    }

    if (!validateEmail(email)) {
      setError("Please enter a valid email address");
      return;
    }

    setLoading(true);
    try {
      // TODO: Validate email exists in system
      await new Promise((resolve) => setTimeout(resolve, 500));
      setStep("code");
    } catch (err) {
      setError("Failed to verify email. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleCodeSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!code) {
      setError("Please enter the verification code");
      return;
    }

    if (code.length < 6) {
      setError("Verification code must be at least 6 characters");
      return;
    }

    setLoading(true);
    try {
      // TODO: Validate code with backend
      console.log("Validating code:", { email, code });
      await new Promise((resolve) => setTimeout(resolve, 800));
      setStep("password");
    } catch (err) {
      setError("Invalid verification code. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!password || !confirmPassword) {
      setError("Please enter both password fields");
      return;
    }

    if (!validatePassword(password)) {
      setError("Password must be at least 6 characters");
      return;
    }

    if (password !== confirmPassword) {
      setError("Passwords do not match");
      return;
    }

    setLoading(true);
    try {
      // TODO: Create child account with tRPC
      console.log("Creating account:", { email, code, password });
      await new Promise((resolve) => setTimeout(resolve, 1000));

      // Store child session
      localStorage.setItem("childSession", JSON.stringify({ email, role: "child" }));

      // Redirect to child dashboard
      navigate("/child/dashboard");
    } catch (err) {
      setError("Registration failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo/Header - Consistent with SignIn */}
        <div className="text-center mb-8">
          <div className="flex justify-center mb-6">
            <img src="https://d2xsxph8kpxj0f.cloudfront.net/310519663413690608/N2GZiDPfbafCV4EzEdoGhQ/MyNyamanPrimaryLogo_6cbc355f.webp" alt="MyNyaman" className="h-20 w-auto" />
          </div>
          <p className="text-gray-600 mt-4 font-medium">Child Registration</p>
        </div>

        {/* Progress Indicator */}
        <div className="mb-8 flex items-center justify-center gap-2">
          <div className={`w-3 h-3 rounded-full ${step === "email" || step === "code" || step === "password" ? "bg-blue-600" : "bg-gray-300"}`} />
          <div className={`w-3 h-3 rounded-full ${step === "code" || step === "password" ? "bg-blue-600" : "bg-gray-300"}`} />
          <div className={`w-3 h-3 rounded-full ${step === "password" ? "bg-blue-600" : "bg-gray-300"}`} />
        </div>

        {/* Registration Card */}
        <div className="bg-white rounded-2xl shadow-lg p-8">
          {/* Step 1: Email */}
          {step === "email" && (
            <>
              <h2 className="text-2xl font-bold text-gray-900 mb-6">Create Your Account</h2>

              {error && (
                <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex gap-3">
                  <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                  <p className="text-red-700 text-sm">{error}</p>
                </div>
              )}

              <form onSubmit={handleEmailSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Email Address
                  </label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-3 w-5 h-5 text-gray-400" />
                    <Input
                      type="email"
                      placeholder="your.email@example.com"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      disabled={loading}
                      className="pl-10"
                    />
                  </div>
                </div>

                <Button
                  type="submit"
                  disabled={loading}
                  className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 rounded-lg transition disabled:opacity-50 flex items-center justify-center gap-2"
                >
                  {loading ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      Verifying...
                    </>
                  ) : (
                    "Continue"
                  )}
                </Button>
              </form>
            </>
          )}

          {/* Step 2: Verification Code */}
          {step === "code" && (
            <>
              <h2 className="text-2xl font-bold text-gray-900 mb-6">Verify Your Code</h2>

              {error && (
                <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex gap-3">
                  <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                  <p className="text-red-700 text-sm">{error}</p>
                </div>
              )}

              <form onSubmit={handleCodeSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Verification Code
                  </label>
                  <Input
                    type="text"
                    placeholder="Enter 6+ character code"
                    value={code}
                    onChange={(e) => setCode(e.target.value.toUpperCase())}
                    disabled={loading}
                    maxLength={20}
                    className="text-center text-lg font-mono tracking-widest"
                  />
                  <p className="text-xs text-gray-500 mt-2">
                    Ask your parent for this code from the "Manage Children" section
                  </p>
                </div>

                <div className="flex gap-2">
                  <Button
                    type="button"
                    onClick={() => {
                      setStep("email");
                      setError("");
                    }}
                    disabled={loading}
                    className="flex-1 bg-gray-100 hover:bg-gray-200 text-gray-900 font-semibold py-2 rounded-lg"
                  >
                    Back
                  </Button>
                  <Button
                    type="submit"
                    disabled={loading}
                    className="flex-1 bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 rounded-lg transition flex items-center justify-center gap-2"
                  >
                    {loading ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        Verifying...
                      </>
                    ) : (
                      "Verify Code"
                    )}
                  </Button>
                </div>
              </form>
            </>
          )}

          {/* Step 3: Password */}
          {step === "password" && (
            <>
              <h2 className="text-2xl font-bold text-gray-900 mb-6">Create Password</h2>

              {error && (
                <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex gap-3">
                  <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                  <p className="text-red-700 text-sm">{error}</p>
                </div>
              )}

              <form onSubmit={handlePasswordSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Password
                  </label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-3 w-5 h-5 text-gray-400" />
                    <Input
                      type="password"
                      placeholder="••••••••"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      disabled={loading}
                      className="pl-10"
                    />
                  </div>
                  <p className="text-xs text-gray-500 mt-2">
                    Minimum 6 characters
                  </p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Confirm Password
                  </label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-3 w-5 h-5 text-gray-400" />
                    <Input
                      type="password"
                      placeholder="••••••••"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      disabled={loading}
                      className="pl-10"
                    />
                  </div>
                </div>

                <div className="flex gap-2">
                  <Button
                    type="button"
                    onClick={() => {
                      setStep("code");
                      setError("");
                    }}
                    disabled={loading}
                    className="flex-1 bg-gray-100 hover:bg-gray-200 text-gray-900 font-semibold py-2 rounded-lg"
                  >
                    Back
                  </Button>
                  <Button
                    type="submit"
                    disabled={loading}
                    className="flex-1 bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 rounded-lg transition flex items-center justify-center gap-2"
                  >
                    {loading ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        Creating...
                      </>
                    ) : (
                      "Create Account"
                    )}
                  </Button>
                </div>
              </form>
            </>
          )}

          {/* Sign In Link */}
          <div className="mt-6 text-center">
            <p className="text-gray-600">
              Already have an account?{" "}
              <button
                onClick={() => navigate("/child/signin")}
                className="text-blue-600 hover:text-blue-700 font-semibold"
              >
                Sign In
              </button>
            </p>
          </div>
        </div>

        {/* Features Highlight */}
        <div className="mt-8 grid grid-cols-3 gap-4">
          <div className="text-center">
            <div className="text-2xl mb-2">📊</div>
            <p className="text-xs text-gray-600">Track Progress</p>
          </div>
          <div className="text-center">
            <div className="text-2xl mb-2">✅</div>
            <p className="text-xs text-gray-600">Manage Tasks</p>
          </div>
          <div className="text-center">
            <div className="text-2xl mb-2">🧘</div>
            <p className="text-xs text-gray-600">Wellness</p>
          </div>
        </div>
      </div>
    </div>
  );
}
