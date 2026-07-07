import { z } from "zod";
import { publicProcedure, router } from "../_core/trpc";
import { TRPCError } from "@trpc/server";
import crypto from "crypto";

// Password validation schema
const passwordSchema = z
  .string()
  .min(8, "Password must be at least 8 characters")
  .regex(/[A-Z]/, "Password must contain at least one uppercase letter")
  .regex(/[a-z]/, "Password must contain at least one lowercase letter")
  .regex(/[0-9]/, "Password must contain at least one number")
  .regex(/[!@#$%^&*]/, "Password must contain at least one special character (!@#$%^&*)");

// Email validation schema
const emailSchema = z.string().email("Invalid email address");

// Role schema
const roleSchema = z.enum(["parent", "child", "consultant"]);

export const customAuthRouter = router({
  /**
   * Register a new user with email and password
   */
  register: publicProcedure
    .input(
      z.object({
        email: emailSchema,
        password: passwordSchema,
        confirmPassword: z.string(),
        name: z.string().min(2, "Name must be at least 2 characters"),
        role: roleSchema,
      })
    )
    .mutation(async ({ input }) => {
      // Validate passwords match
      if (input.password !== input.confirmPassword) {
        throw new TRPCError({
          code: "BAD_REQUEST",
          message: "Passwords do not match",
        });
      }

      // TODO: Check if email already exists in database
      // TODO: Hash password using bcrypt
      // TODO: Create user in database
      // TODO: Generate email verification token
      // TODO: Send verification email
      // TODO: Return success with message to check email

      return {
        success: true,
        message: "Registration successful! Please check your email to verify your account.",
        email: input.email,
      };
    }),

  /**
   * Login with email and password
   */
  login: publicProcedure
    .input(
      z.object({
        email: emailSchema,
        password: z.string().min(1, "Password is required"),
      })
    )
    .mutation(async ({ input }) => {
      // TODO: Find user by email in database
      // TODO: Verify password using bcrypt
      // TODO: Check if email is verified
      // TODO: Create session/JWT token
      // TODO: Return user data and session token

      throw new TRPCError({
        code: "UNAUTHORIZED",
        message: "Invalid email or password",
      });
    }),

  /**
   * Verify email with token
   */
  verifyEmail: publicProcedure
    .input(z.object({ token: z.string() }))
    .mutation(async ({ input }) => {
      // TODO: Find verification token in database
      // TODO: Check if token is expired
      // TODO: Mark email as verified
      // TODO: Delete verification token
      // TODO: Return success message

      throw new TRPCError({
        code: "BAD_REQUEST",
        message: "Invalid or expired verification token",
      });
    }),

  /**
   * Request password reset
   */
  requestPasswordReset: publicProcedure
    .input(z.object({ email: emailSchema }))
    .mutation(async ({ input }) => {
      // TODO: Find user by email
      // TODO: Generate password reset token
      // TODO: Save token with expiry
      // TODO: Send reset email
      // TODO: Return success message

      return {
        success: true,
        message: "Password reset link sent to your email",
      };
    }),

  /**
   * Reset password with token
   */
  resetPassword: publicProcedure
    .input(
      z.object({
        token: z.string(),
        password: passwordSchema,
        confirmPassword: z.string(),
      })
    )
    .mutation(async ({ input }) => {
      if (input.password !== input.confirmPassword) {
        throw new TRPCError({
          code: "BAD_REQUEST",
          message: "Passwords do not match",
        });
      }

      // TODO: Find reset token in database
      // TODO: Check if token is expired
      // TODO: Hash new password
      // TODO: Update user password
      // TODO: Delete reset token
      // TODO: Return success message

      throw new TRPCError({
        code: "BAD_REQUEST",
        message: "Invalid or expired reset token",
      });
    }),

  /**
   * Validate password strength
   */
  validatePassword: publicProcedure
    .input(z.object({ password: z.string() }))
    .query(({ input }) => {
      const requirements = {
        minLength: input.password.length >= 8,
        hasUppercase: /[A-Z]/.test(input.password),
        hasLowercase: /[a-z]/.test(input.password),
        hasNumber: /[0-9]/.test(input.password),
        hasSpecialChar: /[!@#$%^&*]/.test(input.password),
      };

      const isValid = Object.values(requirements).every((req) => req);

      return {
        isValid,
        requirements,
      };
    }),
});
