package randomForest

import cats.effect.{ContextShift, IO}
import db.Db.getAll
import db.{Db, DolarInfo}
import randomForest.DolarRandomForest.train

import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux

object Main extends App {
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
  val rows: List[DolarInfo] = getAll(db)
  // columns that need to added to feature column
  val featureCols = Array("open", "high", "low", "last", "aj_dif", "ol_vol", "ol_dif", "vol_ope",
    "dolar_bn", "dolar_itau", "dif_sem")
  val labelCol: String = "cierre"
  train(rows, featureCols, labelCol)
}
