package test

import cats.effect.{ContextShift, IO}
import db.Db
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import org.pmml4s.model.{MiningModel, Model}
import myserver.MyMain.db
import _root_.db.Db.get1
import myserver.DolarInfoRequest

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
  val r = DolarInfoRequest(2.92000007629395, 0, 13, 2.90499997138977, -221, 0, 0, 0, 0, 0)
  val d = get1(db, r)
  println(d)
  d match {
    case Nil => println("empty")
    case head::tail => println("full")
  }
}
