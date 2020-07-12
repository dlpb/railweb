package models.srs

case class Srs (id: String, name: String, route: String, region: String, colour: SrsColour = SrsColour("#FFFFFF", "#000000", 1.0))
case class SrsColour(foregroundColour: String, backgroundColour: String, opacity: Double)

case class PresentationSrsData(colour: String, name: String, route: String, region: String, textColour: String)
object PresentationSrsData {
  type PresentationSrsMap = Map[String, PresentationSrsData]

  def toPresentationSrsMap(srs: List[Srs]): PresentationSrsMap = {
    srs.map({
      s =>
        s.id -> PresentationSrsData(s.colour.backgroundColour, s.name, s.route, s.region, s.colour.foregroundColour)
    }).toMap
  }
}