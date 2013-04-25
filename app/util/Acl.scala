package util

import play.api.mvc._
import play.api.mvc.Security.{Authenticated, username}
import controllers.routes
import models.{MPersonLinks, MPerson}
import play.api.Play.current
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.03.13 11:55
 * Description: Security-функции для контроллеров. Например, определение залогиненности юзера.
 */

// Трайт для контроллеров. Содержит некоторые функции, используемые только кон
trait AclT {

  // Алиасы типов, используемых тут. Полезны для укорачивания объявления типов в заголовках функций,
  // предназначенных для проверки прав по технологии Action Composition.
  type PwOptT           = Option[PersonWrapper]
  type ActionF_T        = PwOptT => Request[AnyContent] => Result
  type ActionF_BP_T[A]  = PwOptT => Request[A] => Result
  type AclF_T           = (PwOptT, Request[AnyContent]) => Boolean
  type AclF_BP_T[A]     = (PwOptT, Request[A]) => Boolean

  /**
   * Определить залогиненность юзера
   * @param request Заголовки запроса.
   * @return Номер юзера по таблице Person. Наличие юзера в таблице здесь не проверяется, потому что для этого нужен
   *         коннекшен к базе и далеко не всегда это необходимо.
   */
  protected def person(request: RequestHeader) : PwOptT = {
    request.session.get(username) match {
      case Some(person_id_str) => Some(new PersonWrapper(person_id_str))
      case None => None
    }
  }


  /**
   * Эта функция вызывается для генерации ответа, когда выполнение какого-то действия в контроллере запрещено по причине
   * незалогиненности юзера. В данном случае, должен происходить редирект на форму логина.
   * request с пометкой implicit
   * @param request Заголовки запроса.
   * @return Result, такой же как и в экшенах контроллеров.
   */
  protected def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.index())

  /**
   * Контроллер использует isAuthentificated вместо Action когда необходимо ограничить доступ к ресурсу неавторизованным
   * юзерам. Все анонимусы будут получать в ответ результат функции onUnauthorized(request).
   * Пример кода:
   *    def index = isAuthenticated { implicit person_wrapper_opt => implicit request =>
   *      Ok("Hello " + username)
   *    }
   * @param f Функция генерации ответа, выполняемая когда юзер залогинен.
   * @return Ответ
   */
  protected def isAuthenticated(f: ActionF_T) = {
    Authenticated(person, onUnauthorized) { user =>
      Action(request => f(Some(user))(request))
    }
  }

  /**
   * Юзер может быть как авторизован, так и нет. Пример экшена в контроллере:
   *
   *    def index = maybeAuthenticated { person_option => implicit request =>
   *        val name = person_option match {
   *          case Some(person) => person.first_name
   *          case None         => "anonymous"
   *        }
   *        Ok("Hello, " + name)
   *    }
   * @param f
   * @return
   */
  protected def maybeAuthenticated(f: ActionF_T) = {
    Action(request => f(person(request))(request))
  }

  protected def maybeAuthenticatedBP[A](bodyParser:BodyParser[A])(f: ActionF_BP_T[A]) = {
    Action(bodyParser)(request => f(person(request))(request))
  }

  /**
   * Только для анонимуса. Остальным NotFound выдавать.
   * @param f request-обработчик
   * @return
   */
  protected def isAnon(f: Request[AnyContent] => Result) = {
    Action { request =>
      person(request) match {
        case None     => f(request)
        case Some(pw) => Results.NotFound
      }
    }
  }

  /**
   * Экшен, доступный только админу.
   * @param f тело экшена
   * @return
   */
  protected def isAdminAction(f: ActionF_T) = can(f) {
    (pwOpt, request) => pwOpt.isDefined && pwOpt.get.isAdmin
  }


  //
  // ACL-правила для сайта. Описывают, кто что может делать. Вызываются из контроллеров.
  //

  /**
   * can() - Action-функция абстрагированная от логики проверки. Это нужно для дедубликации кода между различными методами can...()(f)
   * Сама проверка передается в виде функции aclF/2. Если текущий юзер - админ, то проверка aclF не вызывается.
   * @param actionF функция генерации результат экшена. Будет вызвана, если юзер залогинен и aclF вернет true.
   * @param aclF Функция проверки прав. Если возвращает false, то вызывается onUnauthorized()
   * @return Result.
   */
  protected final def can(actionF: ActionF_T)(aclF: AclF_T) = {
    Action { request =>
      val pwOpt = person(request)
      if ((pwOpt.isDefined && pwOpt.get.isAdmin) || aclF(pwOpt, request))
        actionF(pwOpt)(request)
      else
        onUnauthorized(request)
    }
  }


  /**
   * can-экшен, но с BodyParser-ом
   * @param bodyParser BodyParser.
   * @param actionF экшен, генератор ответа.
   * @param aclF функция проверки прав.
   * @tparam A параметр типа боди-парсера.
   * @return
   */
  protected final def canBP[A](bodyParser:BodyParser[A])(actionF: ActionF_BP_T[A])(aclF: AclF_BP_T[A]) = {
    Action(bodyParser) { request =>
      val pwOpt = person(request)
      if ((pwOpt.isDefined && pwOpt.get.isAdmin) || aclF(pwOpt, request))
        actionF(pwOpt)(request)
      else
        onUnauthorized(request)
    }
  }

} // END Acl


// Объект для прямого обращения к функция трайта
object Acl extends AclT {

  // Список админов ресурса
  val admins : Set[Int] = {
    current.configuration.getIntList("person.admins").map { integerList =>
      integerList.map(_.intValue()).toSet
    } getOrElse(Set(26))
  }

  def isAdmin(person_id:Int) = admins.contains(person_id)

}


/**
 * PersonWrapper нужен для ленивого доступа к данным. Часто содержимое MPerson не нужно, поэтому зачем его читать сразу?
 * @param email id юзера
 */
case class PersonWrapper(email:String) extends MPersonLinks {
  lazy val person = MPerson.getByEmail(email).get
}

