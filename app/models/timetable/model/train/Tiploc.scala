package models.timetable.model.train

case class Tiploc(tiploc: String, nlc: Int, nlcCheckChar: Char, tpsDesc: String, stanox: Int, crs: String, desc: String)

case class TiplocAmend(tiploc: String, nlc: Int, nlcCheckChar: Char, tpsDesc: String, stanox: Int, crs: String, desc: String, newTiploc: String)

