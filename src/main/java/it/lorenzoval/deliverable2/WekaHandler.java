package it.lorenzoval.deliverable2;

import org.apache.commons.io.FileUtils;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WekaHandler {

    private static final Logger logger = Logger.getLogger(WekaHandler.class.getName());
    private static final String FORMAT = "%n%-15.15s%-15.15s%-15.15s%n";
    private WekaHandler() {
    }

    private static CostMatrix createCostMatrix() {
        CostMatrix costMatrix = new CostMatrix(2);
        costMatrix.setCell(0, 0, 0.0);
        costMatrix.setCell(1, 0, 1.0);
        costMatrix.setCell(0, 1, 10.0);
        costMatrix.setCell(1, 1, 0.0);
        return costMatrix;
    }

    private static void evaluateResults(WekaResult wekaResult, Instances trainingSet, Instances testingSet,
                                        Classifier classifier, List<String> lines) throws Exception {
        final int classIndex = 0;

        classifier.buildClassifier(trainingSet);

        Evaluation evaluation = new Evaluation(testingSet);
        evaluation.evaluateModel(classifier, testingSet);

        wekaResult.setTP((int) evaluation.numTruePositives(classIndex));
        wekaResult.setFP((int) evaluation.numFalsePositives(classIndex));
        wekaResult.setTN((int) evaluation.numTrueNegatives(classIndex));
        wekaResult.setFN((int) evaluation.numFalseNegatives(classIndex));
        wekaResult.setPrecision(evaluation.precision(classIndex));
        wekaResult.setRecall(evaluation.recall(classIndex));
        wekaResult.setAuc(evaluation.areaUnderROC(classIndex));
        wekaResult.setKappa(evaluation.kappa());

        lines.add(wekaResult.toCSVLine());
    }

    private static void compareCostSensitivity(WekaResult wekaResult, Instances trainingSet, Instances testingSet,
                                               Classifier classifier, List<String> lines) throws Exception {
        wekaResult.setSensitivity("No cost sensitive");
        evaluateResults(wekaResult, trainingSet, testingSet, classifier, lines);

        CostMatrix costMatrix = createCostMatrix();

        wekaResult.setSensitivity("Sensitive threshold");
        CostSensitiveClassifier costSensitiveClassifier = new CostSensitiveClassifier();
        costSensitiveClassifier.setClassifier(classifier);
        costSensitiveClassifier.setCostMatrix(costMatrix);
        costSensitiveClassifier.setMinimizeExpectedCost(true);
        evaluateResults(wekaResult, trainingSet, testingSet, costSensitiveClassifier, lines);

        wekaResult.setSensitivity("Sensitive learning");
        costSensitiveClassifier = new CostSensitiveClassifier();
        costSensitiveClassifier.setClassifier(classifier);
        costSensitiveClassifier.setCostMatrix(costMatrix);
        costSensitiveClassifier.setMinimizeExpectedCost(false);
        evaluateResults(wekaResult, trainingSet, testingSet, costSensitiveClassifier, lines);
    }

    private static void compareClassifiers(WekaResult wekaResult, Instances trainingSet, Instances testingSet,
                                           List<String> lines) throws Exception {
        List<Classifier> classifiers = new ArrayList<>();
        classifiers.add(new NaiveBayes());
        classifiers.add(new RandomForest());
        classifiers.add(new IBk());

        for (Classifier classifier : classifiers) {
            String name = classifier.getClass().getName();
            wekaResult.setClassifier(name.substring(name.lastIndexOf('.') + 1));
            compareCostSensitivity(wekaResult, trainingSet, testingSet, classifier, lines);
        }
    }

    private static void compareClassifiersOversampling(WekaResult wekaResult, Instances trainingSet,
                                                       Instances testingSet, double percentage, List<String> lines) throws Exception {
        Resample resample = new Resample();
        resample.setInputFormat(trainingSet);
        resample.setBiasToUniformClass(1.0);
        resample.setNoReplacement(false);
        resample.setSampleSizePercent(percentage);
        compareClassifiers(wekaResult, trainingSet, testingSet, lines);
    }

    private static void compareClassifiersUndersampling(WekaResult wekaResult, Instances trainingSet,
                                                        Instances testingSet, List<String> lines) throws Exception {
        SpreadSubsample spreadSubsample = new SpreadSubsample();
        spreadSubsample.setDistributionSpread(1.0);
        spreadSubsample.setInputFormat(trainingSet);
        Filter.useFilter(trainingSet, spreadSubsample);
        compareClassifiers(wekaResult, trainingSet, testingSet, lines);
    }

    private static void compareClassifiersSmote(WekaResult wekaResult, Instances trainingSet, Instances testingSet,
                                                double percentage, List<String> lines) throws Exception {
        SMOTE smote = new SMOTE();
        smote.setInputFormat(trainingSet);
        smote.setPercentage(percentage);
        Filter.useFilter(trainingSet, smote);
        compareClassifiers(wekaResult, trainingSet, testingSet, lines);
    }

    private static int countBuggyInstances(Instances trainingSet) {
        int count = 0;
        for (Instance instance : trainingSet) {
            count += (int) instance.value(instance.numAttributes() - 1) ^ 1;
        }
        return count;
    }

    private static double calculatePercentage(Instances trainingSet) {
        int buggy = countBuggyInstances(trainingSet);
        int nonBuggy = trainingSet.size() - buggy;

        if (nonBuggy > buggy)
            return buggy != 0 ? 100.0 * (nonBuggy - buggy) / buggy : 0;
        else
            return nonBuggy != 0 ? 100.0 * (buggy - nonBuggy) / nonBuggy : 0;
    }

    private static void compareBalancing(WekaResult wekaResult, Instances trainingSet, Instances testingSet,
                                         List<String> lines) throws Exception {
        double percentage = calculatePercentage(trainingSet);

        wekaResult.setBalancing("No sampling");
        compareClassifiers(wekaResult, trainingSet, testingSet, lines);

        wekaResult.setBalancing("Oversampling");
        compareClassifiersOversampling(wekaResult, trainingSet, testingSet, percentage, lines);

        wekaResult.setBalancing("Undersampling");
        compareClassifiersUndersampling(wekaResult, trainingSet, testingSet, lines);

        wekaResult.setBalancing("SMOTE");
        compareClassifiersSmote(wekaResult, trainingSet, testingSet, percentage, lines);
    }

    private static void compareTechniques(WekaResult wekaResult, Instances trainingSet, Instances testingSet,
                                          List<String> lines) throws Exception {
        // No feature selection
        wekaResult.setFeatureSelection("No selection");
        compareBalancing(wekaResult, trainingSet, testingSet, lines);

        // BestFirst
        wekaResult.setFeatureSelection("BestFirst");
        AttributeSelection attributeSelection = new AttributeSelection();
        CfsSubsetEval cfsSubsetEval = new CfsSubsetEval();
        BestFirst bestFirst = new BestFirst();
        attributeSelection.setEvaluator(cfsSubsetEval);
        attributeSelection.setSearch(bestFirst);
        attributeSelection.setInputFormat(trainingSet);
        Instances training = Filter.useFilter(trainingSet, attributeSelection);
        Instances testing = Filter.useFilter(testingSet, attributeSelection);

        compareBalancing(wekaResult, training, testing, lines);
    }

    private static List<String> walkForward(WekaResult wekaResult, Instances dataset) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add(WekaResult.CSV_HEADER);

        // Get number of releases by looking at last instance of dataset
        int numReleases = (int) dataset.lastInstance().value(0);
        logger.log(Level.INFO, "Number of releases in the dataset: {0}", numReleases);
        // Skip first iteration because it has empty training set
        for (int i = 2; i <= numReleases; i++) {
            Instances trainingSet = new Instances(dataset, 0);
            Instances testingSet = new Instances(dataset, 0);

            for (Instance instance : dataset) {
                if (instance.value(0) < i)
                    trainingSet.add(instance);
                else if (instance.value(0) == i)
                    testingSet.add(instance);
                else
                    break;
            }

            String message = String.format(FORMAT, "", "Training set:", "Testing set:")
                    + String.format(FORMAT, "Releases:", i == 2 ? (i - 1) : "[1, " + (i - 1) + "]", i)
                    + String.format(FORMAT, "Size:", trainingSet.size(), testingSet.size());
            logger.log(Level.INFO, message);

            int trainingSetSize = trainingSet.size();
            int testingSetSize = testingSet.size();
            wekaResult.setNumTrainingReleases(trainingSetSize);
            wekaResult.setPercentTrainingReleases(100 * trainingSetSize / (trainingSetSize + testingSetSize));
            int buggy = countBuggyInstances(trainingSet);
            int buggyPercent = buggy != 0 ? 100 * buggy / trainingSetSize : 0;
            wekaResult.setPercentDefectiveInTraining(buggyPercent);
            buggy = countBuggyInstances(testingSet);
            buggyPercent = buggy != 0 ? 100 * buggy / testingSetSize : 0;
            wekaResult.setPercentDefectiveInTesting(buggyPercent);
            compareTechniques(wekaResult, trainingSet, testingSet, lines);
        }

        return lines;
    }

    private static Instances loadCSV(Project project) throws IOException {
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File(project.getProjectName() + ".csv"));
        // Set Yes as positive for Buggy
        loader.setNominalLabelSpecs(new Object[]{"Buggy:Yes,No"});
        return loader.getDataSet();
    }

    public static void evaluateDataset(Project project) throws Exception {
        File outFile = new File(project.getProjectName() + "_out.csv");
        Instances dataset = loadCSV(project);
        dataset.setClassIndex(dataset.numAttributes() - 1);
        WekaResult wekaResult = new WekaResult();
        wekaResult.setDataset(project.getProjectName());
        FileUtils.writeLines(outFile, walkForward(wekaResult, dataset));
    }

}
