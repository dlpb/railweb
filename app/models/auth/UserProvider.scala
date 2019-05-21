package models.auth

trait UserProvider {
  def getUsers: Set[DaoUser]
  def updateUser(user: DaoUser)
  def createUser(username: String, password: String, roles: Set[String])
  def deleteUser(user: DaoUser)
}
