import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import data.{LocationMapBasedDataProvider, RouteMapBasedDataProvider}
import models.data.{LocationDataProvider, RouteDataProvider}
import models.list.ListService
import models.location.LocationsService
import models.route.{Route, RoutePoint, RoutesService}
import org.scalatest.{FlatSpec, Matchers}



class ListTest  extends FlatSpec with Matchers {
  val locationsService = new LocationsService(config, locationDataSource)
  val routesService = new RoutesService(config, routeDatasource)

  "List Service" should "list a route between two points that are next to each other in from, to direction" in {
    val from = locationsService.getLocation("CTH").get
    val to = locationsService.getLocation("RMF").get

    val route = new ListService(routesService, locationsService).list(from, to)

    route map {
      _.id
    } should be(List("CTH", "RMF"))
  }

  it should "find a route between two points that have a point in the middle in from,to direction" in {
    val from = locationsService.getLocation("WHR").get
    val to = locationsService.getLocation("BSO").get

    val route = new ListService(routesService, locationsService).list(from, to)

    route map {_.id} should be(List("WHR", "LAI", "BSO"))
  }

  it should "find a route between two points that have a non station point in the middle in from,to direction" in {
    val from = locationsService.getLocation("BET").get
    val to = locationsService.getLocation("CBH").get

    val route = new ListService(routesService, locationsService).list(from, to)

    route map {_.id} should be(List("BET", "BTHNLGN", "CBH"))
  }

  it should "find a route between two points that starts from a junction, in from,to direction" in {
    val from = locationsService.getLocation("RMF").get
    val to = locationsService.getLocation("GDP").get

    val route = new ListService(routesService, locationsService).list(from, to)

    route map {_.id} should be(List("RMF", "GDP"))
  }

  it should "find a route between two points with a junction in between in from, to direction" in {
    val from = locationsService.getLocation("HRO").get
    val to = locationsService.getLocation("CTH").get

    val route = new ListService(routesService, locationsService).list(from, to)

    route map {_.id} should be(List("HRO", "GIDEPKJ", "GDP", "RMF", "CTH"))
  }

  it should "not travel down links when directed" in {
    val from = locationsService.getLocation("HRW").get
    val to = locationsService.getLocation("PAD").get

    val route = new ListService(routesService, locationsService).list(from, to, followFixedLinks = false)

    route map {_.id} should be(List("HRW", "KNT", "SOK", "NWB", "WMB", "WLSDNBJ", "WLSDNN7", "ACTCWHF", "ACWLJN", "AML", "FRIARSJ", "LDBRKJ", "PRTOBJP", "PAD"))
  }

  it should "not travel down freight links when directed" in {
    val from = locationsService.getLocation("HRW").get
    val to = locationsService.getLocation("PAD").get

    val route = new ListService(routesService, locationsService).list(from, to, followFreightLinks = false)

    route map {_.id} should be(List("HRW", "KNT", "SOK", "NWB", "WMB", "SBP", "HDN", "WIJ", "KNL", "QPW", "LUBKIP", "LUBMAV", "LUBWAA", "ZPN", "LUCERCL", "ZBS", "BSZ", "PAD"))

  }

  it should "calculate a long route" in {
    val from = locationsService.getLocation("KGX").get
    val to = locationsService.getLocation("ABD").get

    val route = new ListService(routesService, locationsService).list(from, to)

    route map {_.id} should be(List("KGX", "KNGXBEL", "KNGXFTJ", "HLWYSJ", "FPK", "HGY", "HRN", "AAP", "NSG", "OKL", "NBA", "HDW", "PBR", "BPK", "WMG", "WELHMXO", "HAT", "WGC", "DIGSWEL", "WLW", "WLMRGRN", "KBW", "LNGYJN", "SVG", "HIT", "HITCHCJ", "ARLSCAD", "ARL", "BIW", "SDY", "LBRFTMP", "SNO", "HUN", "HNTNNJN", "CNNGABR", "CNNGSJN", "FLETTON", "PBO", "WRNGTNJ", "HELPSTN", "TALNGTN", "SOKELBY", "SOKEJN", "HGHDJN", "GRA", "GTHMNBJ", "GTHMNJN", "BRKSTSJ", "CLPLLP", "NNG", "NWRKD83", "NWRKFC", "CRLTOTL", "TUXFDWJ", "GRRDGAM", "RET", "RANSKLL", "RANSBAW", "DONCLCJ", "DONCBCJ", "DONCPCJ", "DONCBJN", "DONCSJJ", "DON", "DONCMRG", "ARKSEYL", "BTLYXO", "SHFTHLJ", "JNCROFT", "JNCRMOS", "HCKPBAL", "TEMPLHJ", "HAMBLSJ", "HAMBLNJ", "HAMBRYV", "COLTONJ", "COLTONN", "YORKHLJ", "YORKYSJ", "YORKYSF", "SKELTON", "SKELTNB", "SKELBRO", "TOLERTN", "TOLEPIL", "THI", "LNGLNDJ", "NTR", "NLRTWDR", "NLRTDBW", "DLTNECX", "DLTNS", "DAR", "DLTNN", "FYHLAYC", "FYHLPRM", "FYHLSJN", "THRISTE", "DHM", "CLS", "CLSTOXO", "BRTLYJN", "TYNEY", "LOWFELJ", "KEBGSJN", "KEBGNJN", "NCL", "MAS", "HTONSJN", "HTONNJN", "BENTON", "NSHMSEG", "NSHMLC", "BDLNTNX", "WSLKWXJ", "BDLNMHJ", "ASHGTNJ", "BUTRFHH", "BUTRDCR", "WDD", "CHVNNC", "ACK", "WNGTJN", "ALM", "ALNMILL", "CHHLCHR", "CHT", "BEALSMA", "BEAL", "TWEDMSB", "BWK", "BUMOUTH", "RESTON", "GTHSCE", "CCBNPH", "INNERWK", "TORNGSP", "DUN", "STENTON", "DRM", "LND", "PST", "WAF", "MNKTNHJ", "MUB", "PORTOBL", "CRGNTPS", "EDB", "HYM", "HAYMREJ", "HAYMRCJ", "HAYMRWJ", "SGL", "EGY", "DAM", "NQU", "IVRKSJN", "INK", "IVRKEJN", "DAG", "AUR", "BTS", "KGH", "KDY", "THRN568", "MNC", "LDY", "SPF", "CUP", "LEU", "TAYBDGS", "DUNDCJ", "DEE", "CMPRDNJ", "BYF", "BSI", "MON", "BYL", "GOF", "CAN", "ARB", "INVKLOR", "USAN", "MTS", "CRAIGO", "LAU", "CAARMNT", "SHVNS", "STN", "PLN", "CVEBAY", "CRGISTH", "ABRDFJN", "ABD"))
  }

  it should "find a route between two points that are next to each other in the to,from direction" in {
    val from = locationsService.getLocation("RMF").get
    val to = locationsService.getLocation("CTH").get

    val route = new ListService(routesService, locationsService).list(from, to)

    route map {_.id} should be(List("RMF", "CTH"))
  }

  it should "work" in {
    val from = locationsService.getLocation("PNZ").get
    val to = locationsService.getLocation("WCK").get

    val route = new ListService(routesService, locationsService).list(from, to)
    println("Got an answer")
    route map {r => s"${r.id} - ${r.name}"} foreach println
  }

  private def config = {
    val path = getClass().getResource("routes.json").getPath
    val config = ConfigFactory
      .empty()
      .withValue("data.static.root", ConfigValueFactory.fromAnyRef(
        path.substring(0, path.lastIndexOf("/"))
      ))
    config
  }

  def routeDatasource: RouteMapBasedDataProvider = {
    new RouteMapBasedDataProvider() with RouteDataProvider
  }

  def locationDataSource: LocationMapBasedDataProvider = {
    new LocationMapBasedDataProvider() with LocationDataProvider
  }
}
