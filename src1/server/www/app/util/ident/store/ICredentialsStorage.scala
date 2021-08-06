package util.ident.store

import com.google.inject.ImplementedBy
import io.suggest.auth.UserProfile
import io.suggest.ext.svc.MExtService
import io.suggest.n2.node.MNode

import scala.concurrent.Future

/** Identity/credentials storage interface.
  * Created for migration from MNode.edges to LDAP credentials storage.
  */
@ImplementedBy( classOf[NodeEdgesStorage] )  // TODO Configure to to LDAP+NodeEdgesStorage.
trait ICredentialsStorage {

  /** Reset email and password for some existing person.
    *
    * @param personNode Person node.
    * @param regContext Registration credentials.
    * @return Future with saved node.
    */
  def updateEmailPw(personNode: MNode, regContext: MRegContext, emailFlag: Boolean): Future[MNode]

  /** Create, save and return new person node.
    *
    * @param nodeTechName Internal technical name for person node created.
    * @return Created and saved node.
    */
  def signUp(regContext: MRegContext, nodeTechName: Option[String] = None): Future[MNode]

  /** Password check against stored hash.
    *
    * @param personNode MNode of person for password check.
    * @param password   Password value.
    * @return Boolean.
    */
  def checkPassword(personNode: MNode, password: String): Future[Boolean]

  /** Password change action.
    * Method must also verify of old password correctness, if any.
    *
    * @param personNode MNode of person, where password must be changed.
    * @param password   New password.
    * @return Future with resulting MNode (possibly updated).
    */
  def resetPassword(personNode: MNode, password: String): Future[MNode]

  /** Search for some user in storage via login key (email/phone), and validating password client-side.
    *
    * @param emailOrPhone Login key from remote client: email or phone.
    * @param password     Password provided for login key.
    * @return Future with nodes found.
    */
  def findByEmailPhoneWithPw(emailOrPhone: String, password: String): Future[Seq[MNode]]

  /** Search for user by remote service user id.
    *
    * @param extService External service.
    * @param remoteUserId External service user id.
    * @return Node found, if any.
    */
  def findByExtServiceUserId(extService: MExtService, remoteUserId: String): Future[Seq[MNode]]

  /** Sign-up new user from external service.
    *
    * @param extService External service.
    * @param userProfile User profile data container.
    * @return Future with created and saved MNode.
    */
  def signUpExtService(extService: MExtService, userProfile: UserProfile, lang: Option[String] = None): Future[MNode]

  /** Does user have any ext-service login data?
    *
    * @param personId id of registered user.
    * @return true, if user have at least one stored external-service identity.
    *         false if no external-service data indexed, or if user not found.
    */
  def hasAnyExtServiceCreds(personId: String): Future[Boolean]

  /** Search for nodes with specified email credential.
    *
    * @param emails E-mail addresses.
    * @return Found nodes with such email identity.
    */
  def findByEmail(emails: String*): Future[Seq[MNode]]

}


/** Container of current registration/pwReset state data.
  *
  * @param password Plain-text user password
  */
final case class MRegContext(
                              password              : String,
                              phoneNumber           : Option[String]    = None,
                              email                 : Option[String]    = None,
                              lang                  : Option[String]    = None,
                            )
