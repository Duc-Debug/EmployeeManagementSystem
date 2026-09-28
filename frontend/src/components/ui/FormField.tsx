import type { ReactNode } from "react";

interface FormFieldProps {
  children: ReactNode;
  error?: string;
  hint?: string;
  id: string;
  label: string;
}

export function FormField({ children, error, hint, id, label }: FormFieldProps) {
  const message = error ?? hint ?? " ";

  return (
    <div className="field-group">
      <label htmlFor={id}>{label}</label>
      {children}
      <p className={error ? "field-error" : "field-hint"} id={`${id}-message`}>{message}</p>
    </div>
  );
}
