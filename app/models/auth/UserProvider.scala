package models.auth

trait UserProvider {
  def getUsers: Set[DaoUser]
  def updateUser(user: DaoUser): Unit
  def createUser(username: String, password: String, roles: Set[String]): Unit
  def deleteUser(user: DaoUser): Unit
}
