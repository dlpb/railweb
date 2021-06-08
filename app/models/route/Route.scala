package models.route

import java.time.Duration

case class Route(
                from: RoutePoint,
                to: RoutePoint,
                toc: String,
                singleTrack: String,
                electrification: String,
                speed: String,
                srsCode: String,
                `type`: String,
                distance: Long = 0,
                ){

  val travelTimeInSeconds: Duration = {
    averageTravelTime

  }

   private def averageTravelTime: java.time.Duration = {
    val absoluteSpeedPattern = "(\\d+)-(\\d+)".r
    val linkTimePattern = "(\\d+) MINUTES".r

    val timeInSeconds: Double = if(absoluteSpeedPattern.matches(speed)){
      //take the range and find the mid point
      //then convert that mph rating into m/s
      //then calculate the time in seconds
      //assume distance in meters
      val rangeOfSpeeds = absoluteSpeedPattern.findAllMatchIn(speed).toList
      //if the range is malformed, fall back to distance / time calculation
      val matchedGroups = absoluteSpeedPattern
        .findAllMatchIn(speed)
        .toList
        .head
        .groupCount

      if(matchedGroups != 2) distance / 1.38888
      else {
        val lowerBound = absoluteSpeedPattern
          .findAllMatchIn(speed)
          .toList
          .head
          .group(1)
          .toIntOption

        val upperBound = absoluteSpeedPattern
          .findAllMatchIn(speed)
          .toList
          .head
          .group(2)
          .toIntOption

        val midPoint: Option[Int] = lowerBound.flatMap(l => upperBound.map(u => {
               l + ((u - l) / 2)
        }))

        //assume 1 mile = 1609.34 meters
        //convert miles per hour into meters per hour
        //1 hour is 3600s so divide
        val speedInMetersPerSecond: Option[Double] = midPoint
          .map(_ * 1609.34)
          .map(_ / 3600)

        // get the speed or else fall back to the walking speed
        val calculatedTime = speedInMetersPerSecond.map(distance / _)
          .getOrElse(distance / 1.38888)

        calculatedTime
      }

    }
    else if(linkTimePattern.matches(speed)) {
      //take the time in minutes, and convert it to seconds
      val calculatedSpeed: Double = linkTimePattern
        .findFirstIn(speed)
        .flatMap(_.toIntOption)
        .map(_ * 60)
        .getOrElse(0)
        .toDouble
      calculatedSpeed
    } else if(distance > 0) {
      //assume 5km/h speed
      //1 hour = 3600 seconds
      // in meters per second, that's 1.3888 m/s
      //distance is in meters

      distance / 1.38888

    } else 0

    Duration.ofSeconds(timeInSeconds.toLong)
  }
}

case class RoutePoint(
                     lat: Double,
                     lon: Double,
                     id: String,
                     name: String,
                     `type`: String
                     )











