package models.toc

case class Operator(id: String, name: String, colour: TocColour = TocColour("#FFFFFF", "#000000"))
case class TocColour(foregroundColour: String, backgroundColour: String)

case class PresentationTocData(colour: String, name: String, textColour: String)

object PresentationTocData {
  type PresentationTocMap = Map[String, PresentationTocData]

  def apply(srs: List[Operator]): PresentationTocMap = {
    srs.map({
      s =>
        s.id -> PresentationTocData(s.colour.backgroundColour, s.name, s.colour.foregroundColour)
    }).toMap
  }
}