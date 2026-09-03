"use client";

import {
  forwardRef,
  useEffect,
  useId,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
  type KeyboardEvent,
} from "react";
import { ChevronDown, Search, Check } from "lucide-react";
import { cn } from "@/lib/utils";
import type { OrgUnitType } from "@/types/hrm";

export interface OrgUnitOption {
  depth: number;
  id: number;
  unitCode: string;
  unitName: string;
  unitType?: OrgUnitType;
}

interface OrgUnitComboboxProps {
  allowClear?: boolean;
  ariaDescribedBy?: string;
  ariaInvalid?: boolean;
  disabled?: boolean;
  id: string;
  onChange: (value: string) => void;
  onEnter?: () => void;
  onKeyboardSelect?: () => void;
  options: readonly OrgUnitOption[];
  placeholder: string;
  value: string;
}

function getUnitTypeMeta(unitType?: OrgUnitType, depth = 0) {
  if (unitType === "COMPANY" || depth === 0) {
    return { label: "Công ty", className: "bg-indigo-50 text-indigo-700 border border-indigo-200" };
  }
  if (unitType === "CENTER" || depth === 1) {
    return { label: "Khối", className: "bg-blue-50 text-blue-700 border border-blue-200" };
  }
  if (unitType === "DEPARTMENT" || depth === 2) {
    return { label: "Phòng ban", className: "bg-emerald-50 text-emerald-700 border border-emerald-200" };
  }
  return { label: "Nhóm", className: "bg-amber-50 text-amber-700 border border-amber-200" };
}

export const OrgUnitCombobox = forwardRef<HTMLButtonElement, OrgUnitComboboxProps>(function OrgUnitCombobox(
  {
    ariaDescribedBy,
    ariaInvalid,
    disabled = false,
    id,
    onChange,
    onEnter,
    onKeyboardSelect,
    options,
    placeholder,
    value,
  },
  forwardedRef,
) {
  const rootRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const searchRef = useRef<HTMLInputElement>(null);
  const listboxId = useId();
  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [highlightedIndex, setHighlightedIndex] = useState(0);

  const selectedOption = value ? options.find((option) => String(option.id) === String(value)) : undefined;

  const filteredOptions = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase("vi");
    if (!normalizedQuery) {
      return options;
    }

    return options.filter((option) =>
      [option.unitCode, option.unitName].some((part) =>
        part.toLocaleLowerCase("vi").includes(normalizedQuery)
      )
    );
  }, [options, query]);

  useImperativeHandle(forwardedRef, () => triggerRef.current!);

  // Focus search input when menu opens
  useEffect(() => {
    if (!isOpen) {
      return;
    }

    const selectedIndex = value
      ? filteredOptions.findIndex((option) => String(option.id) === String(value))
      : -1;
    setHighlightedIndex(selectedIndex >= 0 ? selectedIndex : 0);

    const timer = setTimeout(() => {
      searchRef.current?.focus();
    }, 50);
    return () => clearTimeout(timer);
  }, [isOpen]);

  // Click outside listener
  useEffect(() => {
    if (!isOpen) {
      return undefined;
    }

    function handlePointerDown(event: MouseEvent | TouchEvent) {
      const target = event.target as Node | null;
      if (!target || !rootRef.current) {
        return;
      }

      if (!rootRef.current.contains(target)) {
        setIsOpen(false);
        setQuery("");
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("touchstart", handlePointerDown);

    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("touchstart", handlePointerDown);
    };
  }, [isOpen]);

  function closeMenu(focusTrigger = false) {
    setIsOpen(false);
    setQuery("");
    if (focusTrigger) {
      triggerRef.current?.focus();
    }
  }

  function selectOption(option: OrgUnitOption, moveToNextField = false) {
    onChange(String(option.id));
    closeMenu(!moveToNextField);
    if (moveToNextField) {
      onKeyboardSelect?.();
    }
  }

  function handleTriggerKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (disabled) {
      return;
    }

    if (event.key === "ArrowDown" || event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      setIsOpen(true);
      return;
    }

    if (event.key === "Enter") {
      onEnter?.();
    }
  }

  function handleSearchKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.nativeEvent.isComposing) {
      return;
    }

    switch (event.key) {
      case "ArrowDown":
        event.preventDefault();
        if (filteredOptions.length === 0) return;
        setHighlightedIndex((current) => (current + 1) % filteredOptions.length);
        break;
      case "ArrowUp":
        event.preventDefault();
        if (filteredOptions.length === 0) return;
        setHighlightedIndex((current) => (current - 1 + filteredOptions.length) % filteredOptions.length);
        break;
      case "Enter": {
        event.preventDefault();
        const option = filteredOptions[highlightedIndex];
        if (option) {
          selectOption(option, true);
        }
        break;
      }
      case "Escape":
        event.preventDefault();
        closeMenu(true);
        break;
      case "Tab":
        closeMenu();
        break;
      default:
        break;
    }
  }

  return (
    <div className="relative w-full" ref={rootRef}>
      {/* Trigger Button */}
      <button
        aria-controls={isOpen ? listboxId : undefined}
        aria-describedby={ariaDescribedBy}
        aria-expanded={isOpen}
        aria-haspopup="listbox"
        aria-invalid={ariaInvalid}
        className={cn(
          "w-full rounded-xl border border-slate-200 bg-slate-50/70 px-3.5 py-2 text-xs font-semibold text-slate-800 text-left outline-none transition flex items-center justify-between",
          isOpen
            ? "border-indigo-500 bg-white ring-2 ring-indigo-100"
            : "hover:bg-slate-100/70 focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100",
          disabled && "cursor-not-allowed opacity-50"
        )}
        disabled={disabled}
        id={id}
        onClick={() => {
          if (!disabled) {
            setIsOpen((prev) => !prev);
          }
        }}
        onKeyDown={handleTriggerKeyDown}
        ref={triggerRef}
        type="button"
      >
        <span className={cn("truncate", !selectedOption && "text-slate-400 font-normal")}>
          {selectedOption ? `${selectedOption.unitCode} · ${selectedOption.unitName}` : placeholder}
        </span>
        <ChevronDown
          className={cn(
            "size-4 text-slate-400 shrink-0 transition-transform duration-200 ml-2",
            isOpen && "rotate-180 text-indigo-600"
          )}
        />
      </button>

      {/* Inline Dropdown Menu */}
      {isOpen && (
        <div
          className="absolute left-0 right-0 top-full z-50 mt-1.5 flex flex-col rounded-2xl border border-slate-200 bg-white p-2 shadow-2xl animate-in fade-in zoom-in-95 duration-100"
          style={{ maxHeight: "280px" }}
        >
          {/* Search Box */}
          <div className="relative mb-2 border-b border-slate-100 pb-2">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-3.5 -translate-y-1/2 text-slate-400" />
            <input
              aria-activedescendant={
                filteredOptions[highlightedIndex]
                  ? `${listboxId}-${filteredOptions[highlightedIndex].id}`
                  : undefined
              }
              aria-controls={listboxId}
              aria-expanded="true"
              aria-label="Tìm đơn vị tổ chức"
              className="w-full rounded-xl border border-slate-200 bg-slate-50/80 py-1.5 pl-8 pr-3 text-xs font-semibold text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
              onChange={(e) => {
                setQuery(e.target.value);
                setHighlightedIndex(0);
              }}
              onKeyDown={handleSearchKeyDown}
              placeholder="Tìm theo tên hoặc mã phòng ban..."
              ref={searchRef}
              role="combobox"
              type="search"
              value={query}
            />
          </div>

          {/* Org Unit Tree List */}
          <ul
            aria-label="Kết quả đơn vị tổ chức"
            className="flex-1 overflow-y-auto space-y-0.5 [scrollbar-width:thin] [scrollbar-color:#cbd5e1_transparent] max-h-[200px]"
            id={listboxId}
            role="listbox"
          >
            {filteredOptions.map((option, index) => {
              const isSelected = String(option.id) === String(value);
              const isHighlighted = index === highlightedIndex;
              const meta = getUnitTypeMeta(option.unitType, option.depth);

              return (
                <li
                  className={option.depth === 1 ? "pt-1 border-t border-slate-100" : ""}
                  key={option.id}
                  role="presentation"
                >
                  <button
                    aria-selected={isSelected}
                    className={cn(
                      "w-full flex items-center justify-between rounded-xl px-2.5 py-1.5 text-left text-xs transition cursor-pointer group",
                      isSelected
                        ? "bg-indigo-50 text-indigo-900 font-bold"
                        : isHighlighted
                        ? "bg-slate-100 text-slate-900"
                        : "text-slate-700 hover:bg-slate-50"
                    )}
                    id={`${listboxId}-${option.id}`}
                    onClick={(e) => {
                      e.preventDefault();
                      selectOption(option);
                    }}
                    onMouseEnter={() => setHighlightedIndex(index)}
                    role="option"
                    type="button"
                  >
                    <div className="flex items-center gap-2 min-w-0">
                      {/* Tree Indentation Guides */}
                      <span className="font-mono text-slate-400 select-none text-[11px] shrink-0">
                        {option.depth === 0
                          ? "🏢"
                          : option.depth === 1
                          ? "├─"
                          : option.depth === 2
                          ? "│  ├─"
                          : "│  │  └─"}
                      </span>
                      <span className="truncate">{option.unitName}</span>
                      <span
                        className={cn(
                          "text-[9px] px-1.5 py-0.2 rounded-md font-bold shrink-0 uppercase",
                          meta.className
                        )}
                      >
                        {meta.label}
                      </span>
                    </div>

                    <div className="flex items-center gap-2 shrink-0 ml-2">
                      <span className="text-[10px] text-slate-400 font-mono">
                        {option.unitCode}
                      </span>
                      {isSelected && <Check className="size-3.5 text-indigo-600 shrink-0" />}
                    </div>
                  </button>
                </li>
              );
            })}

            {filteredOptions.length === 0 && (
              <li className="py-4 text-center text-xs text-slate-400">
                Không tìm thấy đơn vị nào phù hợp.
              </li>
            )}
          </ul>
        </div>
      )}
    </div>
  );
});
