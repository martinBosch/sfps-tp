package db

import cats.effect.{ContextShift, IO}
import doobie.Transactor
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor.Aux

import db.Db.{createDB, insertMany}
import db.DolarInfoCSVReader.readDolarInfo


object Main extends App {
  val dolarInfoCSVReader = DolarInfoCSVReader("data/train.csv")
  val rows: List[DolarInfo] = readDolarInfo(dolarInfoCSVReader)

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
  createDB(db)
  println("Loadind data to db...")
  insertMany(db, rows)
  //    for( r <- rows) insert1(r).run.transact(xa).unsafeRunSync
  //    insertMany(rows).run.transact(xa).unsafeRunSync
}
