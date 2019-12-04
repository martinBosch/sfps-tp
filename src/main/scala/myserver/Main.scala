package myserver

import org.http4s.HttpRoutes
import org.http4s.syntax._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe._

import db.{Db, DolarInfo}
import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts
import cats._
import cats.effect._
import cats.data._
import cats.implicits._
import doobie.util.transactor.Transactor.Aux
import fs2.Stream

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.pmml4s.model.Model

import scala.concurrent.ExecutionContext.Implicits.global
import db.Db.{get1, insert1}


case class Prediction(cierre: String)

case class DolarInfoRequest(dolar_bn: Double, ol_dif: Double, aj_dif: Double, dolar_itau: Double, dif_sem: Double,
                            vol_ope: Double, last: Double, open: Double, high: Double, ol_vol: Double)

object MyMain extends IOApp {
  val model: Model = Model.fromFile("model.xml")

  // We need a ContextShift[IO] before we can construct a Transactor[IO]. The passed ExecutionContext
  // is where nonblocking operations will be executed. For testing here we're using a synchronous EC.
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)
  // A transactor that gets connections from java.sql.DriverManager and executes blocking operations
  // on an our synchronous EC. See the chapter on connection handling for more info.
  val xa: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",     // driver classname
    "jdbc:postgresql:sfps",      // connect URL (driver-specific)
    "postgres",                  // user
    "postgres",                  // password
    //Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
  )
  val db = Db(xa)

  val dolarPredictionService = HttpRoutes.of[IO] {
    case req @ GET -> Root / "predict" =>
      for {
        r <- req.as[DolarInfoRequest]
        resp <- get1(db, r) match {
          case Nil => {
            println("Predicting value...")
            val cierre_predict: Double = model.predict("dolar_bn" -> r.dolar_bn, "ol_dif" -> r.ol_dif,
              "aj_dif" -> r.aj_dif, "dolar_itau" -> r.dolar_itau, "dif_sem" -> r.dif_sem, "vol_ope" -> r.vol_ope,
              "last" -> r.last, "open" -> r.open, "high" -> r.high, "ol_vol" -> r.ol_vol).head._2.toString.toDouble

            val dolarInfo: DolarInfo = DolarInfo(open=r.open, high=r.high, last=r.last, cierre=cierre_predict,
              aj_dif=r.aj_dif, ol_vol=r.ol_vol, ol_dif=r.ol_dif, vol_ope=r.vol_ope, dolar_bn=r.dolar_bn,
              dolar_itau=r.dolar_itau, dif_sem=r.dif_sem, id=0, fecha="-1", low=0, unidad="TONS", mon="D")

            insert1(dolarInfo).run.transact(db.xa).unsafeRunSync

            Ok(Prediction(cierre_predict.toString).asJson)
          }
          case head :: tail => {
            println("Existing value...")
            Ok(Prediction(head.toString).asJson)
          }
        }
      } yield resp

    case req @ POST -> Root / "predict" =>
      for {
        r <- req.as[DolarInfoRequest]
        resp <- get1(db, r) match {
          case Nil => {
            println("Predicting value...")
            val cierre_predict: Double = model.predict("dolar_bn" -> r.dolar_bn, "ol_dif" -> r.ol_dif,
              "aj_dif" -> r.aj_dif, "dolar_itau" -> r.dolar_itau, "dif_sem" -> r.dif_sem, "vol_ope" -> r.vol_ope,
              "last" -> r.last, "open" -> r.open, "high" -> r.high, "ol_vol" -> r.ol_vol).head._2.toString.toDouble

            val dolarInfo: DolarInfo = DolarInfo(open=r.open, high=r.high, last=r.last, cierre=cierre_predict,
              aj_dif=r.aj_dif, ol_vol=r.ol_vol, ol_dif=r.ol_dif, vol_ope=r.vol_ope, dolar_bn=r.dolar_bn,
              dolar_itau=r.dolar_itau, dif_sem=r.dif_sem, id=0, fecha="-1", low=0, unidad="TONS", mon="D")

            insert1(dolarInfo).run.transact(db.xa).unsafeRunSync

            Ok(Prediction(cierre_predict.toString).asJson)
          }
          case head :: tail => {
            println("Existing value...")
            Ok(Prediction(head.toString).asJson)
          }
        }
      } yield resp
  }.orNotFound

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(dolarPredictionService)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}

// curl -X GET -H "Content-type: application/json" -H "Accept: application/json" -d '{"dolar_bn":"2.92", "ol_dif":"0", "aj_dif":"13", "dolar_itau":"2.905", "dif_sem":"-221", "vol_ope":"0", "last":"0", "open":"1", "high":"2", "ol_vol":"0"}' http://localhost:8080/predict

// curl -X GET -H "Content-type: application/json" -H "Accept: application/json" -d '{"dolar_bn":"2.92", "ol_dif":"0", "aj_dif":"13", "dolar_itau":"2.905", "dif_sem":"-221", "vol_ope":"0", "last":"0", "open":"0", "high":"0", "ol_vol":"0"}' http://localhost:8080/predict

// curl -X GET -H "Content-type: application/json" -H "Accept: application/json" -d '{"dolar_bn":"15.2", "ol_dif":"-3700", "aj_dif":"3.5", "dolar_itau":"15.24", "dif_sem":"-229.5", "vol_ope":"0", "last":"0", "open":"0", "high":"0", "ol_vol":"79800"}' http://localhost:8080/predict
