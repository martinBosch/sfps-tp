package db

import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import doobie.util.transactor.Transactor.Aux
import fs2.Stream
import myserver.DolarInfoRequest

//import java.sql.Timestamp
//import java.time.LocalDateTime
//import java.time.format.DateTimeFormatter


case class Db(xa: Aux[IO, Unit])

object Db {
  def createDB(db: Db): Unit = {
    val drop = sql"""
      DROP TABLE IF EXISTS train
    """.update.run

    val create = sql"""
      CREATE TABLE train (
        id   SERIAL,
        fecha VARCHAR,
        open FLOAT,
        high FLOAT,
        low FLOAT,
        last FLOAT,
        cierre FLOAT,
        aj_dif FLOAT,
        mon CHAR,
        ol_vol INT,
        ol_dif INT,
        vol_ope INT,
        unidad CHAR(4),
        dolar_bn FLOAT,
        dolar_itau FLOAT,
        dif_sem FLOAT
      )
    """.update.run

    (drop, create).mapN(_ + _).transact(db.xa).unsafeRunSync
  }

  def insert1(dolar: DolarInfo): Update0 = {
    val id: Int = dolar.id
    val fecha: String = dolar.fecha
    val open: Double = dolar.open
    val high: Double = dolar.high
    val low: Double = dolar.low
    val last: Double = dolar.last
    val cierre: Double = dolar.cierre
    val aj_dif: Double = dolar.aj_dif
    val mon: String = dolar.mon
    val ol_vol: Double = dolar.ol_vol
    val ol_dif: Double = dolar.ol_dif
    val vol_ope: Double = dolar.vol_ope
    val unidad: String = dolar.unidad
    val dolar_bn: Double = dolar.dolar_bn
    val dolar_itau: Double = dolar.dolar_itau
    val dif_sem: Double = dolar.dif_sem
    println(s"inserting row $id...")

    sql"insert into train (id, fecha, open, high, low, last, cierre, aj_dif, mon, ol_vol, ol_dif, vol_ope, unidad, dolar_bn, dolar_itau, dif_sem) values ($id, $fecha, $open, $high, $low, $last, $cierre, $aj_dif, $mon, $ol_vol, $ol_dif, $vol_ope, $unidad, $dolar_bn, $dolar_itau, $dif_sem)".update
  }

  @scala.annotation.tailrec
  def insertMany(db: Db, ds: List[DolarInfo]): Unit = {
    ds match {
      case Nil =>
      case dolarInfo :: tail =>
        insert1(dolarInfo).run.transact(db.xa).unsafeRunSync
        insertMany(db, tail)
    }
  }

  def getAll(db: Db): List[DolarInfo] = {
    sql"select id, fecha, open, high, low, last, cierre, aj_dif, mon, ol_vol, ol_dif, vol_ope, unidad, dolar_bn, dolar_itau, dif_sem from train".query[DolarInfo].to[List].transact(db.xa).unsafeRunSync
  }

  def get1(db: Db, dolarInfoRequest: DolarInfoRequest): List[Double] = {
    sql"""
      select cierre
      from train
      where dolar_bn = ${dolarInfoRequest.dolar_bn}
       and ol_dif = ${dolarInfoRequest.ol_dif}
       and aj_dif = ${dolarInfoRequest.aj_dif}
       and dolar_itau = ${dolarInfoRequest.dolar_itau}
       and dif_sem = ${dolarInfoRequest.dif_sem}
       and vol_ope = ${dolarInfoRequest.vol_ope}
       and last = ${dolarInfoRequest.last}
       and open = ${dolarInfoRequest.open}
       and high = ${dolarInfoRequest.high}
       and ol_vol = ${dolarInfoRequest.ol_vol}
    """.query[Double].to[List].transact(db.xa).unsafeRunSync
  }
}
