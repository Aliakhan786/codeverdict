import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Dialog } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input, Label, Select, Textarea } from "@/components/ui/input";
import { useSaveProblem } from "@/api/hooks";
import { ApiError } from "@/api/client";
import { COMPARE_MODES, DIFFICULTIES, SAMPLE_INPUT_HELP, TOPICS } from "@/lib/constants";
import type { Problem, ProblemRequest } from "@/types/api";

const EMPTY: ProblemRequest = {
  title: "",
  statement: "",
  topic: "ARRAYS",
  difficulty: "EASY",
  methodSignature: "",
  sampleInput: "",
  sampleOutput: "",
  compareMode: "EXACT",
};

export function ProblemDialog({ open, onOpenChange, problem, onSaved }: {
  open: boolean; onOpenChange: (o: boolean) => void; problem?: Problem; onSaved?: (p: Problem) => void;
}) {
  const [form, setForm] = useState<ProblemRequest>(EMPTY);
  const save = useSaveProblem();

  useEffect(() => {
    if (open) {
      setForm(problem
        ? {
            title: problem.title, statement: problem.statement, topic: problem.topic, difficulty: problem.difficulty,
            methodSignature: problem.methodSignature, sampleInput: problem.sampleInput ?? "",
            sampleOutput: problem.sampleOutput ?? "", compareMode: problem.compareMode,
          }
        : EMPTY);
    }
  }, [open, problem]);

  const set = <K extends keyof ProblemRequest>(k: K, v: ProblemRequest[K]) => setForm((f) => ({ ...f, [k]: v }));

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const saved = await save.mutateAsync({ id: problem?.id, body: form });
      toast.success(problem ? "Problem updated" : "Problem added to your docket");
      onOpenChange(false);
      onSaved?.(saved);
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Could not save the problem");
    }
  };

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title={problem ? "Edit problem" : "Add a practice problem"}
      description="Write the method signature exactly; your Solution class must have this method."
    >
      <form onSubmit={submit} className="space-y-4">
        <div>
          <Label>Title</Label>
          <Input required maxLength={200} value={form.title} onChange={(e) => set("title", e.target.value)} placeholder="Two Sum" />
        </div>
        <div>
          <Label hint="include constraints - the AI uses them to build valid tests">Statement</Label>
          <Textarea required rows={6} maxLength={10000} value={form.statement} onChange={(e) => set("statement", e.target.value)} />
        </div>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          <div>
            <Label>Topic</Label>
            <Select value={form.topic} onChange={(e) => set("topic", e.target.value as ProblemRequest["topic"])}>
              {TOPICS.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
            </Select>
          </div>
          <div>
            <Label>Difficulty</Label>
            <Select value={form.difficulty} onChange={(e) => set("difficulty", e.target.value as ProblemRequest["difficulty"])}>
              {DIFFICULTIES.map((d) => <option key={d} value={d}>{d.charAt(0) + d.slice(1).toLowerCase()}</option>)}
            </Select>
          </div>
          <div className="col-span-2 sm:col-span-1">
            <Label>Output order</Label>
            <Select value={form.compareMode} onChange={(e) => set("compareMode", e.target.value as ProblemRequest["compareMode"])}>
              {COMPARE_MODES.map((m) => <option key={m.value} value={m.value} title={m.hint}>{m.label}</option>)}
            </Select>
          </div>
        </div>
        <div>
          <Label>Method signature</Label>
          <Input required className="font-mono" value={form.methodSignature} onChange={(e) => set("methodSignature", e.target.value)} placeholder="int[] twoSum(int[] nums, int target)" />
        </div>
        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <Label hint="Java arguments">Sample input</Label>
            <Input className="font-mono" value={form.sampleInput} onChange={(e) => set("sampleInput", e.target.value)} placeholder="new int[]{2, 7, 11, 15}, 9" />
          </div>
          <div>
            <Label>Sample output</Label>
            <Input className="font-mono" value={form.sampleOutput} onChange={(e) => set("sampleOutput", e.target.value)} placeholder="[0, 1]" />
          </div>
        </div>
        <p className="text-xs text-muted">{SAMPLE_INPUT_HELP}</p>
        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button variant="gold" disabled={save.isPending}>{save.isPending ? "Saving..." : problem ? "Save changes" : "Add problem"}</Button>
        </div>
      </form>
    </Dialog>
  );
}
