import { useState } from "react";
import { useLocation } from "wouter";
import { Card } from "@/components/ui/card";
import { Users, Briefcase, Baby } from "lucide-react";
import { useRole } from "@/contexts/RoleContext";

export default function RoleSelection() {
  const [, navigate] = useLocation();
  const { setRole } = useRole();

  const handleRoleSelect = (role: string) => {
    localStorage.setItem("selectedRole", role);
    
    if (role === "parent") {
      setRole("parent");
      localStorage.setItem("parentSession", "true");
      navigate("/dashboard/parent");
    } else if (role === "consultant") {
      setRole("consultant");
      localStorage.setItem("consultantSession", "true");
      navigate("/dashboard/consultant");
    } else if (role === "child") {
      setRole("child");
      localStorage.setItem("childSession", "true");
      navigate("/child/dashboard");
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-gray-50 flex items-center justify-center p-4 py-12">
      <div className="w-full max-w-4xl">
        {/* Logo/Header */}
        <div className="text-center mb-16">
          <div className="flex justify-center mb-6">
            <img src="https://d2xsxph8kpxj0f.cloudfront.net/310519663413690608/N2GZiDPfbafCV4EzEdoGhQ/MyNyamanPrimaryLogo_6cbc355f.webp" alt="MyNyaman" className="h-24 w-auto" />
          </div>
          <p className="text-lg text-gray-600 font-medium mt-4">Choose your role to continue</p>
        </div>

        {/* Role Selection Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-12">
          {/* Parent Card */}
          <Card
            onClick={() => handleRoleSelect("parent")}
            className="p-10 cursor-pointer hover:shadow-2xl transition-all duration-300 border-2 border-gray-100 hover:border-blue-500 bg-white hover:bg-blue-50 group"
          >
            <div className="flex flex-col items-center text-center">
              <div className="text-6xl mb-6 group-hover:scale-110 transition-transform duration-300">👨‍👩‍👧</div>
              <h3 className="font-bold text-xl text-gray-900 mb-3">Parent</h3>
              <p className="text-gray-600 leading-relaxed">Monitor and manage your children's digital wellness with powerful tools and insights</p>
            </div>
          </Card>

          {/* Child Card - Middle */}
          <Card
            onClick={() => handleRoleSelect("child")}
            className="p-10 cursor-pointer hover:shadow-2xl transition-all duration-300 border-2 border-gray-100 hover:border-yellow-500 bg-white hover:bg-yellow-50 group ring-2 ring-blue-200 ring-opacity-50"
          >
            <div className="flex flex-col items-center text-center">
              <div className="text-6xl mb-6 group-hover:scale-110 transition-transform duration-300">👧</div>
              <h3 className="font-bold text-xl text-gray-900 mb-3">Child</h3>
              <p className="text-gray-600 leading-relaxed">Track your screen time and stay connected with your family</p>
            </div>
          </Card>

          {/* Consultant Card */}
          <Card
            onClick={() => handleRoleSelect("consultant")}
            className="p-10 cursor-pointer hover:shadow-2xl transition-all duration-300 border-2 border-gray-100 hover:border-blue-700 bg-white hover:bg-blue-50 group"
          >
            <div className="flex flex-col items-center text-center">
              <div className="text-6xl mb-6 group-hover:scale-110 transition-transform duration-300">👨‍💼</div>
              <h3 className="font-bold text-xl text-gray-900 mb-3">Consultant</h3>
              <p className="text-gray-600 leading-relaxed">Provide expert guidance and support to families on digital wellness</p>
            </div>
          </Card>
        </div>

        {/* Footer */}
        <div className="text-center">
          <p className="text-gray-600 font-medium">
            Not registered yet?{" "}
            <a href="/register" className="text-blue-600 hover:text-blue-700 font-semibold transition">
              Create an account
            </a>
          </p>
        </div>
      </div>
    </div>
  );
}
