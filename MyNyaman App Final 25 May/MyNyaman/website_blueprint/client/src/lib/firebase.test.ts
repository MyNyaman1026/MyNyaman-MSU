import { describe, it, expect } from "vitest";
import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";

describe("Firebase Configuration", () => {
  it("should initialize Firebase with valid configuration", () => {
    const firebaseConfig = {
      apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
      authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
      projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
      storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
      messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
      appId: import.meta.env.VITE_FIREBASE_APP_ID,
    };

    // Check that all required config values are present
    expect(firebaseConfig.apiKey).toBeDefined();
    expect(firebaseConfig.authDomain).toBeDefined();
    expect(firebaseConfig.projectId).toBeDefined();
    expect(firebaseConfig.appId).toBeDefined();

    // Initialize Firebase
    const app = initializeApp(firebaseConfig);
    expect(app).toBeDefined();

    // Get auth instance
    const auth = getAuth(app);
    expect(auth).toBeDefined();
    expect(auth.app).toBe(app);
  });

  it("should have Firebase API key in correct format", () => {
    const apiKey = import.meta.env.VITE_FIREBASE_API_KEY;
    expect(apiKey).toBeDefined();
    expect(apiKey.length).toBeGreaterThan(0);
    // Firebase API keys typically contain alphanumeric characters and hyphens
    expect(/^[A-Za-z0-9_-]+$/.test(apiKey)).toBe(true);
  });

  it("should have Firebase project ID", () => {
    const projectId = import.meta.env.VITE_FIREBASE_PROJECT_ID;
    expect(projectId).toBeDefined();
    expect(projectId).toContain("mynyaman");
  });
});
