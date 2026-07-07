import { describe, it, expect, beforeEach, afterEach } from "vitest";

// Mock child linking code storage
const childLinkingCodes: Record<
  string,
  {
    code: string;
    childName: string;
    childEmail: string;
    childPhone: string;
    ageGroup: string;
    expiresAt: number;
    used: boolean;
  }
> = {};

// Mock function to generate a child linking code
function generateChildLinkingCode(
  childName: string,
  childEmail: string,
  childPhone: string,
  ageGroup: string
): { code: string; expiresAt: number } {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  let code = "";
  for (let i = 0; i < 8; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }

  const expiresAt = Date.now() + 24 * 60 * 60 * 1000; // 24 hours from now

  childLinkingCodes[code] = {
    code,
    childName,
    childEmail,
    childPhone,
    ageGroup,
    expiresAt,
    used: false,
  };

  return { code, expiresAt };
}

// Mock function to validate a child linking code
function validateChildLinkingCode(code: string): {
  valid: boolean;
  error?: string;
  data?: {
    childName: string;
    childEmail: string;
    childPhone: string;
    ageGroup: string;
  };
} {
  const codeData = childLinkingCodes[code];

  if (!codeData) {
    return { valid: false, error: "Invalid code" };
  }

  if (codeData.used) {
    return { valid: false, error: "Code already used" };
  }

  if (Date.now() > codeData.expiresAt) {
    return { valid: false, error: "Code expired" };
  }

  return {
    valid: true,
    data: {
      childName: codeData.childName,
      childEmail: codeData.childEmail,
      childPhone: codeData.childPhone,
      ageGroup: codeData.ageGroup,
    },
  };
}

// Mock function to mark code as used
function markCodeAsUsed(code: string): boolean {
  const codeData = childLinkingCodes[code];
  if (!codeData) return false;

  codeData.used = true;
  return true;
}

describe("Child Linking System", () => {
  beforeEach(() => {
    // Clear all codes before each test
    Object.keys(childLinkingCodes).forEach((key) => delete childLinkingCodes[key]);
  });

  describe("Code Generation", () => {
    it("should generate a valid 8-character alphanumeric code", () => {
      const { code } = generateChildLinkingCode(
        "Sarah",
        "sarah@example.com",
        "1234567890",
        "9-12"
      );

      expect(code).toHaveLength(8);
      expect(/^[A-Z0-9]{8}$/.test(code)).toBe(true);
    });

    it("should generate unique codes for different children", () => {
      const { code: code1 } = generateChildLinkingCode(
        "Sarah",
        "sarah@example.com",
        "1234567890",
        "9-12"
      );
      const { code: code2 } = generateChildLinkingCode(
        "Alex",
        "alex@example.com",
        "0987654321",
        "13-15"
      );

      expect(code1).not.toBe(code2);
    });

    it("should set expiry time to 24 hours from now", () => {
      const now = Date.now();
      const { expiresAt } = generateChildLinkingCode(
        "Sarah",
        "sarah@example.com",
        "1234567890",
        "9-12"
      );

      const expiryHours = (expiresAt - now) / (1000 * 60 * 60);
      expect(expiryHours).toBeGreaterThan(23.9);
      expect(expiryHours).toBeLessThanOrEqual(24);
    });

    it("should store child details with the code", () => {
      const { code } = generateChildLinkingCode(
        "Sarah",
        "sarah@example.com",
        "1234567890",
        "9-12"
      );

      const codeData = childLinkingCodes[code];
      expect(codeData.childName).toBe("Sarah");
      expect(codeData.childEmail).toBe("sarah@example.com");
      expect(codeData.childPhone).toBe("1234567890");
      expect(codeData.ageGroup).toBe("9-12");
    });

    it("should mark code as unused initially", () => {
      const { code } = generateChildLinkingCode(
        "Sarah",
        "sarah@example.com",
        "1234567890",
        "9-12"
      );

      const codeData = childLinkingCodes[code];
      expect(codeData.used).toBe(false);
    });
  });

  describe("Code Validation", () => {
    it("should validate a valid code", () => {
      const { code } = generateChildLinkingCode(
        "Sarah",
        "sarah@example.com",
        "1234567890",
        "9-12"
      );

      const result = validateChildLinkingCode(code);
      expect(result.valid).toBe(true);
      expect(result.data?.childName).toBe("Sarah");
    });

    it("should reject an invalid code", () => {
      const result = validateChildLinkingCode("INVALID");
      expect(result.valid).toBe(false);
      expect(result.error).toBe("Invalid code");
    });

    it("should reject an already used code", () => {
      const { code } = generateChildLinkingCode(
        "Sarah",
        "sarah@example.com",
        "1234567890",
        "9-12"
      );

      markCodeAsUsed(code);
      const result = validateChildLinkingCode(code);
      expect(result.valid).toBe(false);
      expect(result.error).toBe("Code already used");
    });

    it("should reject an expired code", () => {
      const { code } = generateChildLinkingCode(
        "Sarah",
        "sarah@example.com",
        "1234567890",
        "9-12"
      );

      // Manually set expiry to past
      childLinkingCodes[code].expiresAt = Date.now() - 1000;

      const result = validateChildLinkingCode(code);
      expect(result.valid).toBe(false);
      expect(result.error).toBe("Code expired");
    });

    it("should return child details on successful validation", () => {
      const { code } = generateChildLinkingCode(
        "Sarah",
        "sarah@example.com",
        "1234567890",
        "9-12"
      );

      const result = validateChildLinkingCode(code);
      expect(result.valid).toBe(true);
      expect(result.data).toEqual({
        childName: "Sarah",
        childEmail: "sarah@example.com",
        childPhone: "1234567890",
        ageGroup: "9-12",
      });
    });
  });

  describe("Code Usage Tracking", () => {
    it("should mark code as used", () => {
      const { code } = generateChildLinkingCode(
        "Sarah",
        "sarah@example.com",
        "1234567890",
        "9-12"
      );

      const result = markCodeAsUsed(code);
      expect(result).toBe(true);
      expect(childLinkingCodes[code].used).toBe(true);
    });

    it("should prevent code reuse after marking as used", () => {
      const { code } = generateChildLinkingCode(
        "Sarah",
        "sarah@example.com",
        "1234567890",
        "9-12"
      );

      markCodeAsUsed(code);
      const result = validateChildLinkingCode(code);
      expect(result.valid).toBe(false);
      expect(result.error).toBe("Code already used");
    });

    it("should return false when marking non-existent code as used", () => {
      const result = markCodeAsUsed("INVALID");
      expect(result).toBe(false);
    });
  });

  describe("Multiple Children Management", () => {
    it("should handle multiple children with different codes", () => {
      const { code: code1 } = generateChildLinkingCode(
        "Sarah",
        "sarah@example.com",
        "1111111111",
        "9-12"
      );
      const { code: code2 } = generateChildLinkingCode(
        "Alex",
        "alex@example.com",
        "2222222222",
        "13-15"
      );
      const { code: code3 } = generateChildLinkingCode(
        "Emma",
        "emma@example.com",
        "3333333333",
        "5-8"
      );

      const result1 = validateChildLinkingCode(code1);
      const result2 = validateChildLinkingCode(code2);
      const result3 = validateChildLinkingCode(code3);

      expect(result1.valid).toBe(true);
      expect(result1.data?.childName).toBe("Sarah");
      expect(result2.valid).toBe(true);
      expect(result2.data?.childName).toBe("Alex");
      expect(result3.valid).toBe(true);
      expect(result3.data?.childName).toBe("Emma");
    });

    it("should track usage independently for each code", () => {
      const { code: code1 } = generateChildLinkingCode(
        "Sarah",
        "sarah@example.com",
        "1111111111",
        "9-12"
      );
      const { code: code2 } = generateChildLinkingCode(
        "Alex",
        "alex@example.com",
        "2222222222",
        "13-15"
      );

      markCodeAsUsed(code1);

      const result1 = validateChildLinkingCode(code1);
      const result2 = validateChildLinkingCode(code2);

      expect(result1.valid).toBe(false);
      expect(result1.error).toBe("Code already used");
      expect(result2.valid).toBe(true);
    });
  });

  describe("Edge Cases", () => {
    it("should handle empty code", () => {
      const result = validateChildLinkingCode("");
      expect(result.valid).toBe(false);
      expect(result.error).toBe("Invalid code");
    });

    it("should handle code with special characters", () => {
      const result = validateChildLinkingCode("ABC@#$%^");
      expect(result.valid).toBe(false);
      expect(result.error).toBe("Invalid code");
    });

    it("should be case-sensitive for code validation", () => {
      const { code } = generateChildLinkingCode(
        "Sarah",
        "sarah@example.com",
        "1234567890",
        "9-12"
      );

      const result = validateChildLinkingCode(code.toLowerCase());
      expect(result.valid).toBe(false);
    });
  });
});
