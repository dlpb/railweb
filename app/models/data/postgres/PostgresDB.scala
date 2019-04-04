package models.data.postgres

import java.net.URI
import java.sql.{Connection, DriverManager}

import com.typesafe.config.Config
import javax.inject.Inject
import models.auth.DaoUser

class PostgresDB @Inject() (config: Config) {


  private val createTableSQL =
    """CREATE TABLE IF NOT EXISTS users (
      |id BIGSERIAL PRIMARY KEY,
      |username VARCHAR(100) NOT NULL,
      |password VARCHAR(64) NOT NULL,
      |email VARCHAR(256),
      |roles VARCHAR(4096),
      |location_visits JSON,
      |route_visits JSON
      |)""".stripMargin

  private val ensureAdminUser =
    """
      |INSERT INTO users
      |    (username, password, roles)
      |SELECT 'admin', '226b92530c136015950c07ed4631257ff020ab807530aec6b9c0957374788fc', 'MapUser,VisitUser,AdminUser'
      |WHERE
      |    NOT EXISTS (
      |        SELECT username FROM users WHERE username='admin'
      |    );
    """.stripMargin

  private val updateLocationSql = "UPDATE users SET location_visits = ?::JSON where id = ?"
  private val updateRoutesSql = "UPDATE users SET route_visits = ?::JSON where id = ? "
  private val getRoutesSql = "SELECT route_visits FROM users WHERE id = ?"
  private val getLocationsSql = "SELECT location_visits FROM users WHERE id = ?"
  private val getUsersSql = "SELECT id, username, password, roles FROM users"
  private val createUserSql = "INSERT INTO users (username, password, roles) VALUES (?, ?, ?)"
  private val updateUserSql = "UPDATE users SET username = ?, password = ?, roles = ? WHERE id = ?"
  private val deleteUserSql = "DELETE FROM users WHERE id = ?"

  ensureDatabaseSetup()

  def ensureDatabaseSetup(): Unit = {
    val connection = getConnection(config)
    val statement = connection.createStatement()
    statement.executeUpdate(createTableSQL)
    statement.executeUpdate(ensureAdminUser)
    statement.close()
    connection.close()
  }

  def updateLocationsForUser(userId: Long, data: String): Unit = {
    println(s"Location Tracing saving data")
    try {
      val connection = getConnection(config)
      val statement = connection.prepareStatement(updateLocationSql)
      statement.setObject(1, data)
      statement.setLong(2, userId)
      statement.executeUpdate()
      statement.close()
      connection.close()
    }
    catch {
      case e: Exception => println(s"Location tracing exception $e")
    }
  }

  def updateRoutesForUser(userId: Long, data: String): Unit = {
    val connection = getConnection(config)
    val statement = connection.prepareStatement(updateRoutesSql)
    statement.setObject(1, data)
    statement.setLong(2, userId)
    statement.executeUpdate()
    statement.close()
    connection.close()
  }

  def getLocationsForUser(userId: Long): String = {
    val connection = getConnection(config)
    val statement = connection.prepareStatement(getLocationsSql)
    statement.setLong(1, userId)
    val resultSet = statement.executeQuery()
    val sb = new StringBuilder
    while(resultSet.next()){
      sb.append(resultSet.getString("location_visits"))
    }
    statement.close()
    connection.close()
    sb.toString()
  }

  def getRoutesForUser(userId: Long): String = {
    val connection = getConnection(config)
    val statement = connection.prepareStatement(getRoutesSql)
    statement.setLong(1, userId)
    val resultSet = statement.executeQuery()
    val sb = new StringBuilder
    while(resultSet.next()){
      sb.append(resultSet.getString("location_visits"))
    }
    statement.close()
    connection.close()
    sb.toString()
  }
  def createUser(user: DaoUser) = {
    val connection = getConnection(config)
    val statement = connection.prepareStatement(createUserSql)
    statement.setString(1, user.username)
    statement.setString(2, user.password)
    statement.setString(3, user.roles.mkString(","))
    statement.executeUpdate()
    statement.close()
    connection.close()
  }
  def updateUser(user: DaoUser) = {
    val connection = getConnection(config)
    val statement = connection.prepareStatement(updateUserSql)
    statement.setString(1, user.username)
    statement.setString(2, user.password)
    statement.setString(3, user.roles.mkString(","))
    statement.setLong(4, user.id)
    statement.executeUpdate()
    statement.close()
    connection.close()
  }

  def getUsers(): List[DaoUser] = {
    val connection = getConnection(config)
    val statement = connection.prepareStatement(getUsersSql)
    val resultSet = statement.executeQuery()
    var users: List[DaoUser] = List.empty[DaoUser]
    while(resultSet.next()){
      val id = resultSet.getLong("id")
      val username = resultSet.getString("username")
      val password = resultSet.getString("password")
      val roles = resultSet.getString("roles")
      users = DaoUser(id, username, password, roles.split(",").toSet) :: users
    }
    statement.close()
    connection.close()
    users
  }
  def deleteUserById(id: Long) = {
    val connection = getConnection(config)
    val statement = connection.prepareStatement(deleteUserSql)
    statement.setLong(1, id)
    statement.executeUpdate()
    statement.close()
    connection.close()
  }

  def getConnection(config: Config): Connection = {
    val dbUri = new URI(config.getString("postgres.db.url"))
    val username = dbUri.getUserInfo.split(":")(0)
    val password = dbUri.getUserInfo.split(":")(1)
    val dbUrl = s"jdbc:postgresql://${dbUri.getHost}${dbUri.getPath}"

    DriverManager.getConnection(dbUrl, username, password)
  }
}
