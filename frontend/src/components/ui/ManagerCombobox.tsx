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

export interface ManagerOption {
  employeeId: number;
  fullName: string;
  roleCode: string;
  roleName: string;
  username: string;
}

interface ComboboxMenuPosition {
  direction: "above" | "below";
  left: number;
  maxBlockSize: number;
  top: number;
  width: number;
}

interface ManagerComboboxProps {
  allowClear?: boolean;
  ariaDescribedBy?: string;
  ariaInvalid?: boolean;
  disabled?: boolean;
  id: string;
  onChange: (value: string) => void;
  onEnter?: () => void;
  onKeyboardSelect?: () => void;
  options: readonly ManagerOption[];
  placeholder?: string;
  value: string;
}

export const ManagerCombobox = forwardRef<HTMLButtonElement, ManagerComboboxProps>(function ManagerCombobox(
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
    placeholder = "Chọn người quản lý",
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

  const selectedOption = options.find((option) => option.employeeId === Number(value));

  const filteredOptions = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase("vi");
    if (!normalizedQuery) {
      return options;
    }

    return options.filter((option) =>
      [option.fullName, option.username, option.roleCode, option.roleName]
        .some((part) => (part || "").toLocaleLowerCase("vi").includes(normalizedQuery)),
    );
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

    const selectedIndex = filteredOptions.findIndex((option) => option.employeeId === Number(value));
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

  function selectOption(option: ManagerOption | null, moveToNextField = false) {
    onChange(option ? String(option.employeeId) : "");
    closeMenu(!moveToNextField);
    if (moveToNextField) {
      onKeyboardSelect?.();
    }
  }

  function clearSelection(e: React.MouseEvent) {
    e.stopPropagation();
    onChange("");
    setQuery("");
    triggerRef.current?.focus();
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
          aria-activedescendant={filteredOptions[highlightedIndex] ? `${listboxId}-${filteredOptions[highlightedIndex].employeeId}` : undefined}
          aria-controls={listboxId}
          aria-expanded="true"
          aria-label="Tìm nhân sự quản lý"
          className="input"
          onChange={(event) => {
            setQuery(event.target.value);
            setHighlightedIndex(0);
          }}
          onKeyDown={handleSearchKeyDown}
          placeholder="Tìm theo tên, vai trò hoặc mã..."
          ref={searchRef}
          role="combobox"
          type="search"
          value={query}
        />
      </div>
      <ul aria-label="Kết quả nhân sự quản lý" className="org-unit-combobox__list" id={listboxId} role="listbox">
        {allowClear && (
          <li className="org-unit-combobox__item" role="presentation">
            <button
              aria-selected={!selectedOption}
              className={`org-unit-combobox__option ${!selectedOption ? "is-selected" : ""}`}
              onClick={(e) => {
                e.preventDefault();
                selectOption(null);
              }}
              onMouseDown={(e) => {
                e.preventDefault();
                selectOption(null);
              }}
              role="option"
              type="button"
            >
              <div className="org-unit-combobox__option-main">
                <div className="org-unit-combobox__text-group">
                  <span style={{ color: "#64748b", fontStyle: "italic", fontSize: "0.75rem" }}>
                    -- Không gán người quản lý --
                  </span>
                </div>
              </div>
            </button>
          </li>
        )}

        {filteredOptions.map((option, index) => {
          const isSelected = option.employeeId === selectedOption?.employeeId;
          const isHighlighted = index === highlightedIndex;

          return (
            <li className="org-unit-combobox__item" key={option.employeeId} role="presentation">
              <button
                aria-selected={isSelected}
                className={`org-unit-combobox__option ${isHighlighted ? "is-highlighted" : ""} ${isSelected ? "is-selected" : ""}`}
                id={`${listboxId}-${option.employeeId}`}
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
                  <div className="org-unit-combobox__text-group">
                    <div className="org-unit-combobox__title-row">
                      <strong className="org-unit-combobox__unit-name">{option.fullName}</strong>
                      <span className="role-chip" style={{ fontSize: "0.625rem" }}>
                        {option.roleCode} · {option.roleName}
                      </span>
                    </div>
                    <small className="org-unit-combobox__unit-code">@{option.username}</small>
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
      {filteredOptions.length === 0 ? (
        <p className="org-unit-combobox__empty">Không tìm thấy nhân sự phù hợp.</p>
      ) : null}
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
          className={selectedOption ? "org-unit-combobox__trigger" : "org-unit-combobox__trigger is-placeholder"}
          disabled={disabled}
          id={id}
          onClick={openMenu}
          onKeyDown={handleTriggerKeyDown}
          ref={triggerRef}
          type="button"
        >
          <span>
            {selectedOption ? `${selectedOption.fullName} (${selectedOption.roleCode}) ${selectedOption.roleName}` : placeholder}
          </span>
          <Icon name="chevronDown" />
        </button>
        {allowClear && selectedOption && !disabled ? (
          <button
            aria-label="Xóa lựa chọn quản lý"
            className="org-unit-combobox__clear"
            onClick={clearSelection}
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
