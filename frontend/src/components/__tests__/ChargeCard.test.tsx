import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ChargeCard } from "@/components/ChargeCard";
import type { Charge } from "@/types/api";

const base: Charge = {
  id: "c1",
  category: "BUG",
  severity: "HIGH",
  lineStart: 3,
  lineEnd: 3,
  title: "Wrong answer when all numbers are negative",
  explanation: "best starts at 0",
  status: "PROVEN",
  statusReason: "Fails on your code, passes on the trusted reference",
  badEvidence: false,
  proofTestCode: "@Test void proof() { assertEquals(-1, new Solution().maxSubArray(new int[]{-3, -1, -2})); }",
  proofOutput: "AssertionFailedError: expected: <-1> but was: <0>",
  mistakeTag: "WRONG_CONDITION",
  mistakeLabel: "Wrong condition",
};

describe("ChargeCard", () => {
  it("shows a proven charge as Guilty with its evidence open", () => {
    render(<ChargeCard charge={base} index={0} />);
    expect(screen.getByText("Guilty")).toBeInTheDocument();
    expect(screen.getByText(/expected: <-1> but was: <0>/)).toBeInTheDocument();
    expect(screen.getByText(/line 3/)).toBeInTheDocument();
  });

  it("collapses dismissed charges and explains bad evidence when opened", () => {
    render(<ChargeCard charge={{ ...base, status: "DISMISSED", badEvidence: true }} index={0} />);
    expect(screen.getByText("Dismissed")).toBeInTheDocument();
    expect(screen.queryByText(/double-check rule/)).not.toBeInTheDocument();
    fireEvent.click(screen.getByText(base.title));
    expect(screen.getByText(/double-check rule/)).toBeInTheDocument();
  });

  it("offers a verified fix for acquitted charges", () => {
    const onApply = vi.fn();
    render(
      <ChargeCard
        charge={{ ...base, status: "ACQUITTED", suggestedFixCode: "class Solution {}", fixVerified: true, fixExplanation: "Start at MIN_VALUE" }}
        index={0}
        onApplyFix={onApply}
      />,
    );
    expect(screen.getByText("Acquitted")).toBeInTheDocument();
    expect(screen.getByText(/test passes after fix/)).toBeInTheDocument();
    fireEvent.click(screen.getByText("Apply fix in editor"));
    expect(onApply).toHaveBeenCalledOnce();
  });

  it("jumps to the evidence line", () => {
    const onShow = vi.fn();
    render(<ChargeCard charge={{ ...base, evidenceLine: 7 }} index={0} onShowLines={onShow} />);
    fireEvent.click(screen.getByText(/line 3/));
    expect(onShow).toHaveBeenCalledWith(7);
  });
});
