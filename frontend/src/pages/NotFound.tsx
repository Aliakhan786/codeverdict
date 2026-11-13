import { useNavigate } from "react-router-dom";
import { EmptyState } from "@/components/EmptyState";
import { Button } from "@/components/ui/button";

export default function NotFoundPage() {
  const navigate = useNavigate();
  return (
    <EmptyState
      title="Case not found"
      message="This page does not exist (or was dismissed)."
      action={<Button variant="gold" onClick={() => navigate("/")}>Back to the lobby</Button>}
    />
  );
}
