import { useAuth } from "@/_core/hooks/useAuth";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { getLoginUrl } from "@/const";
import { useLocation } from "wouter";
import { Shield, Clock, MessageSquare, Users, BarChart3, Lock, Zap, ArrowRight, CheckCircle } from "lucide-react";
import { useEffect } from "react";

export default function Home() {
  const { user, loading, isAuthenticated } = useAuth();
  const [, navigate] = useLocation();

  useEffect(() => {
    if (isAuthenticated && user) {
      // User is authenticated, show home page
    }
  }, [isAuthenticated, user]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
        <div className="animate-pulse">
          <div className="w-12 h-12 bg-blue-600 rounded-full"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-white">
      {/* Navigation */}
      <nav className="bg-white border-b border-gray-100 sticky top-0 z-50 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <img src="https://d2xsxph8kpxj0f.cloudfront.net/310519663413690608/N2GZiDPfbafCV4EzEdoGhQ/MyNyamanPrimaryLogo_6cbc355f.webp" alt="MyNyaman" className="h-10 w-auto" />
          </div>
          {!isAuthenticated && (
            <Button 
              onClick={() => window.location.href = getLoginUrl()}
              className="bg-blue-600 hover:bg-blue-700 text-white font-semibold px-6 py-2 rounded-lg transition"
            >
              Sign In
            </Button>
          )}
        </div>
      </nav>

      {/* Hero Section */}
      <section className="bg-gradient-to-br from-blue-50 via-white to-gray-50 py-24 lg:py-32">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-20">
            <div className="inline-block mb-6">
              <span className="bg-blue-100 text-blue-700 px-4 py-2 rounded-full text-sm font-semibold">
                Digital Wellness Platform
              </span>
            </div>
            <h1 className="text-5xl md:text-6xl lg:text-7xl font-bold text-gray-900 mb-8 leading-tight">
              Protect Your Family's Digital <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-600 to-blue-700">Wellness</span>
            </h1>
            <p className="text-xl text-gray-600 mb-10 max-w-3xl mx-auto leading-relaxed">
              MyNyaman is a comprehensive parental control and digital wellness platform that helps families build healthy device habits through intelligent monitoring, time management, and expert guidance.
            </p>
            {!isAuthenticated && (
              <div className="flex flex-col sm:flex-row gap-4 justify-center">
                <Button
                  size="lg"
                  onClick={() => window.location.href = getLoginUrl()}
                  className="bg-blue-600 hover:bg-blue-700 text-white font-semibold py-4 px-8 rounded-lg transition text-lg"
                >
                  Get Started Free
                  <ArrowRight className="w-5 h-5 ml-2" />
                </Button>
                <Button
                  size="lg"
                  variant="outline"
                  className="border-2 border-gray-300 text-gray-900 font-semibold py-4 px-8 rounded-lg hover:bg-gray-50 transition text-lg"
                >
                  Learn More
                </Button>
              </div>
            )}
          </div>

          {/* Hero Image Placeholder */}
          <div className="bg-gradient-to-br from-blue-100 to-blue-50 rounded-2xl h-96 flex items-center justify-center border border-blue-200">
            <div className="text-center">
              <Shield className="w-24 h-24 text-blue-400 mx-auto mb-4 opacity-50" />
              <p className="text-gray-500 font-medium">Digital Wellness Illustration</p>
            </div>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section className="py-20 lg:py-28 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-4xl md:text-5xl font-bold text-gray-900 mb-6">Powerful Features</h2>
            <p className="text-xl text-gray-600 max-w-2xl mx-auto">Everything you need to ensure your family's digital safety</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {/* Feature 1 */}
            <Card className="p-8 hover:shadow-xl transition-all duration-300 border border-gray-200 bg-white">
              <div className="bg-blue-100 w-14 h-14 rounded-lg flex items-center justify-center mb-6">
                <Shield className="w-7 h-7 text-blue-600" />
              </div>
              <h3 className="text-xl font-bold text-gray-900 mb-4">Smart Monitoring</h3>
              <p className="text-gray-600 leading-relaxed">
                Track app usage, screen time, and device activity with detailed analytics and real-time insights.
              </p>
            </Card>

            {/* Feature 2 */}
            <Card className="p-8 hover:shadow-xl transition-all duration-300 border border-gray-200 bg-white">
              <div className="bg-yellow-100 w-14 h-14 rounded-lg flex items-center justify-center mb-6">
                <Clock className="w-7 h-7 text-yellow-600" />
              </div>
              <h3 className="text-xl font-bold text-gray-900 mb-4">Time Management</h3>
              <p className="text-gray-600 leading-relaxed">
                Set flexible daily limits, schedule device-free times, and manage app access with ease.
              </p>
            </Card>

            {/* Feature 3 */}
            <Card className="p-8 hover:shadow-xl transition-all duration-300 border border-gray-200 bg-white">
              <div className="bg-blue-100 w-14 h-14 rounded-lg flex items-center justify-center mb-6">
                <MessageSquare className="w-7 h-7 text-blue-600" />
              </div>
              <h3 className="text-xl font-bold text-gray-900 mb-4">Expert Guidance</h3>
              <p className="text-gray-600 leading-relaxed">
                Connect with digital wellness consultants for personalized advice and family support.
              </p>
            </Card>
          </div>
        </div>
      </section>

      {/* Role Features Section */}
      <section className="py-20 lg:py-28 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-4xl md:text-5xl font-bold text-gray-900 mb-6">Designed for Everyone</h2>
            <p className="text-xl text-gray-600 max-w-2xl mx-auto">Tailored features for parents, children, and consultants</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {/* Parent Card */}
            <Card className="p-10 border-l-4 border-l-blue-600 hover:shadow-lg transition-all bg-white">
              <div className="text-4xl mb-6">👨‍👩‍👧</div>
              <h3 className="text-2xl font-bold text-gray-900 mb-4">For Parents</h3>
              <ul className="space-y-3 text-gray-600">
                <li className="flex items-start gap-3">
                  <CheckCircle className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
                  <span>Monitor device usage in real-time</span>
                </li>
                <li className="flex items-start gap-3">
                  <CheckCircle className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
                  <span>Set app restrictions and time limits</span>
                </li>
                <li className="flex items-start gap-3">
                  <CheckCircle className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
                  <span>Get expert recommendations</span>
                </li>
              </ul>
            </Card>

            {/* Child Card */}
            <Card className="p-10 border-l-4 border-l-yellow-500 hover:shadow-lg transition-all bg-white">
              <div className="text-4xl mb-6">👧</div>
              <h3 className="text-2xl font-bold text-gray-900 mb-4">For Children</h3>
              <ul className="space-y-3 text-gray-600">
                <li className="flex items-start gap-3">
                  <CheckCircle className="w-5 h-5 text-yellow-600 flex-shrink-0 mt-0.5" />
                  <span>Track your own screen time</span>
                </li>
                <li className="flex items-start gap-3">
                  <CheckCircle className="w-5 h-5 text-yellow-600 flex-shrink-0 mt-0.5" />
                  <span>Manage wellness activities</span>
                </li>
                <li className="flex items-start gap-3">
                  <CheckCircle className="w-5 h-5 text-yellow-600 flex-shrink-0 mt-0.5" />
                  <span>Chat with consultants</span>
                </li>
              </ul>
            </Card>

            {/* Consultant Card */}
            <Card className="p-10 border-l-4 border-l-blue-700 hover:shadow-lg transition-all bg-white">
              <div className="text-4xl mb-6">👨‍💼</div>
              <h3 className="text-2xl font-bold text-gray-900 mb-4">For Consultants</h3>
              <ul className="space-y-3 text-gray-600">
                <li className="flex items-start gap-3">
                  <CheckCircle className="w-5 h-5 text-blue-700 flex-shrink-0 mt-0.5" />
                  <span>Provide expert guidance</span>
                </li>
                <li className="flex items-start gap-3">
                  <CheckCircle className="w-5 h-5 text-blue-700 flex-shrink-0 mt-0.5" />
                  <span>Support multiple families</span>
                </li>
                <li className="flex items-start gap-3">
                  <CheckCircle className="w-5 h-5 text-blue-700 flex-shrink-0 mt-0.5" />
                  <span>Track progress and outcomes</span>
                </li>
              </ul>
            </Card>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 lg:py-28 bg-gradient-to-r from-blue-600 to-blue-700">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-4xl md:text-5xl font-bold text-white mb-8">Ready to Protect Your Family?</h2>
          <p className="text-xl text-blue-100 mb-10 leading-relaxed">
            Join thousands of families who are building healthier digital habits with MyNyaman.
          </p>
          {!isAuthenticated && (
            <Button
              size="lg"
              onClick={() => window.location.href = getLoginUrl()}
              className="bg-white text-blue-600 hover:bg-gray-100 font-semibold py-4 px-10 rounded-lg transition text-lg"
            >
              Get Started Free
              <ArrowRight className="w-5 h-5 ml-2" />
            </Button>
          )}
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-900 text-gray-300 py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
            <div>
              <div className="flex items-center gap-2 mb-4">
                <img src="https://d2xsxph8kpxj0f.cloudfront.net/310519663413690608/N2GZiDPfbafCV4EzEdoGhQ/MyNyamanPrimaryLogo_6cbc355f.webp" alt="MyNyaman" className="h-8 w-auto" />
              </div>
              <p className="text-sm text-gray-400">Digital wellness for modern families.</p>
            </div>
            <div>
              <h4 className="font-semibold text-white mb-4">Product</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="hover:text-white transition">Features</a></li>
                <li><a href="#" className="hover:text-white transition">Pricing</a></li>
                <li><a href="#" className="hover:text-white transition">Security</a></li>
              </ul>
            </div>
            <div>
              <h4 className="font-semibold text-white mb-4">Company</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="hover:text-white transition">About</a></li>
                <li><a href="#" className="hover:text-white transition">Blog</a></li>
                <li><a href="#" className="hover:text-white transition">Contact</a></li>
              </ul>
            </div>
            <div>
              <h4 className="font-semibold text-white mb-4">Legal</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="hover:text-white transition">Privacy</a></li>
                <li><a href="#" className="hover:text-white transition">Terms</a></li>
                <li><a href="#" className="hover:text-white transition">Support</a></li>
              </ul>
            </div>
          </div>
          <div className="border-t border-gray-800 pt-8 text-center text-sm text-gray-400">
            <p>&copy; 2026 MyNyaman. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
