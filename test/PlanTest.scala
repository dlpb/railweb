import java.io.InputStream

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream
import models.plan.{PlanService, Reader}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class PlanTest extends FlatSpec with Matchers {

  "Plan Service" should "create a padded url for reading location timetables" in {
    val url = PlanService.createUrlForLocationTimetables("CTH", 2019, 1, 1, 30, 900)
    url should be("http://railweb-timetables.herokuapp.com/timetables/location/CTH?year=2019&month=01&day=01&from=0030&to=0900")
  }

  it should "create an unpadded url for reading location timetables" in {
    val url = PlanService.createUrlForLocationTimetables("CTH", 2019, 10, 11, 2030, 2200)
    url should be("http://railweb-timetables.herokuapp.com/timetables/location/CTH?year=2019&month=10&day=11&from=2030&to=2200")
  }

  it should "get timetable for around now" in {
    val from = PlanService.from
    val to = PlanService.to

    val fromTime = from.getHour*100 + from.getMinute
    val toTime = to.getHour*100 + to.getMinute

    val expectedUrl = PlanService.createUrlForLocationTimetables("CTH", from.getYear, from.getMonthValue, from.getDayOfMonth, fromTime, toTime)

    val service = new PlanService(new Reader {
      override def getInputStream(url: String): InputStream = {
        url should be(expectedUrl)
        new ByteInputStream()
      }
    })

    service.getTrainsForLocationAroundNow("CTH")
  }

}
