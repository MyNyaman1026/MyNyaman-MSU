import { useState } from "react";
import { createUserWithEmailAndPassword, sendEmailVerification } from "firebase/auth";
import { getAuth } from "firebase/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { AlertCircle, Loader } from "lucide-react";

const auth = getAuth();
const LOGO_URL = "https://cdn.manus.im/webdev-assets/mynyaman-web/MyNyamanPrimaryLogo.webp";

export default function Register() {
  const [step, setStep] = useState<"role" | "details">("role");
  const [role, setRole] = useState<"parent" | "child" | "consultant" | null>(null);
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const navigate = (path: string) => {
    window.location.href = path;
  };

  const handleRoleSelect = (selectedRole: "parent" | "child" | "consultant") => {
    setRole(selectedRole);
    setStep("details");
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      if (!email || !password || !fullName || !role) {
        setError("Please fill in all fields");
        setLoading(false);
        return;
      }

      // Create Firebase user
      const firebaseUser = await createUserWithEmailAndPassword(auth, email, password);
      
      // Send verification email
      await sendEmailVerification(firebaseUser.user);

      // Create user in database via API
      const dbResponse = await fetch("/api/trpc/registration.createUser", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          json: {
            email,
            name: fullName,
            role,
            firebaseUid: firebaseUser.user.uid,
          },
        }),
      });

      if (!dbResponse.ok) {
        throw new Error("Failed to create user in database");
      }

      const dbData = await dbResponse.json();
      
      if (dbData.error?.json?.message) {
        throw new Error(dbData.error.json.message);
      }

      // Redirect to appropriate dashboard
      if (role === "parent") {
        navigate("/dashboard/parent");
      } else if (role === "child") {
        navigate("/dashboard/child");
      } else if (role === "consultant") {
        navigate("/dashboard/consultant");
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "Registration failed";
      setError(errorMessage);
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-500 via-indigo-500 to-purple-600 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <img src={LOGO_URL} alt="MyNyaman" className="h-16 mx-auto" />
        </div>

        {step === "role" ? (
          // Role Selection
          <Card className="p-8 bg-white shadow-xl">
            <h1 className="text-2xl font-bold text-center mb-2 text-gray-900">Create Account</h1>
            <p className="text-center text-gray-600 mb-8">Choose your role</p>

            <div className="space-y-4">
              {[
                { role: "parent" as const, emoji: "👨‍👩‍👧", label: "Parent" },
                { role: "child" as const, emoji: "👧", label: "Child" },
                { role: "consultant" as const, emoji: "👨‍💼", label: "Consultant" },
              ].map(({ role: r, emoji, label }) => (
                <button
                  key={r}
                  onClick={() => handleRoleSelect(r)}
                  className="w-full p-4 border-2 border-gray-200 rounded-lg hover:border-blue-500 hover:bg-blue-50 transition text-center"
                >
                  <div className="text-3xl mb-2">{emoji}</div>
                  <div className="font-semibold text-gray-900">{label}</div>
                </button>
              ))}
            </div>
          </Card>
        ) : (
          // Registration Form
          <Card className="p-8 bg-white shadow-xl">
            <h1 className="text-2xl font-bold text-center mb-2 text-gray-900">Register as {role}</h1>
            <p className="text-center text-gray-600 mb-6">Complete your registration</p>

            <form onSubmit={handleRegister} className="space-y-4">
              <Input
                type="text"
                placeholder="Full Name"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                disabled={loading}
              />
              <Input
                type="email"
                placeholder="Email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={loading}
              />
              <Input
                type="password"
                placeholder="Password (min 6 characters)"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={loading}
              />

              {error && (
                <div className="flex items-center gap-2 p-3 bg-red-50 border border-red-200 rounded text-red-700">
                  <AlertCircle className="w-4 h-4" />
                  <span className="text-sm">{error}</span>
                </div>
              )}

              <Button
                type="submit"
                disabled={loading}
                className="w-full bg-blue-600 hover:bg-blue-700 text-white"
              >
                {loading ? (
                  <>
                    <Loader className="w-4 h-4 mr-2 animate-spin" />
                    Creating account...
                  </>
                ) : (
                  "Create Account"
                )}
              </Button>

              <button
                type="button"
                onClick={() => {
                  setStep("role");
                  setRole(null);
                }}
                className="w-full text-blue-600 hover:text-blue-700 text-sm font-medium"
              >
                Back
              </button>
            </form>
          </Card>
        )}
      </div>
    </div>
  );
}
