import { useState } from "react";
import { useLocation } from "wouter";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ArrowLeft, Send, MessageCircle } from "lucide-react";
import { useChild } from "@/contexts/ChildContext";

interface Message {
  id: string;
  sender: "user" | "consultant";
  content: string;
  timestamp: string;
}

const initialMessages: Message[] = [
  { id: "1", sender: "consultant", content: "Hello! How can I help you today?", timestamp: "10:30 AM" },
  { id: "2", sender: "user", content: "My child is spending too much time on social media", timestamp: "10:31 AM" },
  { id: "3", sender: "consultant", content: "That's a common concern. Have you tried setting daily limits?", timestamp: "10:32 AM" },
];

export default function Chat() {
  const [, navigate] = useLocation();
  const { selectedChild } = useChild();
  const [messages, setMessages] = useState(initialMessages);
  const [newMessage, setNewMessage] = useState("");

  const handleSendMessage = () => {
    if (newMessage.trim()) {
      const message: Message = {
        id: String(messages.length + 1),
        sender: "user",
        content: newMessage,
        timestamp: new Date().toLocaleTimeString(),
      };
      setMessages([...messages, message]);
      setNewMessage("");
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-50 via-white to-indigo-50 flex flex-col">
      {/* Header */}
      <div className="bg-white border-b border-slate-200 sticky top-0 z-40">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate("/dashboard/parent")}
              className="p-2 hover:bg-slate-100 rounded-lg transition"
            >
              <ArrowLeft className="w-6 h-6 text-slate-600" />
            </button>
            <div>
              <h1 className="text-2xl font-bold text-slate-900">
                Chat with Consultant {selectedChild && `- ${selectedChild.name}`}
              </h1>
              <p className="text-sm text-slate-600">Expert guidance on digital wellness</p>
            </div>
          </div>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 max-w-4xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-8 overflow-y-auto">
        <div className="space-y-4">
          {messages.map((msg) => (
            <div key={msg.id} className={`flex ${msg.sender === "user" ? "justify-end" : "justify-start"}`}>
              <Card className={`max-w-xs p-4 ${msg.sender === "user" ? "bg-blue-600 text-white" : "bg-slate-100"}`}>
                <p className="text-sm">{msg.content}</p>
                <p className={`text-xs mt-2 ${msg.sender === "user" ? "text-blue-100" : "text-slate-600"}`}>{msg.timestamp}</p>
              </Card>
            </div>
          ))}
        </div>
      </div>

      {/* Input */}
      <div className="bg-white border-t border-slate-200 sticky bottom-0">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex gap-2">
            <input
              type="text"
              placeholder="Type your message..."
              value={newMessage}
              onChange={(e) => setNewMessage(e.target.value)}
              onKeyPress={(e) => e.key === "Enter" && handleSendMessage()}
              className="flex-1 px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-purple-600 focus:border-transparent"
            />
            <Button onClick={handleSendMessage} className="bg-purple-600 hover:bg-purple-700 text-white">
              <Send className="w-4 h-4" />
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
