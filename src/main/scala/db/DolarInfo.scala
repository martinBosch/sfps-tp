package db

case class DolarInfo(id: Int, fecha: String, open: Double, high: Double, low: Double, last: Double, cierre: Double,
                     aj_dif: Double, mon: String, ol_vol: Double, ol_dif: Double, vol_ope: Double, unidad: String,
                     dolar_bn: Double, dolar_itau: Double, dif_sem: Double)
