package models.auth

trait UserProvider {
  def getUsers: Set[DaoUser]
  def updateUser(user: DaoUser)
}
