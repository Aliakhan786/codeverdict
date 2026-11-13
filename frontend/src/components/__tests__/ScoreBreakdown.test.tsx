import { render, screen } from "@testing-library/react";
import { TooltipProvider } from "@radix-ui/react-tooltip";
import { describe, expect, it } from "vitest";
import { ScoreBreakdown } from "@/components/ScoreBreakdown";

describe("ScoreBreakdown", () => {
  it("shows each part out of its maximum", () => {
    render(
      <TooltipProvider>
        <ScoreBreakdown scores={{ correctness: 35, performance: 20, edgeCases: 7, quality: 15, partial: false }} />
      </TooltipProvider>,
    );
    expect(screen.getByText("35 / 40")).toBeInTheDocument();
    expect(screen.getByText("20 / 30")).toBeInTheDocument();
    expect(screen.getByText("7 / 10")).toBeInTheDocument();
    expect(screen.getByText("15 / 20")).toBeInTheDocument();
    expect(screen.getAllByText("Measured")).toHaveLength(3);
    expect(screen.getByText("AI-rated")).toBeInTheDocument();
  });

  it("says when a part was not measured and the score was scaled", () => {
    render(
      <TooltipProvider>
        <ScoreBreakdown scores={{ correctness: 40, performance: 30, partial: true }} />
      </TooltipProvider>,
    );
    expect(screen.getAllByText("not measured")).toHaveLength(2);
    expect(screen.getByText(/scaled over the parts that were/)).toBeInTheDocument();
  });
});
