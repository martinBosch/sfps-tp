package myclient

import org.http4s.circe._
import org.http4s.client.dsl.io._
import org.http4s.client.blaze._
import cats.effect.{ContextShift, IO, Timer}
import db.{DolarInfo, DolarInfoCSVReader}
import db.DolarInfoCSVReader.readDolarInfo
import io.circe.generic.auto._
import fs2.Stream
import myserver.DolarInfoRequest
import org.http4s.Uri
import io.circe.syntax._
import org.http4s.dsl.io._

import scala.concurrent.ExecutionContext.Implicits.global


case class Prediction(cierre: String)


object Main extends App {
  import scala.concurrent.ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  def dolarPredictionClient(dolarInfo: DolarInfo): Stream[IO, Prediction] = {
    // Encode a DolarInfoRequest request
    val req = POST(
      DolarInfoRequest(dolarInfo.dolar_bn, dolarInfo.ol_dif, dolarInfo.aj_dif, dolarInfo.dolar_itau,
        dolarInfo.dif_sem, dolarInfo.vol_ope, dolarInfo.last, dolarInfo.open, dolarInfo.high, dolarInfo.ol_vol).asJson,
      Uri.uri("http://localhost:8080/predict"))
    // Create a client
    BlazeClientBuilder[IO](global).stream.flatMap { httpClient =>
      // Decode a Prediction response
      Stream.eval(httpClient.expect(req)(jsonOf[IO, Prediction]))
    }
  }

  def predictCierre(dolarInfo: DolarInfo): Double = {
    val dolarPrediciton = dolarPredictionClient(dolarInfo)

    val res = dolarPrediciton.compile.last.unsafeRunSync
    res match {
      case Some(prediction) => prediction.cierre.toDouble
      case None => 0.0
    }
  }

  val dolarInfoCSVReader = DolarInfoCSVReader("data/test.csv")
  val rows: List[DolarInfo] = readDolarInfo(dolarInfoCSVReader)

  println(rows.head.id)
  println(s"Cierre: ${rows.head.cierre} - Predict Cierre: ${predictCierre(rows.head)}")

  //  rows.map(row => {
//    println(s"Cierre: ${row.cierre} - Predict Cierre: ${predictCierre(row)}")
//  })
}