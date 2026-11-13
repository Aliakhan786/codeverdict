import confetti from "canvas-confetti";

/** Confetti only for real wins: personal best, new badge, level-up. */
export function celebrate(big = false) {
  const colors = ["#e8b84a", "#f5d27a", "#34c778", "#ffffff"];
  confetti({ particleCount: big ? 160 : 90, spread: big ? 100 : 70, origin: { y: 0.35 }, colors, disableForReducedMotion: true });
  if (big) {
    setTimeout(() => confetti({ particleCount: 80, angle: 60, spread: 60, origin: { x: 0, y: 0.6 }, colors }), 250);
    setTimeout(() => confetti({ particleCount: 80, angle: 120, spread: 60, origin: { x: 1, y: 0.6 }, colors }), 400);
  }
}

/** A short "gavel" knock made with the Web Audio API (no audio files). Off unless enabled in Settings. */
export function gavelSound() {
  try {
    const Ctx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    const ctx = new Ctx();
    [0, 0.16].forEach((t) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = "triangle";
      osc.frequency.setValueAtTime(180, ctx.currentTime + t);
      osc.frequency.exponentialRampToValueAtTime(60, ctx.currentTime + t + 0.12);
      gain.gain.setValueAtTime(0.35, ctx.currentTime + t);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + t + 0.15);
      osc.connect(gain).connect(ctx.destination);
      osc.start(ctx.currentTime + t);
      osc.stop(ctx.currentTime + t + 0.16);
    });
    setTimeout(() => ctx.close(), 800);
  } catch {
    /* audio not available */
  }
}
