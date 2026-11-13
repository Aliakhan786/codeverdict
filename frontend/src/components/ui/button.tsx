import { forwardRef, type ButtonHTMLAttributes } from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg text-sm font-medium transition-all " +
    "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold/60 disabled:pointer-events-none disabled:opacity-50 " +
    "active:scale-[0.98]",
  {
    variants: {
      variant: {
        gold: "bg-gold text-[#1a1406] shadow-gold hover:brightness-110 font-semibold",
        primary: "bg-fg text-bg hover:opacity-90",
        outline: "border border-line bg-transparent hover:bg-raised text-fg",
        ghost: "hover:bg-raised text-muted hover:text-fg",
        danger: "bg-guilty/15 text-guilty border border-guilty/40 hover:bg-guilty/25",
        success: "bg-acquitted/15 text-acquitted border border-acquitted/40 hover:bg-acquitted/25",
      },
      size: {
        sm: "h-8 px-3 text-xs",
        md: "h-10 px-4",
        lg: "h-12 px-6 text-base",
        icon: "h-9 w-9",
      },
    },
    defaultVariants: { variant: "outline", size: "md" },
  },
);

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof buttonVariants> {}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(({ className, variant, size, ...props }, ref) => (
  <button ref={ref} className={cn(buttonVariants({ variant, size }), className)} {...props} />
));
Button.displayName = "Button";
