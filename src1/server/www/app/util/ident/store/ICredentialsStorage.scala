package util.ident.store

import com.google.inject.ImplementedBy
import io.suggest.auth.UserProfile
import io.suggest.common.empty.OptionUtil
import io.suggest.ext.svc.MExtService
import io.suggest.n2.edge.{MEdge, MEdgeDoc, MEdgeInfo, MNodeEdges, MPredicates}
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.{MNode, MNodeTypes}
import io.suggest.n2.node.common.MNodeCommon
import io.suggest.n2.node.meta.{MBasicMeta, MMeta}
import japgolly.univeq._

import scala.concurrent.Future

/** Identity/credentials storage interface.
  * Created for copy some active credentials data from MNode.edges to LDAP credentials storage.
  *
  * Primary credentials storage remains to be inside edges, because non-trivial nested-objects schema with indexed
  * sub-fields cannot be represented inside LDAP, and LDAP is have only copy of some data, that may be needed by other
  * non-web services (like e-mail servers pair: postfix/dovecot).
  *
  * Main purpose of [[ICredentialsStorage]] interface: provide *composable* APIs for different credentials storage
  * backends, so credentials (or parts of them) may be stored in several areas simulateously.
  */
@ImplementedBy( classOf[NodeEdgesCredentials] )  // TODO Configure to to LDAP+NodeEdgesStorage.
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

  /** Return user's emails. */
  def findEmailsOfPerson(personNode: MNode, limit: Int = 0): Future[Seq[String]]

  /** Return emails for userId. */
  def findEmailsOfPersonId(personId: String, limit: Int = 0): Future[Seq[String]]

  /** Return user's emails, where access to user node via request.user.personNodeOptFut is preferred. */
  def findEmailsOfPersonIdFut(personId: String, personOptFut: => Future[Option[MNode]], limit: Int = 0): Future[Seq[String]]


  /** Unified API method for accessing and altering credentials data. */
  //def credAction( args: MCredsActionArgs ): Future[MCredsActionResult]

  /** Save credentials into backend storage(s) (into MNode.edges, LDAP, etc).
    *
    * @param intoNode Right() => Save into existing node.
    *                 Left(nodeTechName) => Create new node. TODO Make also optional MNode here?
    *                 "None" will throw exception, if selected backend cannot create new nodes, and new node
    *                 expected to be created by previous backend.
    * @param creds Credentials data for writing.
    * @param append true = append new credential(s).
    *               false = forget previous credentials (only with predicates in creds array) and append new credentials list.
    * @return Node created or updated after saving.
    *         If some storage backend cannot return MNode instances, it must returns intoNode.get value.
    */
  def write( intoNode: Option[MNode], creds: Seq[MEdge], append: Boolean ): Future[MNode]

  /** Read credentials for node from storage backend.
    *
    * @param personNode Person node.
    * @param criterias Credentials criterias looking for.
    * @return Credentials edges found in nodes.
    */
  def get( personNode: MNode, criterias: Seq[Criteria] ): Future[Seq[MEdge]]

  /** Search by credentials inside indexed storage.
    *
    * @param criterias Search criterias.
    * @param personIds Limit ids of person node searching for.
    * @param limit Maximum count of results returned. 10 by default.
    * @return Node ids found.
    *         To extract criterias found, use mNodes.{getByIdCache() | multiGetCacheSec()} + this.get(MNode, criterias).
    */
  //def search( criterias: Seq[Criteria],
  //            personIds: Seq[String] = Nil,
  //            limit: Option[Int] = Some(10) ): Future[Seq[String]]

}


object ICredentialsStorage {

  /** Make email edge. */
  def emailEdge(flag: Boolean, emails: String*): MEdge = {
    MEdge(
      predicate = MPredicates.Ident.Email,
      nodeIds = emails.toSet,
      info = MEdgeInfo(
        flag = OptionUtil.SomeBool( flag ),
      ),
    )
  }

  /** Make password edge for writing into storages. */
  def passwordEdgeBeforeWrite(password: String): MEdge = {
    MEdge(
      predicate = MPredicates.Ident.Password,
      // Instance have unencrypted password inside text field. So, every storage backend implementation
      // could properly hash password depending on available algo's, supported by single storage backend implementation.
      doc = MEdgeDoc(
        text = Some( password ),
      ),
    )
  }
  def passwordEdgeOnNodeWrite(passwordHash: String): MEdge = {
    MEdge(
      predicate = MPredicates.Ident.Password,
      info = MEdgeInfo(
        textNi = Some( passwordHash ),
      )
    )
  }

  /** Make new edge for phone number. */
  def phoneEdge(phoneNumber: String): MEdge = {
    MEdge(
      predicate = MPredicates.Ident.Phone,
      nodeIds   = Set.empty + phoneNumber,
      info = MEdgeInfo(
        flag = OptionUtil.SomeBool.someTrue,
      )
    )
  }


  /** Make new empty default node instance, based on credentials. */
  def personNode(creds: Seq[MEdge]): MNode = {
    MNode(
      common = MNodeCommon(
        ntype       = MNodeTypes.Person,
        isDependent = false,
        isEnabled   = true,
      ),
      meta = MMeta(
        basic = MBasicMeta(
          langs    = creds
            .iterator
            .filter(_.predicate ==* MPredicates.Ident.Lang)
            .flatMap(_.nodeIds)
            .toList
            .distinct,
          // no names here: name will be dynamically generated based on idents using guessDisplayName() methods.
        ),
      ),
      edges = MNodeEdges(
        out = creds,
      ),
    )
  }

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
