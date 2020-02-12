package io.suggest.ext.svc

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import io.suggest.i18n.MsgCodes
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.15 12:24
 * Description: Общий код клиентских и серверных моделей сервисов внешнего размещения.
 */
object MExtServices extends StringEnum[MExtService] {

  case object VKontakte extends MExtService("vk") {
    override def mainPageUrl = "https://vk.com/"
    override def nameI18N = "VKontakte"
  }


  case object FaceBook extends MExtService("fb") {
    override def mainPageUrl = "https://facebook.com/"
    override def nameI18N = "Facebook"
  }


  case object Twitter extends MExtService("tw") {
    /** Ссылка на главную твиттера, и на собственный акк, если юзер залогинен. */
    override def mainPageUrl = "https://twitter.com/"
    override def nameI18N = "Twitter"
  }


  /** Единая система идентификация и авторизации.
    * Она глубоко интегрирована в гос.услуги, поэтому интегрируемся с гос.услугами.
    */
  case object GosUslugi extends MExtService("gu") {
    /** URL главной страницы сервиса. */
    override def mainPageUrl = "https://gosuslugi.ru/"
    override def nameI18N = MsgCodes.`GovServices.ESIA`
  }


  /** Google Play (Market).
    *
    * id взят из спеки для portable web apps:
    * [[https://developer.mozilla.org/en-US/docs/Web/Manifest/related_applications]]
    */
  case object GooglePlay extends MExtService("play") {
    override def mainPageUrl = "https://play.google.com/"
    override def nameI18N = "Google Play"
  }


  /** Apple ITunes / App store.
    *
    * id взят из спеки для portable web apps:
    * [[https://developer.mozilla.org/en-US/docs/Web/Manifest/related_applications]]
    */
  case object AppleITunes extends MExtService("itunes") {
    override def mainPageUrl = "https://itunes.apple.com/"
    override def nameI18N = "Apple AppStore"
  }


  override def values = findValues


  def appDistrs: List[MExtService] =
    GooglePlay :: AppleITunes :: Nil

}


/** Класс для одного элемента, описывающего внешний сервис. */
sealed abstract class MExtService(override val value: String) extends StringEnumEntry {

  /** URL главной страницы сервиса. */
  def mainPageUrl: String

  /** Отображамое имя, заданное через код в messages. */
  def nameI18N: String

}

object MExtService {

  /** Поддержка play-json. */
  implicit def mExtServiceFormat: Format[MExtService] =
    EnumeratumUtil.valueEnumEntryFormat( MExtServices )

  @inline implicit def univEq: UnivEq[MExtService] = UnivEq.derive


  implicit final class MExtServicesOpsExt( private val extService: MExtService ) extends AnyVal {

    /** Доступна ли функция внешнего размещения карточки? Никогда не возвращает exception. */
    def hasAdvExt: Boolean = {
      extService match {
        case MExtServices.VKontakte | MExtServices.FaceBook | MExtServices.Twitter => true
        case _ => false
      }
    }

    /** Доступен ли логин в s.io через данный сервис? OAuth/OpenID/etc. */
    def hasLogin: Boolean = {
      extService match {
        case MExtServices.GosUslugi | MExtServices.VKontakte => true
        case _ => false
      }
    }

    /** Человекочитабельный юзернейм (id страницы) suggest.io на стороне сервиса. */
    def myUserName: Option[String] = {
      extService match {
        case MExtServices.Twitter => Some("@suggest_io")
        case _ => None
      }
    }

    /** Код локализованного предложения "Я в_этом_сервисе" */
    def iAtServiceI18N: String =
      "I.at." + extService.value

  }

}

