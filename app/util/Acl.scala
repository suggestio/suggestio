package util

import play.api.mvc._
import play.api.mvc.Security.Authenticated
import controllers.routes
import java.sql.Connection
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
trait Acl {

  // Алиасы типов, используемых тут. Полезны для укорачивания объявления типов в заголовках функций,
  // предназначенных для проверки прав по технологии Action Composition.
  type ActionF_T        = Option[PersonWrapper] => Request[AnyContent] => Result
  type ActionF_BP_T[A]  = Option[PersonWrapper] => Request[A] => Result
  type AclF_T           = (Option[PersonWrapper], Request[AnyContent]) => Boolean
  type AclF_BP_T[A]     = (Option[PersonWrapper], Request[A]) => Boolean

  /**
   * Определить залогиненность юзера
   * @param request Заголовки запроса.
   * @return Номер юзера по таблице Person. Наличие юзера в таблице здесь не проверяется, потому что для этого нужен
   *         коннекшен к базе и далеко не всегда это необходимо.
   */
  protected def person(request: RequestHeader) : Option[PersonWrapper] = {
    request.session.get(Security.username) match {
      case Some(person_id_str) =>
        Some(new PersonWrapper(person_id_str.toInt))

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
   * Используется Some(PersonWrapper) вместо голого PersonWrapper для совместимости с другими функциями и шаблонами.
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
   * Контроллер-экшен, используемый для запрета выполнения редактирования профиля, если у юзера нет на это прав.
   * @param person_id чей профиль хотят редактировать
   * @param f генератор результата.
   * @return Result
   */
  protected def canUpdateUserProfileAction(person_id:Int)(f: ActionF_T) = can(f) {
    (pwOpt, request) => pwOpt.isDefined && can_update_user_profile_check(person_id, pwOpt.get.id)
  }

  /**
   * Непосредственная проверка полномочий на редактирование профиля. Тут код проверки.
   * Вынесен в отдельную функцию, ибо вызывается из нескольких мест с разными frontend-методами
   * @param person_id чей профиль хотят редактировать.
   * @param current_person_id id залогиненного юзера
   * @return true | false
   */
  protected final def can_update_user_profile_check(person_id:Int, current_person_id:Int) = {
    current_person_id == person_id || Acl.isAdmin(current_person_id)
  }


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
object Acl extends Acl {

  // Список админов ресурса
  val admins : Set[Int] = {
    current.configuration.getIntList("person.admins").map { integerList =>
      integerList.map(_.intValue()).toSet
    } getOrElse(Set(26))
  }

  def isAdmin(person_id:Int) = admins.contains(person_id)


  /**
   * Кто может редактировать профиль пользователя? Сам пользователь и админ. Метод обычно вызывается из шаблона для проверки
   * привелегий текущего юзера и рендера/нерендера тех или иных частей страницы.
   * @param person_id id редактируемого профиля
   * @param u Acl-контекст юзера, пришедший из maybeAuth..()
   * @return true | false
   */
  def canUpdateUserProfile(person_id:Int)(implicit u:Option[PersonWrapper]) : Boolean = u match {
    case Some(pw) => can_update_user_profile_check(person_id, pw.id)
    case None     => false
  }

}


// Враппер для прозрачного получения объекта Person из базы и пользования линками в другие модели в обход Person.
case class PersonWrapper(id:Int) extends MPersonLinks {
  private var _cache : MPerson = null

  /**
   * Выдать объект Person, возможно закешированный.
   * Считается, что необходимый Person есть в базе, иначе exception.
   * @param c
   * @return
   */
  def person(implicit c:Connection) : MPerson = _cache match {
    case null =>
      _cache = MPerson.getById(id).get
      _cache

    case cached => cached
  }

  protected def getIntId: Int = id
}

