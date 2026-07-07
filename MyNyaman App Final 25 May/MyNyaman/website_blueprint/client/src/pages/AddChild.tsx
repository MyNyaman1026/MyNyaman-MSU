import { useState } from "react";
import { useLocation } from "wouter";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ArrowLeft, Plus, Users } from "lucide-react";

interface Child {
  id: string;
  name: string;
  email: string;
  age: number;
}

export default function AddChild() {
  const [, navigate] = useLocation();
  const [children, setChildren] = useState<Child[]>([
    { id: "1", name: "Sarah", email: "sarah@example.com", age: 12 },
    { id: "2", name: "Alex", email: "alex@example.com", age: 10 },
  ]);
  const [showForm, setShowForm] = useState(false);
  const [newChild, setNewChild] = useState({ name: "", email: "", age: "" });

  const handleAddChild = () => {
    if (newChild.name && newChild.email && newChild.age) {
      setChildren([...children, { id: String(children.length + 1), ...newChild, age: parseInt(newChild.age) }]);
      setNewChild({ name: "", email: "", age: "" });
      setShowForm(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-cyan-50 via-white to-blue-50 pb-8">
      <div className="bg-white border-b border-slate-200 sticky top-0 z-40">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center gap-4">
            <button onClick={() => navigate("/dashboard/parent")} className="p-2 hover:bg-slate-100 rounded-lg transition">
              <ArrowLeft className="w-6 h-6 text-slate-600" />
            </button>
            <h1 className="text-2xl font-bold text-slate-900">Manage Children</h1>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Children List */}
        <div className="mb-8">
          <h2 className="text-lg font-semibold text-slate-900 mb-4 flex items-center gap-2">
            <Users className="w-5 h-5" /> Your Children
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {children.map((child) => (
              <Card key={child.id} className="p-6 bg-gradient-to-br from-blue-50 to-cyan-100">
                <h3 className="font-semibold text-slate-900 mb-2">{child.name}</h3>
                <p className="text-sm text-slate-600 mb-1">Email: {child.email}</p>
                <p className="text-sm text-slate-600">Age: {child.age} years</p>
              </Card>
            ))}
          </div>
        </div>

        {/* Add Child Form */}
        {!showForm ? (
          <Button onClick={() => setShowForm(true)} className="bg-blue-600 hover:bg-blue-700 text-white flex items-center gap-2">
            <Plus className="w-4 h-4" /> Add New Child
          </Button>
        ) : (
          <Card className="p-6">
            <h3 className="text-lg font-semibold text-slate-900 mb-4">Add New Child</h3>
            <div className="space-y-4">
              <input
                type="text"
                placeholder="Child's Name"
                value={newChild.name}
                onChange={(e) => setNewChild({ ...newChild, name: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-600 focus:border-transparent"
              />
              <input
                type="email"
                placeholder="Child's Email"
                value={newChild.email}
                onChange={(e) => setNewChild({ ...newChild, email: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-600 focus:border-transparent"
              />
              <input
                type="number"
                placeholder="Child's Age"
                value={newChild.age}
                onChange={(e) => setNewChild({ ...newChild, age: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-600 focus:border-transparent"
              />
              <div className="flex gap-2">
                <Button variant="outline" onClick={() => setShowForm(false)}>Cancel</Button>
                <Button onClick={handleAddChild} className="bg-blue-600 hover:bg-blue-700 text-white">Add Child</Button>
              </div>
            </div>
          </Card>
        )}
      </div>
    </div>
  );
}
