import java.util.Random;

public class Network {
    public enum OptimizerType {
        VANILLA,
        MOMENTUM,
        ADAM
    }

    private double[][][] weights;
    private double[][] biases;
    private int[] layerSizes;
    private double learningRate;
    private OptimizerType optimizerType;

    private double[][][] momentumW;
    private double[][] momentumB;
    private double momentumCoeff = 0.9;

    private double[][][] adamM_W, adamV_W;
    private double[][] adamM_B, adamV_B;
    private double adamBeta1 = 0.9;
    private double adamBeta2 = 0.999;
    private double adamEpsilon = 1e-8;
    private int adamT = 0;

    private double[][] activations;

    public Network(int inputSize, int[] hiddenSizes, int outputSize, double learningRate, OptimizerType optimizer) {
        this.learningRate = learningRate;
        this.optimizerType = optimizer;

        layerSizes = new int[hiddenSizes.length + 2];
        layerSizes[0] = inputSize;
        for (int i = 0; i < hiddenSizes.length; i++) {
            layerSizes[i + 1] = hiddenSizes[i];
        }
        layerSizes[layerSizes.length - 1] = outputSize;

        // Unique seed per network instance
        Random rand = new Random(System.nanoTime() ^ Thread.currentThread().getId() ^ (long)(Math.random() * Long.MAX_VALUE));
        weights = new double[layerSizes.length - 1][][];
        biases = new double[layerSizes.length - 1][];

        for (int layer = 0; layer < weights.length; layer++) {
            int fromSize = layerSizes[layer];
            int toSize = layerSizes[layer + 1];
            double scale = Math.sqrt(2.0 / fromSize);
            weights[layer] = new double[fromSize][toSize];
            biases[layer] = new double[toSize];
            for (int i = 0; i < fromSize; i++) {
                for (int j = 0; j < toSize; j++) {
                    weights[layer][i][j] = rand.nextGaussian() * scale;
                }
            }
        }
        activations = new double[layerSizes.length][];
        initOptimizerState();
    }

    public Network(int inputSize, int[] hiddenSizes, int outputSize, double learningRate) {
        this(inputSize, hiddenSizes, outputSize, learningRate, OptimizerType.VANILLA);
    }

    private void initOptimizerState() {
        if (optimizerType == OptimizerType.MOMENTUM) {
            momentumW = new double[weights.length][][];
            momentumB = new double[biases.length][];
            for (int layer = 0; layer < weights.length; layer++) {
                momentumW[layer] = new double[weights[layer].length][weights[layer][0].length];
                momentumB[layer] = new double[biases[layer].length];
            }
        } else if (optimizerType == OptimizerType.ADAM) {
            adamM_W = new double[weights.length][][];
            adamV_W = new double[weights.length][][];
            adamM_B = new double[biases.length][];
            adamV_B = new double[biases.length][];
            for (int layer = 0; layer < weights.length; layer++) {
                adamM_W[layer] = new double[weights[layer].length][weights[layer][0].length];
                adamV_W[layer] = new double[weights[layer].length][weights[layer][0].length];
                adamM_B[layer] = new double[biases[layer].length];
                adamV_B[layer] = new double[biases[layer].length];
            }
        }
    }

    public double[] forward(double[] input) {
        activations[0] = input.clone();
        for (int layer = 0; layer < weights.length; layer++) {
            int fromSize = layerSizes[layer];
            int toSize = layerSizes[layer + 1];
            boolean isOutputLayer = (layer == weights.length - 1);
            activations[layer + 1] = new double[toSize];
            for (int j = 0; j < toSize; j++) {
                double sum = biases[layer][j];
                for (int i = 0; i < fromSize; i++) {
                    sum += activations[layer][i] * weights[layer][i][j];
                }
                activations[layer + 1][j] = isOutputLayer ? sum : Math.tanh(sum);
            }
        }
        return activations[activations.length - 1].clone();
    }

    public void learn(double[] target) { learn(target, 1.0); }

    public void learn(double[] target, double weight) {
        if (activations[0] == null) return;
        double effectiveLR = learningRate * weight;
        double[][] errors = new double[layerSizes.length][];
        int outputLayer = layerSizes.length - 1;
        errors[outputLayer] = new double[layerSizes[outputLayer]];
        for (int i = 0; i < layerSizes[outputLayer]; i++) {
            errors[outputLayer][i] = target[i] - activations[outputLayer][i];
        }
        for (int layer = outputLayer - 1; layer >= 1; layer--) {
            errors[layer] = new double[layerSizes[layer]];
            for (int i = 0; i < layerSizes[layer]; i++) {
                double sum = 0;
                for (int j = 0; j < layerSizes[layer + 1]; j++) {
                    sum += errors[layer + 1][j] * weights[layer][i][j];
                }
                double activation = activations[layer][i];
                errors[layer][i] = sum * (1 - activation * activation);
            }
        }
        switch (optimizerType) {
            case VANILLA -> applyVanilla(errors, effectiveLR);
            case MOMENTUM -> applyMomentum(errors, effectiveLR);
            case ADAM -> applyAdam(errors, effectiveLR);
        }
    }

    private void applyVanilla(double[][] errors, double lr) {
        for (int layer = 0; layer < weights.length; layer++) {
            for (int i = 0; i < layerSizes[layer]; i++) {
                for (int j = 0; j < layerSizes[layer + 1]; j++) {
                    weights[layer][i][j] += lr * errors[layer + 1][j] * activations[layer][i];
                }
            }
            for (int j = 0; j < layerSizes[layer + 1]; j++) {
                biases[layer][j] += lr * errors[layer + 1][j];
            }
        }
    }

    private void applyMomentum(double[][] errors, double lr) {
        for (int layer = 0; layer < weights.length; layer++) {
            for (int i = 0; i < layerSizes[layer]; i++) {
                for (int j = 0; j < layerSizes[layer + 1]; j++) {
                    double grad = errors[layer + 1][j] * activations[layer][i];
                    momentumW[layer][i][j] = momentumCoeff * momentumW[layer][i][j] + lr * grad;
                    weights[layer][i][j] += momentumW[layer][i][j];
                }
            }
            for (int j = 0; j < layerSizes[layer + 1]; j++) {
                double grad = errors[layer + 1][j];
                momentumB[layer][j] = momentumCoeff * momentumB[layer][j] + lr * grad;
                biases[layer][j] += momentumB[layer][j];
            }
        }
    }

    private void applyAdam(double[][] errors, double lr) {
        adamT++;
        double bc1 = 1 - Math.pow(adamBeta1, adamT);
        double bc2 = 1 - Math.pow(adamBeta2, adamT);
        for (int layer = 0; layer < weights.length; layer++) {
            for (int i = 0; i < layerSizes[layer]; i++) {
                for (int j = 0; j < layerSizes[layer + 1]; j++) {
                    double grad = errors[layer + 1][j] * activations[layer][i];
                    adamM_W[layer][i][j] = adamBeta1 * adamM_W[layer][i][j] + (1 - adamBeta1) * grad;
                    adamV_W[layer][i][j] = adamBeta2 * adamV_W[layer][i][j] + (1 - adamBeta2) * grad * grad;
                    double mHat = adamM_W[layer][i][j] / bc1;
                    double vHat = adamV_W[layer][i][j] / bc2;
                    weights[layer][i][j] += lr * mHat / (Math.sqrt(vHat) + adamEpsilon);
                }
            }
            for (int j = 0; j < layerSizes[layer + 1]; j++) {
                double grad = errors[layer + 1][j];
                adamM_B[layer][j] = adamBeta1 * adamM_B[layer][j] + (1 - adamBeta1) * grad;
                adamV_B[layer][j] = adamBeta2 * adamV_B[layer][j] + (1 - adamBeta2) * grad * grad;
                double mHat = adamM_B[layer][j] / bc1;
                double vHat = adamV_B[layer][j] / bc2;
                biases[layer][j] += lr * mHat / (Math.sqrt(vHat) + adamEpsilon);
            }
        }
    }

    public OptimizerType getOptimizerType() { return optimizerType; }
    public double getLearningRate() { return learningRate; }

    public double getWeightMagnitude() {
        double sum = 0;
        for (int layer = 0; layer < weights.length; layer++) {
            for (int i = 0; i < weights[layer].length; i++) {
                for (int j = 0; j < weights[layer][i].length; j++) {
                    sum += weights[layer][i][j] * weights[layer][i][j];
                }
            }
        }
        return Math.sqrt(sum);
    }


    public double[] getWeights() {
        int total = 0;
        for (int layer = 0; layer < weights.length; layer++) {
            total += layerSizes[layer] * layerSizes[layer + 1];
            total += layerSizes[layer + 1];
        }
        double[] result = new double[total];
        int idx = 0;
        for (int layer = 0; layer < weights.length; layer++) {
            for (int i = 0; i < weights[layer].length; i++) {
                for (int j = 0; j < weights[layer][i].length; j++) {
                    result[idx++] = weights[layer][i][j];
                }
            }
            for (int j = 0; j < biases[layer].length; j++) {
                result[idx++] = biases[layer][j];
            }
        }
        return result;
    }

    public void setWeights(double[] data) {
        int idx = 0;
        for (int layer = 0; layer < weights.length; layer++) {
            for (int i = 0; i < weights[layer].length; i++) {
                for (int j = 0; j < weights[layer][i].length; j++) {
                    weights[layer][i][j] = data[idx++];
                }
            }
            for (int j = 0; j < biases[layer].length; j++) {
                biases[layer][j] = data[idx++];
            }
        }
    }
}
