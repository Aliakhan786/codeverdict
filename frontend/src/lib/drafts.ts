/** Editor drafts are autosaved per problem in localStorage, so nothing is lost on refresh. */
const key = (problemId: string) => `cv:draft:${problemId}`;

export function loadDraft(problemId: string): string | null {
  try {
    return localStorage.getItem(key(problemId));
  } catch {
    return null;
  }
}

export function saveDraft(problemId: string, code: string) {
  try {
    localStorage.setItem(key(problemId), code);
  } catch {
    /* storage full or disabled */
  }
}

export function clearDraft(problemId: string) {
  try {
    localStorage.removeItem(key(problemId));
  } catch {
    /* ignore */
  }
}
