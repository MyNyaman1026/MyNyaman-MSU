import { Button } from "@/components/ui/button";
import { useLocation } from "wouter";
import { Shield, Clock, AlertTriangle, MessageSquare } from "lucide-react";

export default function LandingPage() {
  const [, setLocation] = useLocation();

  return (
    <div className="min-h-screen bg-white">
      {/* Header */}
      <header className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
        <div className="flex items-center gap-3">
          <img src="https://d2xsxph8kpxj0f.cloudfront.net/310519663413690608/N2GZiDPfbafCV4EzEdoGhQ/MyNyamanPrimaryLogo_6cbc355f.webp" alt="MyNyaman" className="h-10 w-auto" />
        </div>
        <div className="flex gap-3">
          <Button
            variant="outline"
            onClick={() => setLocation("/signin")}
            className="px-6"
          >
            SIGN IN
          </Button>
          <Button
            onClick={() => setLocation("/register")}
            className="px-6 bg-blue-600 hover:bg-blue-700"
          >
            REGISTER
          </Button>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-6xl mx-auto px-6 py-12">
        <div className="grid grid-cols-2 gap-12 items-center mb-16">
          {/* Left Side - Content */}
          <div>
            <h1 className="text-5xl font-bold text-gray-900 mb-4">
              Digital Wellness <br />
              <span className="text-yellow-400">&</span> Safety <br />
              for Youth
            </h1>
            <p className="text-lg text-gray-600 mb-8">
              MyNyaman is a comprehensive parental control and digital wellness platform that helps families build healthy device habits through intelligent monitoring, time management, and expert guidance.
            </p>
            <div className="flex gap-4">
              <Button
                onClick={() => setLocation("/register")}
                className="px-8 py-6 bg-blue-600 hover:bg-blue-700 text-lg"
              >
                Get Started Free
              </Button>
            </div>
          </div>

          {/* Right Side - Image Placeholder */}
          <div className="bg-gradient-to-br from-blue-50 to-blue-100 rounded-lg h-96 flex items-center justify-center">
            <div className="text-center">
              <div className="text-6xl mb-4">👨‍👩‍👧‍👦</div>
              <p className="text-gray-600">Family Digital Wellness</p>
            </div>
          </div>
        </div>

        {/* Features Grid */}
        <div className="mb-16">
          <h2 className="text-3xl font-bold text-gray-900 mb-12 text-center">
            Core Features
          </h2>
          <div className="grid grid-cols-2 gap-6">
            {/* Child Safety */}
            <div className="bg-blue-50 rounded-2xl p-8 border border-blue-100">
              <div className="w-16 h-16 bg-blue-200 rounded-full flex items-center justify-center mb-4">
                <Shield className="w-8 h-8 text-blue-600" />
              </div>
              <h3 className="text-xl font-bold text-gray-900 mb-2">Child Safety</h3>
              <p className="text-gray-600">
                Protect your children with app blocking, content filtering, and real-time monitoring of device activity.
              </p>
            </div>

            {/* Screentime Usage */}
            <div className="bg-green-50 rounded-2xl p-8 border border-green-100">
              <div className="w-16 h-16 bg-green-200 rounded-full flex items-center justify-center mb-4">
                <Clock className="w-8 h-8 text-green-600" />
              </div>
              <h3 className="text-xl font-bold text-gray-900 mb-2">Screentime Usage</h3>
              <p className="text-gray-600">
                Set flexible daily limits, schedule device-free times, and manage app access with ease.
              </p>
            </div>

            {/* Cyberbully Alert */}
            <div className="bg-red-50 rounded-2xl p-8 border border-red-100">
              <div className="w-16 h-16 bg-red-200 rounded-full flex items-center justify-center mb-4">
                <AlertTriangle className="w-8 h-8 text-red-600" />
              </div>
              <h3 className="text-xl font-bold text-gray-900 mb-2">Cyberbully Alert</h3>
              <p className="text-gray-600">
                Receive instant alerts about suspicious activities and potential cyberbullying incidents.
              </p>
            </div>

            {/* Expert Consultation */}
            <div className="bg-purple-50 rounded-2xl p-8 border border-purple-100">
              <div className="w-16 h-16 bg-purple-200 rounded-full flex items-center justify-center mb-4">
                <MessageSquare className="w-8 h-8 text-purple-600" />
              </div>
              <h3 className="text-xl font-bold text-gray-900 mb-2">Expert Consultation</h3>
              <p className="text-gray-600">
                Connect with digital wellness consultants for personalized advice and family support.
              </p>
            </div>
          </div>
        </div>

        {/* Available Platforms */}
        <div className="text-center py-12 border-t border-gray-200">
          <p className="text-gray-600 mb-6">Available for:</p>
          <div className="flex justify-center gap-6">
            <div className="w-12 h-12 bg-gray-100 rounded-lg flex items-center justify-center text-xl">
              🤖
            </div>
            <div className="w-12 h-12 bg-gray-100 rounded-lg flex items-center justify-center text-xl">
              💻
            </div>
            <div className="w-12 h-12 bg-gray-100 rounded-lg flex items-center justify-center text-xl">
              🍎
            </div>
            <div className="w-12 h-12 bg-gray-100 rounded-lg flex items-center justify-center text-xl">
              🌐
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-gray-900 text-white py-8 px-6 mt-16">
        <div className="max-w-6xl mx-auto text-center">
          <p className="text-gray-400">
            © 2026 MyNyaman. Building healthy digital habits for modern families.
          </p>
        </div>
      </footer>
    </div>
  );
}
