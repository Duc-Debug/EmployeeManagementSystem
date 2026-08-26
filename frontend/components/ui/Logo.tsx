import type { SVGProps } from "react";

interface LogoProps extends SVGProps<SVGSVGElement> {
  size?: number | string;
  variant?: "mark" | "full";
  theme?: "dark" | "light";
}

export function Logo({
  size = 36,
  variant = "mark",
  theme = "light",
  className = "",
  ...props
}: LogoProps) {
  return (
    <div className={`app-brand-logo ${className}`} style={{ display: "inline-flex", alignItems: "center", gap: "0.75rem" }}>
      <svg
        width={size}
        height={size}
        viewBox="0 0 48 48"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        {...props}
      >
        <defs>
          <linearGradient id="em-grad-primary" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#6366F1" />
            <stop offset="100%" stopColor="#4338CA" />
          </linearGradient>
          <linearGradient id="em-grad-accent" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#38BDF8" />
            <stop offset="100%" stopColor="#6366F1" />
          </linearGradient>
          <linearGradient id="em-grad-surface" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#818CF8" />
            <stop offset="100%" stopColor="#4F46E5" />
          </linearGradient>
          <filter id="em-glow" x="-20%" y="-20%" width="140%" height="140%">
            <feDropShadow dx="0" dy="4" stdDeviation="6" floodColor="#4F46E5" floodOpacity="0.35" />
          </filter>
        </defs>

        {/* Outer Hex-Shield Container with glow */}
        <rect
          x="4"
          y="4"
          width="40"
          height="40"
          rx="12"
          fill="url(#em-grad-primary)"
          filter="url(#em-glow)"
        />

        {/* Geometric Network Nodes & Structural Organization Shapes */}
        {/* Top Node (Leadership / Hub) */}
        <circle cx="24" cy="14" r="3.5" fill="#FFFFFF" />
        
        {/* Connecting Organization Lines */}
        <path
          d="M24 17.5V23M24 23L15 28M24 23L33 28"
          stroke="#FFFFFF"
          strokeWidth="2.2"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeOpacity="0.9"
        />

        {/* Bottom Left Node */}
        <circle cx="15" cy="30" r="3" fill="url(#em-grad-accent)" stroke="#FFFFFF" strokeWidth="1.5" />
        
        {/* Bottom Center Node */}
        <circle cx="24" cy="34" r="3" fill="#FFFFFF" />
        <path d="M24 23V31" stroke="#FFFFFF" strokeWidth="2.2" strokeLinecap="round" strokeOpacity="0.9" />

        {/* Bottom Right Node */}
        <circle cx="33" cy="30" r="3" fill="url(#em-grad-accent)" stroke="#FFFFFF" strokeWidth="1.5" />

        {/* Subtle decorative glass highlight */}
        <path
          d="M6 16C6 10.4772 10.4772 6 16 6H32C37.5228 6 42 10.4772 42 16V20C30 18 18 20 6 25V16Z"
          fill="#FFFFFF"
          fillOpacity="0.12"
        />
      </svg>

      {variant === "full" && (
        <div style={{ display: "flex", flexDirection: "column", lineHeight: 1.15 }}>
          <span
            style={{
              fontFamily: 'var(--font-display, "Space Grotesk", sans-serif)',
              fontSize: "1.125rem",
              fontWeight: 700,
              letterSpacing: "-0.025em",
              color: theme === "dark" ? "#FFFFFF" : "var(--color-ink-strong, #0F172A)",
            }}
          >
            Nexus<span style={{ color: "#6366F1" }}>HRM</span>
          </span>
          <span
            style={{
              fontSize: "0.6875rem",
              fontWeight: 500,
              color: theme === "dark" ? "#94A3B8" : "var(--color-muted, #64748B)",
              letterSpacing: "0.04em",
              textTransform: "uppercase",
            }}
          >
            Enterprise System
          </span>
        </div>
      )}
    </div>
  );
}
