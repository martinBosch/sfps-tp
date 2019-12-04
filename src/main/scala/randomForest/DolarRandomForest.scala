package randomForest

import java.io.File

import db.DolarInfo
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.{RandomForestClassificationModel, RandomForestClassifier}
import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.ml.regression.RandomForestRegressor
import org.apache.spark.ml.util.MLWritable
import org.apache.spark.sql.SparkSession
import org.jpmml.sparkml.PMMLBuilder
import org.spark_project.dmg.pmml.PMML


object DolarRandomForest {
  def train(data: List[DolarInfo], featureCols: Array[String], labelCol: String): Unit = {
    // context for spark
    val spark = SparkSession.builder
      .master("local[*]")
      .appName("lambda")
      .getOrCreate()

    // SparkSession has implicits
    import spark.implicits._

    val rowsDF = data.toDF()

    // VectorAssembler to add feature column
    // input columns - cols
    // feature column - features
    val assembler = new VectorAssembler().setInputCols(featureCols).setOutputCol("features")
    val featureDf = assembler.transform(rowsDF)

    // StringIndexer define new 'label' column with 'result' column
    val indexer = new StringIndexer().setInputCol(labelCol).setOutputCol("label")
    val labelDf = indexer.fit(featureDf).transform(featureDf)

    // split data set training and test
    // training data set - 70%
    // test data set - 30%
    val seed = 5043
    val Array(trainingData, testData) = labelDf.randomSplit(Array(0.7, 0.3), seed)

    // train Random Forest model with training data set
    val randomForestClassifier = new RandomForestRegressor().setMaxDepth(10).setMaxBins(100)

    val pipeline = new Pipeline().setStages(Array(assembler, indexer, randomForestClassifier))
    val randomForestModel = pipeline.fit(rowsDF)

    val file = new PMMLBuilder(trainingData.schema, randomForestModel).buildFile(new File("model.xml"));
  }
}
