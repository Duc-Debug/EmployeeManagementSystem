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

export interface OrgUnitOption {
  depth: number;
  id: number;
  unitCode: string;
  unitName: string;
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

export const OrgUnitCombobox = forwardRef<HTMLButtonElement, OrgUnitComboboxProps>(function OrgUnitCombobox(
  {
    allowClear = true,
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
    const preferredMenuHeight = 288;
    const triggerRect = trigger.getBoundingClientRect();
    const dialog = trigger.closest("dialog");
    const dialogRect = dialog?.getBoundingClientRect();
    const headerRect = dialog?.querySelector(".dialog__header")?.getBoundingClientRect();
    const footerRect = dialog?.querySelector(".dialog__footer")?.getBoundingClientRect();
    const menuTopBoundary = headerRect ? headerRect.bottom + viewportGutter : (dialogRect ? dialogRect.top + viewportGutter : viewportGutter);
    const menuBottomBoundary = footerRect ? footerRect.top - viewportGutter : (dialogRect ? dialogRect.bottom - viewportGutter : window.innerHeight - viewportGutter);
    const availableAbove = Math.max(triggerRect.top - menuTopBoundary - menuGap, 0);
    const availableBelow = Math.max(menuBottomBoundary - triggerRect.bottom - menuGap, 0);
    const direction = availableBelow >= preferredMenuHeight || availableBelow >= availableAbove ? "below" : "above";
    const maxBlockSize = Math.max(Math.min(direction === "below" ? availableBelow : availableAbove, preferredMenuHeight), 120);
    const width = Math.min(triggerRect.width, Math.max(window.innerWidth - viewportGutter * 2, 0));
    const left = Math.min(Math.max(triggerRect.left, viewportGutter), Math.max(window.innerWidth - width - viewportGutter, viewportGutter));

    setMenuPosition({
      direction,
      left,
      maxBlockSize,
      top: direction === "below" ? triggerRect.bottom + menuGap : triggerRect.top - menuGap,
      width,
    });
  }, []);

  useLayoutEffect(() => {
    if (!isOpen) {
      return;
    }

    updateMenuPosition();
    window.addEventListener("resize", updateMenuPosition);
    window.addEventListener("scroll", updateMenuPosition, true);
    return () => {
      window.removeEventListener("resize", updateMenuPosition);
      window.removeEventListener("scroll", updateMenuPosition, true);
    };
  }, [isOpen, updateMenuPosition]);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    const frame = window.requestAnimationFrame(() => searchRef.current?.focus());
    return () => window.cancelAnimationFrame(frame);
  }, [isOpen]);

  useEffect(() => {
    if (highlightedIndex >= filteredOptions.length) {
      setHighlightedIndex(Math.max(filteredOptions.length - 1, 0));
    }
  }, [filteredOptions.length, highlightedIndex]);

  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      if (!rootRef.current?.contains(event.target as Node) && !menuRef.current?.contains(event.target as Node)) {
        setIsOpen(false);
        setQuery("");
        setMenuPortalTarget(null);
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, []);

  function openMenu() {
    const portalTarget = triggerRef.current?.closest<HTMLElement>("dialog") ?? document.body;
    const selectedIndex = filteredOptions.findIndex((option) => option.id === selectedOption?.id);
    setHighlightedIndex(selectedIndex >= 0 ? selectedIndex : 0);
    setMenuPortalTarget(portalTarget);
    setIsOpen(true);
  }

  function closeMenu(restoreFocus = false) {
    setIsOpen(false);
    setQuery("");
    setMenuPortalTarget(null);
    if (restoreFocus) {
      window.requestAnimationFrame(() => triggerRef.current?.focus());
    }
  }

  function selectOption(option: OrgUnitOption, moveToNextField = false) {
    onChange(String(option.id));
    setIsOpen(false);
    setQuery("");
    setMenuPortalTarget(null);
    window.requestAnimationFrame(() => {
      if (moveToNextField && onKeyboardSelect) {
        onKeyboardSelect();
        return;
      }

      triggerRef.current?.focus();
    });
  }

  function moveHighlight(nextIndex: number) {
    if (filteredOptions.length === 0) {
      return;
    }

    const wrappedIndex = (nextIndex + filteredOptions.length) % filteredOptions.length;
    setHighlightedIndex(wrappedIndex);
  }

  function handleTriggerKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (event.nativeEvent.isComposing || disabled) {
      return;
    }

    if (event.key === "ArrowDown" || event.key === " ") {
      event.preventDefault();
      openMenu();
      return;
    }

    if (event.key === "Enter") {
      event.preventDefault();
      if (value && onEnter) {
        onEnter();
        return;
      }

      openMenu();
    }
  }

  function handleSearchKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.nativeEvent.isComposing) {
      return;
    }

    switch (event.key) {
      case "ArrowDown":
        event.preventDefault();
        moveHighlight(highlightedIndex + 1);
        break;
      case "ArrowUp":
        event.preventDefault();
        moveHighlight(highlightedIndex - 1);
        break;
      case "Home":
        event.preventDefault();
        setHighlightedIndex(0);
        break;
      case "End":
        event.preventDefault();
        setHighlightedIndex(Math.max(filteredOptions.length - 1, 0));
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
          placeholder="Tìm theo tên hoặc mã"
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
          return (
            <li key={option.id} role="presentation">
              <button
                aria-selected={isSelected}
                className={isHighlighted ? "org-unit-combobox__option is-highlighted" : "org-unit-combobox__option"}
                data-depth={Math.min(option.depth, 4)}
                id={`${listboxId}-${option.id}`}
                onClick={() => selectOption(option)}
                onMouseMove={() => setHighlightedIndex(index)}
                role="option"
                type="button"
              >
                <span className="org-unit-combobox__option-copy">
                  <strong>{option.unitName}</strong>
                  <small>{option.unitCode}</small>
                </span>
                {isSelected ? <Icon name="check" /> : null}
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
          aria-invalid={ariaInvalid || undefined}
          className={selectedOption ? "org-unit-combobox__trigger" : "org-unit-combobox__trigger is-placeholder"}
          disabled={disabled}
          id={id}
          onClick={openMenu}
          onKeyDown={handleTriggerKeyDown}
          ref={triggerRef}
          type="button"
        >
          <span>{selectedOption ? `${selectedOption.unitCode} · ${selectedOption.unitName}` : placeholder}</span>
          <Icon name="chevronDown" />
        </button>
        {allowClear && value && !disabled ? (
          <button
            aria-label="Bỏ chọn đơn vị tổ chức"
            className="org-unit-combobox__clear"
            onClick={() => {
              onChange("");
              closeMenu(true);
            }}
            type="button"
          >
            <Icon name="close" />
          </button>
        ) : null}
      </div>

      {menuPortalTarget && menu ? createPortal(menu, menuPortalTarget) : null}
    </div>
  );
});
