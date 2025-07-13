package com.lin1000.justdance.audio;

public class FFT {
    public static void fft(double[] real, double[] imag) {
        int n = real.length;
        if (n == 0) return;
        if ((n & (n - 1)) != 0) throw new IllegalArgumentException("n必須是2的次方");

        int bits = (int) (Math.log(n) / Math.log(2));
        for (int j = 1; j < n / 2; j++) {
            int swapPos = Integer.reverse(j) >>> (32 - bits);
            if (swapPos > j) {
                double tempReal = real[j];
                double tempImag = imag[j];
                real[j] = real[swapPos];
                imag[j] = imag[swapPos];
                real[swapPos] = tempReal;
                imag[swapPos] = tempImag;
            }
        }

        for (int size = 2; size <= n; size <<= 1) {
            int halfsize = size >> 1;
            double phaseShiftStepReal = Math.cos(-2 * Math.PI / size);
            double phaseShiftStepImag = Math.sin(-2 * Math.PI / size);
            double currentPhaseShiftReal = 1.0;
            double currentPhaseShiftImag = 0.0;

            for (int fftStep = 0; fftStep < halfsize; fftStep++) {
                for (int i = fftStep; i < n; i += size) {
                    int off = i + halfsize;
                    double tr = (currentPhaseShiftReal * real[off]) - (currentPhaseShiftImag * imag[off]);
                    double ti = (currentPhaseShiftReal * imag[off]) + (currentPhaseShiftImag * real[off]);

                    real[off] = real[i] - tr;
                    imag[off] = imag[i] - ti;

                    real[i] += tr;
                    imag[i] += ti;
                }
                double tempReal = currentPhaseShiftReal;
                currentPhaseShiftReal = (tempReal * phaseShiftStepReal) - (currentPhaseShiftImag * phaseShiftStepImag);
                currentPhaseShiftImag = (tempReal * phaseShiftStepImag) + (currentPhaseShiftImag * phaseShiftStepReal);
            }
        }
    }
}