package models.list

import javax.inject.Inject
import models.location.{Location, LocationsService}
import models.route.{Route, RoutesService}

import scala.collection.mutable


class ListService @Inject()(
                             routesService: RoutesService,
                             locationsService: LocationsService
                           ) {



  def list(beginning: Location,
           end: Location,
           followFixedLinks: Boolean = false,
           followFreightLinks: Boolean = true
          ): List[Location] = {

    val frontier: collection.mutable.PriorityQueue[OrderedLocationWrapper] = new mutable.PriorityQueue[OrderedLocationWrapper]()
    frontier += OrderedLocationWrapper(0,beginning)

    var path: Map[Location, Location] = Map.empty
    var pathCost: Map[Location, Int] = Map.empty

    path = path + (beginning -> beginning)
    pathCost = pathCost + (beginning -> 0)

    while(frontier.nonEmpty) {
      val current = frontier.dequeue().loc
      if(current.id.equals(end.id)){
        return findPath(path, beginning, end)
      }

      val neighbours = graphNeighbours(current, followFixedLinks, followFreightLinks)
      neighbours foreach {
        next =>
          val newCost: Int = pathCost(current) + getCostBetween(current, next)
          if(!pathCost.keySet.contains(next) || newCost < pathCost(next)) {
            pathCost = pathCost + (next -> newCost)
            val priority: Int = newCost + heuristic(end, next)
            frontier.enqueue(OrderedLocationWrapper(priority, next))
            path = path + (next -> current)
          }
      }
    }
    findPath(path, beginning, end)
  }

  def findPath(path: Map[Location, Location], beginning: Location, end: Location): List[Location] = {

    def process(path: Map[Location, Location], beginning: Location, current: Location, accumulator: List[Location]): List[Location] = {
      if(current.equals(beginning)) accumulator
      else {
        val breadcrumb: Location = path.getOrElse(current, beginning)
        val reducedMap = path - current
        process(reducedMap, beginning, breadcrumb, accumulator :+ current)
      }
    }
    (process(path, beginning, end, List()) :+ beginning).reverse
  }


  case class OrderedLocationWrapper(priority: Int, loc: Location) extends Ordered[OrderedLocationWrapper] {
    override def compare(other: OrderedLocationWrapper): Int = {
      other.priority - this.priority
    }
  }
  def heuristic(end: Location, next: Location): Int = {
    def calculateDistance(from: Location, to: Location): Long = {
      def deg2rad(deg: Double): Double = {
        deg * (Math.PI/180)
      }

      val R = 6371; // Radius of the earth in km
      val dLat = deg2rad(to.location.lat - from.location.lat)  // deg2rad below
      val dLon = deg2rad(to.location.lon - from.location.lon)
      val a =
        Math.sin(dLat/2) * Math.sin(dLat/2) +
          Math.cos(deg2rad(from.location.lat)) * Math.cos(deg2rad(to.location.lat)) *
            Math.sin(dLon/2) * Math.sin(dLon/2)

      val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
      val d = R * c; // Distance in km
      (d * 1000).toLong
    }
    calculateDistance(end, next).toInt
  }

  def getCostBetween(from: Location, to: Location): Int = {
    val route: Option[Route] = routesService.getRoute(from.id, to.id) match {
      case None => routesService.getRoute(to.id, from.id)
      case x => x
    }
    route map {_.distance.toInt} getOrElse Int.MaxValue
  }
  def graphNeighbours(location: Location, followFixedLinks: Boolean, followFreightLinks: Boolean): Set[Location] = {
    def fixedLinkFilter(r: Route): Boolean = {
      val isFixedLink: Boolean = r.srsCode.equals("Link")
      val fixedLinksAllowed: Boolean = if (!followFixedLinks) !isFixedLink else true
      fixedLinksAllowed
    }

    def freightLinkFilter(r: Route): Boolean = {
      val isFreightLink: Boolean = r.toc.equals("FRGT")
      val freightLinkAllowed: Boolean = if (!followFreightLinks) !isFreightLink else true
      freightLinkAllowed
    }

    val siblingRoutes: Set[Route] = routesService.getRoutes.filter ({

      r =>
        val fixedLinksAllowed: Boolean = fixedLinkFilter(r)
        val freightLinksAllowed: Boolean = freightLinkFilter(r)
        freightLinksAllowed && fixedLinksAllowed && (r.from.id.equals(location.id) || r.to.id.equals(location.id))
      }
    )
    val locs: Set[Location] =
      (siblingRoutes.map(_.from.id) ++ siblingRoutes.map {_.to.id})
        .filterNot(_.equals(location.id))
        .flatMap(l => locationsService.getLocation(l))

    locs
  }


}
