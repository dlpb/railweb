package services.visit.event

import java.time.LocalDateTime

import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import models.auth.User
import models.data.{Event, EventDataProvider, Visit}
import services.location.LocationService
import services.route.RouteService


@Singleton
class EventService @Inject() (config: Config,
                                      locationService: LocationService,
                                      routeService: RouteService,
                                      eventDataProvider: EventDataProvider) {
  def saveEventsAsJson(events: String, user: User) = eventDataProvider.saveEventsAsJson(events, user)

  def getEventsAsJson(user: User): String = eventDataProvider.getEventsAsJson(user)

  def getEventsForUser(user: User): List[Event] = eventDataProvider.getEvents(user)

  def saveEvent(event: Event, user: User): Unit = eventDataProvider.saveEvent(event, user)

  def saveEvents(events: List[Event], user: User): Unit = eventDataProvider.saveEvents(events, user)

  def removeLastEvent(events: List[Event], user: User): Unit = eventDataProvider.removeLastEvent(events, user)

  def deleteAllVisits(events: List[Event], user: User): Unit =  eventDataProvider.removeAllEvents(events , user)

  def getEventFromId(id: String, user: User): Option[Event] = getEventsForUser(user).find(_.id.equals(id))


  def ensureActiveEvent(user: User) = {
    val events = getEventsForUser(user)
    val now = LocalDateTime.now()
    val midnightBeforeNow = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
    val midnightAfterNow = midnightBeforeNow.plusHours(24)
    val justBeforeMidnightAfterNow = midnightAfterNow.minusNanos(1)

    val filteredEvents = events
      .filter(event =>
        (event.startedAt.isBefore(now) || event.startedAt.isEqual(now)) && (event.endedAt.isEqual(now) || event.endedAt.isAfter(now)))


    if(filteredEvents.isEmpty){
      val latestEndOfEventOpt = events.sortBy(_.endedAt).map(_.endedAt).reverse.headOption
      val latestEnd = latestEndOfEventOpt.map(end => {if(end.isBefore(midnightBeforeNow)) midnightBeforeNow else end}).getOrElse(midnightBeforeNow)
      val event = Event(
        name = s"${now.getYear}-${now.getMonthValue}-${now.getDayOfMonth}",
        startedAt = latestEnd,
        endedAt = justBeforeMidnightAfterNow)
      saveEvent(event, user)
    }
  }

  def ensureAllVisitsHaveAnEvent(visits: List[Visit[_]], user: User): Unit = {
    val events = getEventsForUser(user)
    val visitsWithoutEvents = visits
      .filterNot(visit => {

        val visitDate = visit.eventOccurredAt
        val visitDateMidnightBefore = visitDate.withHour(0).withMinute(0).withSecond(0).withNano(0)
        val visitDateMidnightAfter = visitDateMidnightBefore.plusHours(24)

        events.exists(event => {
          (event.startedAt.isAfter(visitDateMidnightBefore) || event.startedAt.isEqual(visitDateMidnightBefore)) && event.endedAt.isBefore(visitDateMidnightAfter)
        })
    })
    visitsWithoutEvents.foreach(visit => {
      val visitDate = visit.eventOccurredAt
      val visitDateMidnightBefore = visitDate.withHour(0).withMinute(0).withSecond(0).withNano(0)
      val visitDateMidnightAfter = visitDateMidnightBefore.plusDays(1).minusNanos(1)
      val justBeforeMidnightAfterNow = visitDateMidnightAfter.minusNanos(1)

      val event = Event(
        name = s"${visitDate.getYear}-${visitDate.getMonthValue}-${visitDate.getDayOfMonth}",
        startedAt = visitDateMidnightBefore,
        endedAt = justBeforeMidnightAfterNow)
      saveEvent(event, user)

    })
  }
}
