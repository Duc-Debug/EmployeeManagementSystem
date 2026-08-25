"use client";

import { useId, useLayoutEffect, useRef, type ReactNode, type RefObject } from "react";

import { Icon } from "@/components/ui/Icon";

interface DialogProps {
  children: ReactNode;
  description?: string;
  footer?: ReactNode;
  initialFocusRef?: RefObject<HTMLElement | null>;
  onClose: () => void;
  open: boolean;
  title: string;
}

export function Dialog({ children, description, footer, initialFocusRef, onClose, open, title }: DialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  const descriptionId = useId();

  useLayoutEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) {
      return;
    }

    let frame = 0;
    if (open && !dialog.open) {
      dialog.showModal();
      initialFocusRef?.current?.focus({ preventScroll: true });
      frame = window.requestAnimationFrame(() => initialFocusRef?.current?.focus({ preventScroll: true }));
    }

    if (!open && dialog.open) {
      dialog.close();
    }
    return () => window.cancelAnimationFrame(frame);
  }, [initialFocusRef, open]);

  function handleBackdropClick(event: React.MouseEvent<HTMLDialogElement>) {
    if (event.target === event.currentTarget) {
      onClose();
    }
  }

  return (
    <dialog
      aria-describedby={description ? descriptionId : undefined}
      aria-labelledby={titleId}
      className="dialog"
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
      onClick={handleBackdropClick}
      onKeyDown={(event) => {
        if (event.key === "Escape" && !event.defaultPrevented) {
          event.preventDefault();
          onClose();
        }
      }}
      ref={dialogRef}
    >
      <div className="dialog__panel">
        <header className="dialog__header">
          <div>
            <h2 id={titleId}>{title}</h2>
            {description ? <p id={descriptionId}>{description}</p> : null}
          </div>
          <button aria-label="Đóng hộp thoại" className="icon-button" onClick={onClose} type="button">
            <Icon name="close" />
          </button>
        </header>
        <div className="dialog__body">{children}</div>
        {footer ? <footer className="dialog__footer">{footer}</footer> : null}
      </div>
    </dialog>
  );
}
