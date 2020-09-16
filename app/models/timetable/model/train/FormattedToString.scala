package models.timetable.model.train

trait FormattedToString {
  override def toString: String = {
    getClass
      .getSimpleName
      .replaceAll("\\$", "")
      .replaceAll("([^_])([A-Z])", "$1 $2")
      .trim
  }

}
