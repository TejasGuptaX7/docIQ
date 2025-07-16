import React from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import "./index.css";
import { ClerkProvider } from "@clerk/clerk-react";
import { dark } from "@clerk/themes";

const PUBLISHABLE_KEY = import.meta.env.VITE_CLERK_PUBLISHABLE_KEY;
if (!PUBLISHABLE_KEY) throw new Error("Missing Clerk Publishable Key");

/* Glass-dark theme (matching screenshot palette) */
const appearance = {
  baseTheme: dark,
  variables: {
    colorPrimary: "#7C3AED",          // violet accent
    colorBackground: "rgba(20,20,20,0.9)",
    colorTextSecondary: "#9CA3AF",
    fontFamily: "Inter, sans-serif",
    fontSize: "15px",
    borderRadius: "12px"
  },
  elements: {
    card: "glass-morphism !p-8 !backdrop-blur-lg",
    socialButtonsBlockButton:
      "!bg-white/10 !hover:bg-white/15 !border-white/10 !rounded-lg",
  },
};

createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ClerkProvider publishableKey={PUBLISHABLE_KEY} appearance={appearance}>
      <App />
    </ClerkProvider>
  </React.StrictMode>
);
