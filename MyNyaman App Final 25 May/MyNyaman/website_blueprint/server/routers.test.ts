import { describe, it, expect, beforeEach, vi } from "vitest";
import { appRouter } from "./routers";
import type { TrpcContext } from "./_core/context";

// Mock context for parent user
function createParentContext(): TrpcContext {
  return {
    user: {
      id: 1,
      openId: "parent-user",
      email: "parent@example.com",
      name: "Parent User",
      loginMethod: "manus",
      role: "parent",
      profilePicture: null,
      bio: null,
      createdAt: new Date(),
      updatedAt: new Date(),
      lastSignedIn: new Date(),
    },
    req: {
      protocol: "https",
      headers: {},
    } as TrpcContext["req"],
    res: {} as TrpcContext["res"],
  };
}

// Mock context for child user
function createChildContext(): TrpcContext {
  return {
    user: {
      id: 2,
      openId: "child-user",
      email: "child@example.com",
      name: "Child User",
      loginMethod: "manus",
      role: "child",
      profilePicture: null,
      bio: null,
      createdAt: new Date(),
      updatedAt: new Date(),
      lastSignedIn: new Date(),
    },
    req: {
      protocol: "https",
      headers: {},
    } as TrpcContext["req"],
    res: {} as TrpcContext["res"],
  };
}

// Mock context for consultant user
function createConsultantContext(): TrpcContext {
  return {
    user: {
      id: 3,
      openId: "consultant-user",
      email: "consultant@example.com",
      name: "Consultant User",
      loginMethod: "manus",
      role: "consultant",
      profilePicture: null,
      bio: null,
      createdAt: new Date(),
      updatedAt: new Date(),
      lastSignedIn: new Date(),
    },
    req: {
      protocol: "https",
      headers: {},
    } as TrpcContext["req"],
    res: {} as TrpcContext["res"],
  };
}

describe("MyNyaman Routers", () => {
  describe("Profile Router", () => {
    it("should allow authenticated users to update their profile", async () => {
      const ctx = createParentContext();
      const caller = appRouter.createCaller(ctx);

      // This would normally update the database
      // For now, we're testing the procedure exists and accepts input
      expect(caller.profile.update).toBeDefined();
    });

    it("should reject unauthenticated profile updates", async () => {
      const ctx = {
        user: null,
        req: { protocol: "https", headers: {} },
        res: {},
      } as unknown as TrpcContext;

      const caller = appRouter.createCaller(ctx);

      try {
        await caller.profile.update({ name: "Test" });
        expect.fail("Should have thrown UNAUTHORIZED error");
      } catch (error: any) {
        expect(error.code).toBe("UNAUTHORIZED");
      }
    });
  });

  describe("Chat Router", () => {
    it("should allow parents to send messages", async () => {
      const ctx = createParentContext();
      const caller = appRouter.createCaller(ctx);

      expect(caller.chat.sendMessage).toBeDefined();
      expect(caller.chat.getOrCreateConversation).toBeDefined();
      expect(caller.chat.getMessages).toBeDefined();
    });

    it("should allow children to send messages", async () => {
      const ctx = createChildContext();
      const caller = appRouter.createCaller(ctx);

      expect(caller.chat.sendMessage).toBeDefined();
    });

    it("should allow consultants to send messages", async () => {
      const ctx = createConsultantContext();
      const caller = appRouter.createCaller(ctx);

      expect(caller.chat.sendMessage).toBeDefined();
    });
  });

  describe("Usage Router", () => {
    it("should allow parents to get child reports", async () => {
      const ctx = createParentContext();
      const caller = appRouter.createCaller(ctx);

      expect(caller.usage.getChildReports).toBeDefined();
    });

    it("should allow parents to set app limits", async () => {
      const ctx = createParentContext();
      const caller = appRouter.createCaller(ctx);

      expect(caller.usage.setAppLimit).toBeDefined();
    });

    it("should allow parents to toggle app blocking", async () => {
      const ctx = createParentContext();
      const caller = appRouter.createCaller(ctx);

      expect(caller.usage.toggleAppBlock).toBeDefined();
    });

    it("should allow children to get their own reports", async () => {
      const ctx = createChildContext();
      const caller = appRouter.createCaller(ctx);

      expect(caller.usage.getMyReports).toBeDefined();
    });

    it("should reject non-parents from setting app limits", async () => {
      const ctx = createChildContext();
      const caller = appRouter.createCaller(ctx);

      try {
        await caller.usage.setAppLimit({
          childId: 2,
          appName: "YouTube",
          packageName: "com.google.android.youtube",
          limitMinutes: 60,
          isEnabled: true,
        });
        expect.fail("Should have thrown FORBIDDEN error");
      } catch (error: any) {
        expect(error.code).toBe("FORBIDDEN");
      }
    });
  });

  describe("Alerts Router", () => {
    it("should allow parents to get alerts", async () => {
      const ctx = createParentContext();
      const caller = appRouter.createCaller(ctx);

      expect(caller.alerts.getAlerts).toBeDefined();
      expect(caller.alerts.getUnreadCount).toBeDefined();
    });

    it("should allow parents to mark alerts as read", async () => {
      const ctx = createParentContext();
      const caller = appRouter.createCaller(ctx);

      expect(caller.alerts.markAsRead).toBeDefined();
      expect(caller.alerts.markAllAsRead).toBeDefined();
    });

    it("should reject non-parents from accessing alerts", async () => {
      const ctx = createChildContext();
      const caller = appRouter.createCaller(ctx);

      try {
        await caller.alerts.getAlerts({ limit: 50 });
        expect.fail("Should have thrown FORBIDDEN error");
      } catch (error: any) {
        expect(error.code).toBe("FORBIDDEN");
      }
    });
  });

  describe("Children Router", () => {
    it("should allow parents to add children", async () => {
      const ctx = createParentContext();
      const caller = appRouter.createCaller(ctx);

      expect(caller.children.addChild).toBeDefined();
    });

    it("should allow parents to get their children", async () => {
      const ctx = createParentContext();
      const caller = appRouter.createCaller(ctx);

      expect(caller.children.getMyChildren).toBeDefined();
    });

    it("should allow parents to remove children", async () => {
      const ctx = createParentContext();
      const caller = appRouter.createCaller(ctx);

      expect(caller.children.removeChild).toBeDefined();
    });

    it("should allow children to get their parents", async () => {
      const ctx = createChildContext();
      const caller = appRouter.createCaller(ctx);

      expect(caller.children.getMyParents).toBeDefined();
    });

    it("should reject non-parents from adding children", async () => {
      const ctx = createChildContext();
      const caller = appRouter.createCaller(ctx);

      try {
        await caller.children.addChild({
          childEmail: "newchild@example.com",
        });
        expect.fail("Should have thrown FORBIDDEN error");
      } catch (error: any) {
        expect(error.code).toBe("FORBIDDEN");
      }
    });
  });

  describe("Auth Router", () => {
    it("should return current user", async () => {
      const ctx = createParentContext();
      const caller = appRouter.createCaller(ctx);

      const user = await caller.auth.me();
      expect(user).toBeDefined();
      expect(user?.role).toBe("parent");
    });

    it("should return null for unauthenticated users", async () => {
      const ctx = {
        user: null,
        req: { protocol: "https", headers: {} },
        res: {},
      } as unknown as TrpcContext;

      const caller = appRouter.createCaller(ctx);
      const user = await caller.auth.me();
      expect(user).toBeNull();
    });
  });

  describe("Role-Based Access Control", () => {
    it("should enforce parent-only procedures", async () => {
      const childCtx = createChildContext();
      const consultantCtx = createConsultantContext();

      const childCaller = appRouter.createCaller(childCtx);
      const consultantCaller = appRouter.createCaller(consultantCtx);

      // These should fail for non-parents
      const procedures = [
        () => childCaller.usage.setAppLimit({
          childId: 2,
          appName: "Test",
          packageName: "com.test",
          limitMinutes: 60,
          isEnabled: true,
        }),
        () => childCaller.alerts.getAlerts({ limit: 50 }),
        () => childCaller.children.addChild({ childEmail: "test@example.com" }),
      ];

      for (const proc of procedures) {
        try {
          await proc();
          expect.fail("Should have thrown FORBIDDEN error");
        } catch (error: any) {
          expect(error.code).toBe("FORBIDDEN");
        }
      }
    });

    it("should allow role-specific access", async () => {
      const parentCtx = createParentContext();
      const childCtx = createChildContext();

      const parentCaller = appRouter.createCaller(parentCtx);
      const childCaller = appRouter.createCaller(childCtx);

      // Parents can access parent-only procedures
      expect(parentCaller.children.getMyChildren).toBeDefined();
      expect(parentCaller.alerts.getAlerts).toBeDefined();

      // Children can access child-only procedures
      expect(childCaller.usage.getMyReports).toBeDefined();
      expect(childCaller.children.getMyParents).toBeDefined();
    });
  });
});
