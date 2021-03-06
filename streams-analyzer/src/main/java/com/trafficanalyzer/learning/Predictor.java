package com.trafficanalyzer.learning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session.Runner;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;

import com.trafficanalyzer.streams.entity.PayloadCount;

public class Predictor {

    private static Logger logger = LoggerFactory.getLogger(Predictor.class);

    private static float LEARNING_LOSS = 0.001546f;
    private static float PREDICTION_TRESHOLD = 1.5f;

    private String modelDir;

    private SavedModelBundle modelBundle;

    private boolean determine(float loss) {
        return loss <= LEARNING_LOSS * PREDICTION_TRESHOLD;
    }

    public Predictor(String modelDir) {
        this.modelDir = modelDir;
    }

    public void init() {
        modelBundle = SavedModelBundle.load(modelDir, "serve");
    }

    public boolean predict(PayloadCount count, float max) {
        float[][] vector = { { (float) count.getMtCount() / count.getTotalCount(),
                (float) count.getMoCount() / count.getTotalCount(),
                (float) count.getErrorCount() / count.getTotalCount(), max } };
        final float result = predict(vector);
        logger.debug("device[{}]: MT[{}], MO[{}], max[{}], predict result[{}]", count.getDeviceId(), count.getMtCount(),
                count.getMoCount(), max, result);
        return determine(result);
    }

    public boolean predict(float mtRate, float moRate, float errorRate, float max) {
        float[][] vector = { { mtRate, moRate, errorRate, max } };
        final float result = predict(vector);
        logger.debug("MT[{}], MO[{}], max[{}], predict result[{}]", mtRate, moRate, max, result);
        return determine(result);
    }

    private float predict(float[][] vector) {
        final Runner runner = modelBundle.session().runner();
        final Tensor<Float> input = Tensors.create(vector);
        final Tensor<?> result = runner.feed("Input/Placeholder", input).fetch("accuracy/Mean").run().get(0);
        return result.floatValue();
    }

    public static void main(final String[] args) throws Exception {

        if (args.length != 1) {
            System.out.println("usage: Predictor [model]");
            return;
        }

        final String modelDir = args[0];

        final Predictor model = new Predictor(modelDir);
        model.init();

        // Prepare input
        float[][] vector1 = { { 0.50049800796812749f, 0.49950199203187251f, 0.0f, 0.13157894736842105f } };
        float[][] vector2 = { { 0.50346534653465347f, 0.49653465346534653f, 0.0f, 0.22807017543859651f } };
        float[][] vector3 = { { 0.51522474625422909f, 0.48477525374577091f, 0.0f, 0.65789473684210531f } };

        final float result1 = model.predict(vector1);
        logger.info("vector1 result[{}] is normal? {}", result1, model.determine(result1));
        final float result2 = model.predict(vector2);
        logger.info("vector2 result[{}] is normal? {}", result2, model.determine(result2));
        final float result3 = model.predict(vector3);
        logger.info("vector3 result[{}] is normal? {}", result3, model.determine(result3));
    }

    public void close() {
        this.modelBundle.close();
    }
}