import { useState, useEffect } from "react";
import { useLocation } from "wouter";
import { Button } from "@/components/ui/button";
import { useFirebaseAuth } from "@/contexts/FirebaseAuthContext";
import { Mail, CheckCircle2, Loader2 } from "lucide-react";

export default function VerifyEmail() {
  const [, setLocation] = useLocation();
  const { user, sendVerificationEmail } = useFirebaseAuth();
  const [loading, setLoading] = useState(false);
  const [resent, setResent] = useState(false);
  const [countdown, setCountdown] = useState(0);

  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [countdown]);

  const handleResendEmail = async () => {
    setLoading(true);
    try {
      await sendVerificationEmail();
      setResent(true);
      setCountdown(60);
    } catch (err) {
      console.error("Failed to resend verification email:", err);
    } finally {
      setLoading(false);
    }
  };

  const handleContinue = () => {
    if (user?.emailVerified) {
      setLocation("/role-selection");
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="flex justify-center mb-6">
            <img src="https://d2xsxph8kpxj0f.cloudfront.net/310519663413690608/N2GZiDPfbafCV4EzEdoGhQ/MyNyamanPrimaryLogo_6cbc355f.webp" alt="MyNyaman" className="h-20 w-auto" />
          </div>
        </div>

        {/* Verification Card */}
        <div className="bg-white rounded-2xl shadow-lg p-8">
          <div className="text-center mb-6">
            <div className="flex justify-center mb-4">
              <Mail className="w-16 h-16 text-blue-600" />
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Verify Your Email</h2>
            <p className="text-gray-600">
              We've sent a verification link to <br />
              <span className="font-semibold text-gray-900">{user?.email}</span>
            </p>
          </div>

          {/* Verification Status */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 mb-6">
            <div className="flex items-start gap-3">
              {user?.emailVerified ? (
                <>
                  <CheckCircle2 className="w-6 h-6 text-green-600 flex-shrink-0 mt-0.5" />
                  <div>
                    <p className="font-semibold text-green-700">Email Verified!</p>
                    <p className="text-sm text-green-600">
                      Your email has been successfully verified.
                    </p>
                  </div>
                </>
              ) : (
                <>
                  <Mail className="w-6 h-6 text-blue-600 flex-shrink-0 mt-0.5" />
                  <div>
                    <p className="font-semibold text-blue-700">Check Your Email</p>
                    <p className="text-sm text-blue-600">
                      Click the verification link in the email we sent you.
                    </p>
                  </div>
                </>
              )}
            </div>
          </div>

          {/* Instructions */}
          <div className="bg-gray-50 rounded-lg p-4 mb-6">
            <p className="text-sm text-gray-700 mb-3 font-semibold">Next Steps:</p>
            <ol className="text-sm text-gray-600 space-y-2 list-decimal list-inside">
              <li>Open the email we sent to {user?.email}</li>
              <li>Click the verification link</li>
              <li>Return here and click "Continue"</li>
            </ol>
          </div>

          {/* Resend Email */}
          <div className="mb-6">
            {resent && (
              <p className="text-sm text-green-600 mb-3 text-center">
                ✓ Verification email resent successfully
              </p>
            )}
            <Button
              onClick={handleResendEmail}
              disabled={loading || countdown > 0}
              variant="outline"
              className="w-full flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <Loader2 className="w-5 h-5 animate-spin" />
                  Resending...
                </>
              ) : countdown > 0 ? (
                `Resend in ${countdown}s`
              ) : (
                "Resend Verification Email"
              )}
            </Button>
          </div>

          {/* Continue Button */}
          <Button
            onClick={handleContinue}
            disabled={!user?.emailVerified}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white py-3 rounded-lg font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {user?.emailVerified ? "Continue to Dashboard" : "Waiting for Verification..."}
          </Button>

          {/* Back to Sign In */}
          <div className="mt-6 text-center">
            <button
              onClick={() => setLocation("/signin")}
              className="text-blue-600 hover:text-blue-700 font-medium text-sm"
            >
              Back to Sign In
            </button>
          </div>
        </div>

        {/* Footer */}
        <p className="text-center text-gray-600 text-sm mt-8">
          Didn't receive the email? Check your spam folder or contact support.
        </p>
      </div>
    </div>
  );
}
