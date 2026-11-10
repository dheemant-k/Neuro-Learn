package com.neurolearn.evaluation;

import com.neurolearn.adaptive.BktParameters;

import java.util.List;

/**
 * Fits BKT parameters (P(L0), P(T), P(G), P(S)) to observed right/wrong sequences by maximum
 * likelihood: a coarse grid search followed by coordinate refinement. Each sequence is one
 * student's attempts on one topic, oldest first.
 *
 * <p>Constraints keep the model identifiable (the usual BKT "degenerate" fits are excluded):
 * P(G) ≤ 0.40, P(S) ≤ 0.30.
 */
public final class BktFitter {

    public record Fit(double pInit, double pTransit, double pGuess, double pSlip,
                      double logLikelihood, int sequences, int observations) {

        /** Average negative log-likelihood per answer (lower is better). */
        public double logLossPerAnswer() {
            return observations == 0 ? Double.NaN : -logLikelihood / observations;
        }

        public BktParameters toParameters() {
            return new BktParameters(pInit, pTransit, pGuess, pSlip, 0.85);
        }
    }

    private BktFitter() {
    }

    public static double logLikelihood(List<List<Boolean>> sequences, double pInit, double pT, double pG, double pS) {
        double ll = 0;
        for (List<Boolean> seq : sequences) {
            double pL = pInit;
            for (boolean correct : seq) {
                double pCorrect = pL * (1 - pS) + (1 - pL) * pG;
                pCorrect = Math.min(1 - 1e-9, Math.max(1e-9, pCorrect));
                ll += Math.log(correct ? pCorrect : 1 - pCorrect);
                double posterior = correct
                        ? pL * (1 - pS) / pCorrect
                        : pL * pS / (1 - pCorrect);
                pL = posterior + (1 - posterior) * pT;
            }
        }
        return ll;
    }

    public static Fit evaluate(List<List<Boolean>> sequences, BktParameters p) {
        return new Fit(p.pInit(), p.pTransit(), p.pGuess(), p.pSlip(),
                logLikelihood(sequences, p.pInit(), p.pTransit(), p.pGuess(), p.pSlip()),
                sequences.size(), count(sequences));
    }

    public static Fit fit(List<List<Boolean>> sequences) {
        double[] best = {0.2, 0.1, 0.2, 0.1};
        double bestLl = Double.NEGATIVE_INFINITY;
        // 1. Coarse grid
        for (double l0 = 0.05; l0 <= 0.95; l0 += 0.10)
            for (double t = 0.02; t <= 0.50; t += 0.04)
                for (double g = 0.05; g <= 0.40; g += 0.05)
                    for (double s = 0.02; s <= 0.30; s += 0.04) {
                        double ll = logLikelihood(sequences, l0, t, g, s);
                        if (ll > bestLl) {
                            bestLl = ll;
                            best = new double[]{l0, t, g, s};
                        }
                    }
        // 2. Coordinate refinement with shrinking steps
        double[] lo = {0.001, 0.001, 0.001, 0.001};
        double[] hi = {0.999, 0.999, 0.40, 0.30};
        for (double step = 0.04; step >= 0.0025; step /= 2) {
            boolean improved = true;
            while (improved) {
                improved = false;
                for (int k = 0; k < 4; k++) {
                    for (double dir : new double[]{-1, 1}) {
                        double[] cand = best.clone();
                        cand[k] = Math.min(hi[k], Math.max(lo[k], cand[k] + dir * step));
                        double ll = logLikelihood(sequences, cand[0], cand[1], cand[2], cand[3]);
                        if (ll > bestLl + 1e-12) {
                            bestLl = ll;
                            best = cand;
                            improved = true;
                        }
                    }
                }
            }
        }
        return new Fit(best[0], best[1], best[2], best[3], bestLl, sequences.size(), count(sequences));
    }

    private static int count(List<List<Boolean>> sequences) {
        return sequences.stream().mapToInt(List::size).sum();
    }
}
