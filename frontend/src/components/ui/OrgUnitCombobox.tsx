"use client";

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useImperativeHandle,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type KeyboardEvent,
} from "react";
import { createPortal } from "react-dom";

import { Icon } from "@/components/ui/Icon";
import type { OrgUnitType } from "@/types/hrm";

export interface OrgUnitOption {
  depth: number;
  id: number;
  unitCode: string;
  unitName: string;
  unitType?: OrgUnitType;
}

interface ComboboxMenuPosition {
  direction: "above" | "below";
  left: number;
  maxBlockSize: number;
  top: number;
  width: number;
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
    return { label: "Công ty", className: "unit-tag--company" };
  }
  if (unitType === "CENTER" || depth === 1) {
    return { label: "Khối", className: "unit-tag--center" };
  }
  if (unitType === "DEPARTMENT" || depth === 2) {
    return { label: "Phòng ban", className: "unit-tag--dept" };
  }
  return { label: "Nhóm", className: "unit-tag--team" };
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
  const menuRef = useRef<HTMLDivElement>(null);
  const listboxId = useId();
  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [highlightedIndex, setHighlightedIndex] = useState(0);
  const [menuPortalTarget, setMenuPortalTarget] = useState<HTMLElement | null>(null);
  const [menuPosition, setMenuPosition] = useState<ComboboxMenuPosition | null>(null);
  const selectedOption = options.find((option) => option.id === Number(value));
  const filteredOptions = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase("vi");
    if (!normalizedQuery) {
      return options;
    }

    return options.filter((option) => [option.unitCode, option.unitName]
      .some((part) => part.toLocaleLowerCase("vi").includes(normalizedQuery)));
  }, [options, query]);

  useImperativeHandle(forwardedRef, () => triggerRef.current!);

  const updateMenuPosition = useCallback(() => {
    const trigger = triggerRef.current;
    if (!trigger) {
      return;
    }

    const viewportGutter = 8;
    const menuGap = 4;
    const maxBlockSize = 220;
    const triggerRect = trigger.getBoundingClientRect();
    const top = triggerRect.bottom + menuGap;

    setMenuPosition({
      direction: "below",
      left: Math.max(viewportGutter, triggerRect.left),
      maxBlockSize,
      top,
      width: triggerRect.width,
    });
  }, []);

  useLayoutEffect(() => {
    if (!isOpen) {
      return;
    }

    updateMenuPosition();
  }, [filteredOptions.length, isOpen, updateMenuPosition]);

  useEffect(() => {
    if (!isOpen) {
      return undefined;
    }

    const handleWindowChange = () => {
      updateMenuPosition();
    };

    window.addEventListener("resize", handleWindowChange);
    window.addEventListener("scroll", handleWindowChange, true);

    return () => {
      window.removeEventListener("resize", handleWindowChange);
      window.removeEventListener("scroll", handleWindowChange, true);
    };
  }, [isOpen, updateMenuPosition]);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    const selectedIndex = filteredOptions.findIndex((option) => option.id === Number(value));
    setHighlightedIndex(selectedIndex >= 0 ? selectedIndex : 0);
    searchRef.current?.focus();
  }, [filteredOptions, isOpen, value]);

  useEffect(() => {
    if (!isOpen) {
      return undefined;
    }

    function handlePointerDown(event: MouseEvent | TouchEvent) {
      const target = event.target as HTMLElement | null;
      if (!target) {
        return;
      }

      if (rootRef.current?.contains(target) || menuRef.current?.contains(target) || target.closest?.(".org-unit-combobox__menu")) {
        return;
      }

      setIsOpen(false);
      setQuery("");
    }

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("touchstart", handlePointerDown);

    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("touchstart", handlePointerDown);
    };
  }, [isOpen]);

  useEffect(() => {
    const dialog = rootRef.current?.closest("dialog");
    setMenuPortalTarget(dialog ?? document.body);
  }, []);

  function openMenu() {
    if (disabled) {
      return;
    }

    triggerRef.current?.scrollIntoView({ block: "nearest", behavior: "smooth" });
    setIsOpen(true);
  }

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
      openMenu();
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
        if (filteredOptions.length === 0) {
          return;
        }
        setHighlightedIndex((current) => (current + 1) % filteredOptions.length);
        break;
      case "ArrowUp":
        event.preventDefault();
        if (filteredOptions.length === 0) {
          return;
        }
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

  const menu = isOpen && menuPosition ? (
    <div
      className={menuPosition.direction === "above" ? "org-unit-combobox__menu is-above" : "org-unit-combobox__menu"}
      ref={menuRef}
      style={{
        "--org-unit-menu-max-block-size": `${menuPosition.maxBlockSize}px`,
        left: `${menuPosition.left}px`,
        top: `${menuPosition.top}px`,
        width: `${menuPosition.width}px`,
      } as CSSProperties}
    >
      <div className="org-unit-combobox__search">
        <Icon name="search" />
        <input
          aria-activedescendant={filteredOptions[highlightedIndex] ? `${listboxId}-${filteredOptions[highlightedIndex].id}` : undefined}
          aria-controls={listboxId}
          aria-expanded="true"
          aria-label="Tìm đơn vị tổ chức"
          className="input"
          onChange={(event) => {
            setQuery(event.target.value);
            setHighlightedIndex(0);
          }}
          onKeyDown={handleSearchKeyDown}
          placeholder="Tìm theo tên hoặc mã..."
          ref={searchRef}
          role="combobox"
          type="search"
          value={query}
        />
      </div>
      <ul aria-label="Kết quả đơn vị tổ chức" className="org-unit-combobox__list" id={listboxId} role="listbox">
        {filteredOptions.map((option, index) => {
          const isSelected = option.id === selectedOption?.id;
          const isHighlighted = index === highlightedIndex;
          const meta = getUnitTypeMeta(option.unitType, option.depth);
          const isBlockStart = option.depth === 1;

          return (
            <li
              className={isBlockStart ? "org-unit-combobox__item org-unit-combobox__item--block-start" : "org-unit-combobox__item"}
              key={option.id}
              role="presentation"
            >
              <button
                aria-selected={isSelected}
                className={`org-unit-combobox__option ${isHighlighted ? "is-highlighted" : ""} ${isSelected ? "is-selected" : ""}`}
                data-depth={Math.min(option.depth, 4)}
                id={`${listboxId}-${option.id}`}
                onClick={(e) => {
                  e.preventDefault();
                  selectOption(option);
                }}
                onMouseDown={(e) => {
                  e.preventDefault();
                  selectOption(option);
                }}
                onMouseMove={() => setHighlightedIndex(index)}
                role="option"
                type="button"
              >
                <div className="org-unit-combobox__option-main">
                  <div className="org-unit-combobox__tree-guide">
                    {option.depth === 0 ? (
                      <span className="org-unit-combobox__dot org-unit-combobox__dot--company" />
                    ) : option.depth === 1 ? (
                      <span className="org-unit-combobox__branch">├─</span>
                    ) : (
                      <span className="org-unit-combobox__branch">│&nbsp;&nbsp;└─</span>
                    )}
                  </div>
                  <div className="org-unit-combobox__text-group">
                    <div className="org-unit-combobox__title-row">
                      <strong className="org-unit-combobox__unit-name">{option.unitName}</strong>
                      <span className={`org-unit-tag ${meta.className}`}>{meta.label}</span>
                    </div>
                    <small className="org-unit-combobox__unit-code">{option.unitCode}</small>
                  </div>
                </div>
                {isSelected ? (
                  <span className="org-unit-combobox__selected-icon">
                    <Icon name="check" />
                  </span>
                ) : null}
              </button>
            </li>
          );
        })}
      </ul>
      {filteredOptions.length === 0 ? <p className="org-unit-combobox__empty">Không tìm thấy đơn vị phù hợp.</p> : null}
    </div>
  ) : null;

  return (
    <div className="org-unit-combobox" ref={rootRef}>
      <div className="org-unit-combobox__control">
        <button
          aria-controls={isOpen ? listboxId : undefined}
          aria-describedby={ariaDescribedBy}
          aria-expanded={isOpen}
          aria-haspopup="listbox"
          aria-invalid={ariaInvalid}
          className="w-full rounded-xl border border-slate-200 bg-slate-50/70 px-3.5 py-2 text-xs font-semibold text-slate-800 text-left outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100 flex items-center justify-between"
          disabled={disabled}
          id={id}
          onClick={openMenu}
          onKeyDown={handleTriggerKeyDown}
          ref={triggerRef}
          type="button"
        >
          <span className="truncate">
            {selectedOption ? `${selectedOption.unitCode} · ${selectedOption.unitName}` : placeholder}
          </span>
        </button>
      </div>
      {menuPortalTarget && menu ? createPortal(menu, menuPortalTarget) : null}
    </div>
  );
});
