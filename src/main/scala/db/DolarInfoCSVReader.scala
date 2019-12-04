package db

import scala.io.Source

case class DolarInfoCSVReader(filename: String)

object DolarInfoCSVReader {
  def readDolarInfo(dolarInfoCSVReader: DolarInfoCSVReader): List[DolarInfo] = {
    Source.fromFile(dolarInfoCSVReader.filename).getLines().drop(1).map(toDolarInfo).toList
  }

  def toDolarInfo(line: String): DolarInfo = {
    val dolarInfo = line.split(",").map(_.trim)

    val id: Int = dolarInfo(0).toInt
    println(s"to dolar info $id...")
    //val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a")
    //val fecha: LocalDateTime = LocalDateTime.parse(dolarInfo(1), formatter)
    val fecha: String = dolarInfo(1)
    val open: Float = dolarInfo(2).toFloat
    val high: Float = dolarInfo(3).toFloat
    val low: Float = dolarInfo(4).toFloat
    val last: Float = dolarInfo(5).toFloat
    val cierre: Float = dolarInfo(6).toFloat
    val aj_dif: Float = dolarInfo(7).toFloat
    val mon: String = dolarInfo(8)
    val ol_vol: Int = dolarInfo(9).toInt
    val ol_dif: Int = dolarInfo(10).toInt
    val vol_ope: Int = dolarInfo(11).toInt
    val unidad: String = dolarInfo(12)
    val dolar_bn: Float = dolarInfo(13).toFloat
    val dolar_itau: Float = if (dolarInfo(14) == "NA") 0 else dolarInfo(14).toFloat
    val dif_sem: Float = dolarInfo(15).toFloat

    DolarInfo(id, fecha, open, high, low, last, cierre, aj_dif, mon, ol_vol, ol_dif, vol_ope,
      unidad, dolar_bn, dolar_itau, dif_sem)
  }
}
