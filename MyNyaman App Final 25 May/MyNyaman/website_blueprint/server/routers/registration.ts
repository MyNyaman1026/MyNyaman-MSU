import { publicProcedure, router } from "../_core/trpc";
import { z } from "zod";
import { getDb } from "../db";
import { users } from "../../drizzle/schema";
import { eq } from "drizzle-orm";
import { sdk } from "../_core/sdk";
import { COOKIE_NAME, ONE_YEAR_MS } from "@shared/const";
import { getSessionCookieOptions } from "../_core/cookies";
import type { Request, Response } from "express";

export const registrationRouter = router({
  createUser: publicProcedure
    .input(
      z.object({
        email: z.string().email(),
        name: z.string().min(1),
        role: z.enum(["parent", "child", "consultant"]),
        firebaseUid: z.string(),
      })
    )
    .mutation(async ({ input, ctx }) => {
      const db = await getDb();
      if (!db) throw new Error("Database not available");

      try {
        // Check if user already exists
        const existingUser = await db
          .select()
          .from(users)
          .where(eq(users.openId, input.firebaseUid))
          .limit(1);

        if (existingUser.length > 0) {
          // User already exists, create session token
          const sessionToken = await sdk.createSessionToken(input.firebaseUid, {
            name: existingUser[0].name || "",
            expiresInMs: ONE_YEAR_MS,
          });

          const cookieOptions = getSessionCookieOptions(ctx.req as Request);
          (ctx.res as Response).cookie(COOKIE_NAME, sessionToken, { 
            ...cookieOptions, 
            maxAge: ONE_YEAR_MS 
          });

          return {
            success: true,
            error: "",
            user: existingUser[0],
            role: existingUser[0].role,
          };
        }

        // Create new user with role
        const result = await db.insert(users).values({
          openId: input.firebaseUid,
          email: input.email,
          name: input.name,
          loginMethod: "email",
          role: input.role as "parent" | "child" | "consultant" | "user" | "admin",
          createdAt: new Date(),
          updatedAt: new Date(),
          lastSignedIn: new Date(),
        });

        // Fetch the created user
        const newUser = await db
          .select()
          .from(users)
          .where(eq(users.openId, input.firebaseUid))
          .limit(1);

        if (!newUser[0]) {
          throw new Error("Failed to retrieve created user");
        }

        // Create session token for the new user
        const sessionToken = await sdk.createSessionToken(input.firebaseUid, {
          name: input.name,
          expiresInMs: ONE_YEAR_MS,
        });

        // Set session cookie
        const cookieOptions = getSessionCookieOptions(ctx.req as Request);
        (ctx.res as Response).cookie(COOKIE_NAME, sessionToken, { 
          ...cookieOptions, 
          maxAge: ONE_YEAR_MS 
        });

        return {
          success: true,
          error: "",
          user: newUser[0],
          role: input.role,
        };
      } catch (error) {
        const errorMsg = error instanceof Error ? error.message : String(error);
        console.error("[Registration] Failed to create user:", errorMsg);
        console.error("[Registration] Full error:", error);
        throw new Error(`Failed to create user account: ${errorMsg}`);
      }
    }),

  getUserByFirebaseUid: publicProcedure
    .input(z.object({ firebaseUid: z.string() }))
    .query(async ({ input }) => {
      const db = await getDb();
      if (!db) return null;

      try {
        const result = await db
          .select()
          .from(users)
          .where(eq(users.openId, input.firebaseUid))
          .limit(1);

        return result.length > 0 ? result[0] : null;
      } catch (error) {
        console.error("[Registration] Failed to get user:", error);
        return null;
      }
    }),
});
