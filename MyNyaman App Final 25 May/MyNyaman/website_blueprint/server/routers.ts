import { COOKIE_NAME } from "@shared/const";
import { getSessionCookieOptions } from "./_core/cookies";
import { systemRouter } from "./_core/systemRouter";
import { publicProcedure, protectedProcedure, router } from "./_core/trpc";
import { z } from "zod";
import { getDb } from "./db";
import { users } from "../drizzle/schema";
import { eq } from "drizzle-orm";
import { childLinkingRouter } from "./routers/childLinking";
import { registrationRouter } from "./routers/registration";

export const appRouter = router({
  system: systemRouter,
  childLinking: childLinkingRouter,
  registration: registrationRouter,
  profile: router({
    update: protectedProcedure
      .input(z.object({ name: z.string().optional(), bio: z.string().optional() }))
      .mutation(async ({ ctx, input }) => {
        const db = await getDb();
        if (!db) throw new Error("Database not available");

        const updateData: Record<string, unknown> = {};
        if (input.name !== undefined) updateData.name = input.name;
        if (input.bio !== undefined) updateData.bio = input.bio;

        if (Object.keys(updateData).length === 0) {
          return { success: true };
        }

        await db.update(users)
          .set(updateData)
          .where(eq(users.id, ctx.user.id));

        return { success: true };
      }),
  }),
  auth: router({
    me: publicProcedure.query(opts => opts.ctx.user),
    logout: publicProcedure.mutation(({ ctx }) => {
      const cookieOptions = getSessionCookieOptions(ctx.req);
      ctx.res.clearCookie(COOKIE_NAME, { ...cookieOptions, maxAge: -1 });
      return {
        success: true,
      } as const;
    }),
  }),
});

export type AppRouter = typeof appRouter;
