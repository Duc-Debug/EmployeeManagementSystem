import type { SVGProps } from "react";

export type IconName =
  | "access"
  | "alert"
  | "arrowRight"
  | "branch"
  | "calendar"
  | "check"
  | "chevronDown"
  | "chevronRight"
  | "close"
  | "document"
  | "lock"
  | "logout"
  | "menu"
  | "organization"
  | "plus"
  | "search"
  | "settings"
  | "unlock"
  | "user"
  | "users";

interface IconProps extends Omit<SVGProps<SVGSVGElement>, "children"> {
  name: IconName;
  label?: string;
}

function Glyph({ name }: Pick<IconProps, "name">) {
  switch (name) {
    case "access":
      return <><path d="M12 3 4.5 6.5v5c0 4.3 3 7.8 7.5 9.5 4.5-1.7 7.5-5.2 7.5-9.5v-5L12 3Z" /><path d="m9 12 2 2 4-4" /></>;
    case "alert":
      return <><path d="m12 3 9 16H3L12 3Z" /><path d="M12 9v4" /><path d="M12 16h.01" /></>;
    case "arrowRight":
      return <><path d="M5 12h14" /><path d="m13 6 6 6-6 6" /></>;
    case "branch":
      return <><path d="M6 4v12" /><path d="M6 9h7a3 3 0 0 1 3 3v8" /><circle cx="6" cy="3" r="2" /><circle cx="6" cy="19" r="2" /><circle cx="16" cy="21" r="2" /></>;
    case "calendar":
      return <><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M7 3v4M17 3v4M3 10h18" /></>;
    case "check":
      return <path d="m5 12 4 4L19 6" />;
    case "chevronDown":
      return <path d="m6 9 6 6 6-6" />;
    case "chevronRight":
      return <path d="m9 6 6 6-6 6" />;
    case "close":
      return <path d="m6 6 12 12M18 6 6 18" />;
    case "document":
      return <><path d="M6 3h8l4 4v14H6z" /><path d="M14 3v5h5M9 13h6M9 17h6" /></>;
    case "lock":
      return <><rect x="5" y="10" width="14" height="11" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /></>;
    case "logout":
      return <><path d="M10 17l5-5-5-5" /><path d="M15 12H3" /><path d="M21 19V5a2 2 0 0 0-2-2h-5" /></>;
    case "menu":
      return <path d="M4 7h16M4 12h16M4 17h16" />;
    case "organization":
      return <><rect x="9" y="3" width="6" height="4" rx="1" /><rect x="3" y="17" width="6" height="4" rx="1" /><rect x="15" y="17" width="6" height="4" rx="1" /><path d="M12 7v5M6 17v-2h12v2M6 15v-3h12" /></>;
    case "plus":
      return <path d="M12 5v14M5 12h14" />;
    case "search":
      return <><circle cx="11" cy="11" r="6" /><path d="m16 16 4 4" /></>;
    case "settings":
      return <><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.12 2.12-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1.04 1.56V20.5h-3v-.28A1.7 1.7 0 0 0 10.66 18.66a1.7 1.7 0 0 0-1.88.34l-.06.06L6.6 16.94l.06-.06A1.7 1.7 0 0 0 7 15a1.7 1.7 0 0 0-1.56-1.04H5.2v-3h.24A1.7 1.7 0 0 0 7 9.92a1.7 1.7 0 0 0-.34-1.88L6.6 7.98l2.12-2.12.06.06A1.7 1.7 0 0 0 10.66 6.26 1.7 1.7 0 0 0 11.7 4.7V4.4h3v.3a1.7 1.7 0 0 0 1.04 1.56 1.7 1.7 0 0 0 1.88-.34l.06-.06 2.12 2.12-.06.06a1.7 1.7 0 0 0-.34 1.88 1.7 1.7 0 0 0 1.56 1.04h.24v3h-.24A1.7 1.7 0 0 0 19.4 15Z" /></>;
    case "unlock":
      return <><rect x="5" y="10" width="14" height="11" rx="2" /><path d="M8 10V7a4 4 0 0 1 7.4-2.1" /></>;
    case "user":
      return <><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></>;
    case "users":
      return <><circle cx="9" cy="8" r="3" /><path d="M3 20a6 6 0 0 1 12 0" /><path d="M16 5a3 3 0 0 1 0 6M17 14a5 5 0 0 1 4 5" /></>;
  }
}

export function Icon({ name, label, ...props }: IconProps) {
  return (
    <svg
      aria-hidden={label ? undefined : true}
      aria-label={label}
      fill="none"
      focusable="false"
      stroke="currentColor"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="1.8"
      viewBox="0 0 24 24"
      {...props}
    >
      {label ? <title>{label}</title> : null}
      <Glyph name={name} />
    </svg>
  );
}
