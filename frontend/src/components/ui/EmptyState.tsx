import type { ReactNode } from "react";

import { Icon, type IconName } from "@/components/ui/Icon";

interface EmptyStateProps {
  action?: ReactNode;
  icon?: IconName;
  message: string;
  title: string;
}

export function EmptyState({ action, icon = "document", message, title }: EmptyStateProps) {
  return (
    <div className="empty-state">
      <span aria-hidden="true" className="empty-state__icon"><Icon name={icon} /></span>
      <div>
        <h3>{title}</h3>
        <p>{message}</p>
        {action ? <div className="empty-state__action">{action}</div> : null}
      </div>
    </div>
  );
}
