import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Lightbulb, Share2, Bookmark } from "lucide-react";
import { useState } from "react";

interface Advice {
  id: string;
  title: string;
  content: string;
  category: string;
  source: "ai_generated" | "curated";
  isActive: boolean;
}

interface ParentingAdviceProps {
  advice?: Advice[];
  onShare?: (adviceId: string) => void;
  onBookmark?: (adviceId: string) => void;
  isLoading?: boolean;
}

const defaultAdvice: Advice[] = [
  {
    id: "1",
    title: "Establish Screen-Free Zones",
    content:
      "Create designated areas in your home where screens are not allowed, such as the dining table or bedrooms. This helps establish healthy boundaries and encourages face-to-face interaction during meals and before bedtime.",
    category: "Screen Time Management",
    source: "curated",
    isActive: true,
  },
  {
    id: "2",
    title: "Model Good Digital Habits",
    content:
      "Children learn by example. Demonstrate responsible device usage by limiting your own screen time, putting your phone away during conversations, and taking regular breaks. Your behavior sets the standard for your children.",
    category: "Digital Wellness",
    source: "curated",
    isActive: true,
  },
  {
    id: "3",
    title: "Have Open Conversations",
    content:
      "Talk regularly with your children about their online activities, friends, and concerns. Create a safe space where they feel comfortable discussing what they encounter online without fear of punishment.",
    category: "Communication",
    source: "curated",
    isActive: true,
  },
  {
    id: "4",
    title: "Set Consistent Time Limits",
    content:
      "Establish clear, consistent rules about device usage. Use tools like app limits and scheduled downtime to help enforce these boundaries. Consistency helps children develop self-regulation skills.",
    category: "Screen Time Management",
    source: "curated",
    isActive: true,
  },
  {
    id: "5",
    title: "Encourage Alternative Activities",
    content:
      "Promote offline hobbies and activities like sports, reading, art, or outdoor play. Having engaging alternatives makes it easier for children to step away from screens and develop diverse interests.",
    category: "Digital Wellness",
    source: "curated",
    isActive: true,
  },
];

export default function ParentingAdvice({
  advice = defaultAdvice,
  onShare,
  onBookmark,
  isLoading = false,
}: ParentingAdviceProps) {
  const [bookmarked, setBookmarked] = useState<Set<string>>(new Set());

  const handleBookmark = (adviceId: string) => {
    const newBookmarked = new Set(bookmarked);
    if (newBookmarked.has(adviceId)) {
      newBookmarked.delete(adviceId);
    } else {
      newBookmarked.add(adviceId);
    }
    setBookmarked(newBookmarked);
    onBookmark?.(adviceId);
  };

  const categories = Array.from(new Set(advice.map((a) => a.category)));

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2 mb-6">
        <Lightbulb className="w-6 h-6 text-yellow-500" />
        <h2 className="text-2xl font-bold text-slate-900">Parenting Advice & Resources</h2>
      </div>

      {categories.map((category) => (
        <div key={category}>
          <h3 className="text-lg font-semibold text-slate-900 mb-4">{category}</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {advice
              .filter((a) => a.category === category && a.isActive)
              .map((item) => (
                <Card key={item.id} className="p-6 hover:shadow-lg transition-shadow">
                  <div className="flex items-start justify-between mb-3">
                    <h4 className="text-lg font-semibold text-slate-900 flex-1">{item.title}</h4>
                    <Badge
                      variant={item.source === "ai_generated" ? "secondary" : "outline"}
                      className="ml-2 flex-shrink-0"
                    >
                      {item.source === "ai_generated" ? "AI Generated" : "Curated"}
                    </Badge>
                  </div>

                  <p className="text-slate-600 text-sm mb-4 line-clamp-3">{item.content}</p>

                  <div className="flex gap-2">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleBookmark(item.id)}
                      className={bookmarked.has(item.id) ? "text-blue-600" : ""}
                    >
                      <Bookmark
                        className={`w-4 h-4 ${bookmarked.has(item.id) ? "fill-current" : ""}`}
                      />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => onShare?.(item.id)}
                    >
                      <Share2 className="w-4 h-4" />
                    </Button>
                    <Button variant="outline" size="sm" className="ml-auto">
                      Read More
                    </Button>
                  </div>
                </Card>
              ))}
          </div>
        </div>
      ))}

      {advice.length === 0 && !isLoading && (
        <Card className="p-8 text-center">
          <p className="text-slate-500">No advice available at this time</p>
        </Card>
      )}
    </div>
  );
}
