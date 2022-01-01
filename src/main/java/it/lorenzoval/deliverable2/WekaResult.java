package it.lorenzoval.deliverable2;

import java.math.BigDecimal;

public class WekaResult {

    public static final String CSV_HEADER = "dataset,#TrainingRelease,%training,%Defective in training," +
            "%Defective in testing,classifier,balancing,Feature Selection,Sensitivity,TP,FP,TN,FN,Precision,Recall," +
            "AUC,Kappa";

    private String dataset;
    private int numTrainingReleases;
    private int percentTrainingReleases;
    private int percentDefectiveInTraining;
    private int percentDefectiveInTesting;
    private String classifier;
    private String balancing;
    private String featureSelection;
    private String sensitivity;
    private int tP;
    private int fP;
    private int tN;
    private int fN;
    private double precision;
    private double recall;
    private double auc;
    private double kappa;

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public void setNumTrainingReleases(int numTrainingReleases) {
        this.numTrainingReleases = numTrainingReleases;
    }

    public void setPercentTrainingReleases(int percentTrainingReleases) {
        this.percentTrainingReleases = percentTrainingReleases;
    }

    public void setPercentDefectiveInTraining(int percentDefectiveInTraining) {
        this.percentDefectiveInTraining = percentDefectiveInTraining;
    }

    public void setPercentDefectiveInTesting(int percentDefectiveInTesting) {
        this.percentDefectiveInTesting = percentDefectiveInTesting;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public void setBalancing(String balancing) {
        this.balancing = balancing;
    }

    public void setFeatureSelection(String featureSelection) {
        this.featureSelection = featureSelection;
    }

    public void setSensitivity(String sensitivity) {
        this.sensitivity = sensitivity;
    }

    public void setTP(int tP) {
        this.tP = tP;
    }

    public void setFP(int fP) {
        this.fP = fP;
    }

    public void setTN(int tN) {
        this.tN = tN;
    }

    public void setFN(int fN) {
        this.fN = fN;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    public void setAuc(double auc) {
        this.auc = auc;
    }

    public void setKappa(double kappa) {
        this.kappa = kappa;
    }

    public String toCSVLine() {
        return this.dataset + "," + this.numTrainingReleases + "," + this.percentTrainingReleases + "%," +
                this.percentDefectiveInTraining + "%," + this.percentDefectiveInTesting + "%," + this.classifier + "," +
                this.balancing + "," + this.featureSelection + "," + this.sensitivity + "," + this.tP + "," +
                this.fP + "," + this.tN + "," + this.fN + "," +
                (Double.isNaN(this.precision) ? this.precision : BigDecimal.valueOf(this.precision).toPlainString()) +
                "," + BigDecimal.valueOf(this.recall).toPlainString() + "," +
                BigDecimal.valueOf(this.auc).toPlainString() + "," + BigDecimal.valueOf(this.kappa).toPlainString();
    }

}
