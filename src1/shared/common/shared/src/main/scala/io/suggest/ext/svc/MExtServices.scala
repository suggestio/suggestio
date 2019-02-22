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
    override def hasAdvExt = true
  }


  case object FaceBook extends MExtService("fb") {
    override def mainPageUrl = "https://facebook.com/"
    override def nameI18N = "Facebook"
    override def hasAdvExt = true
  }


  case object Twitter extends MExtService("tw") {
    /** Ссылка на главную твиттера, и на собственный акк, если юзер залогинен. */
    override def mainPageUrl = "https://twitter.com/"
    override def nameI18N = "Twitter"
    override def hasAdvExt = true
    override def myUserName = Some("@suggest_io")
  }


  /** Единая система идентификация и авторизации.
    * Она глубоко интегрирована в гос.услуги, поэтому интегрируемся с гос.услугами.
    */
  case object GosUslugi extends MExtService("gu") {
    /** URL главной страницы сервиса. */
    override def mainPageUrl = "https://gosuslugi.ru/"
    override def nameI18N = MsgCodes.`GovServices.ESIA`
    /** Никакого размещения рекламы на гос.услугах нет. */
    override def hasAdvExt = false
  }


  override def values = findValues

}


/** Класс для одного элемента, описывающего внешний сервис. */
sealed abstract class MExtService(override val value: String) extends StringEnumEntry {

  /** URL главной страницы сервиса. */
  def mainPageUrl: String

  /** Отображамое имя, заданное через код в messages. */
  def nameI18N: String

  /** Код локализованного предложения "Я в_этом_сервисе" */
  def iAtServiceI18N: String =
    "I.at." + value

  /** Человекочитабельный юзернейм (id страницы) suggest.io на стороне сервиса. */
  def myUserName: Option[String] = None

  /** Доступна ли функция внешнего размещения карточки? Никогда не возвращает exception. */
  def hasAdvExt: Boolean

}

object MExtService {

  /** Поддержка play-json. */
  implicit def mExtServiceFormat: Format[MExtService] =
    EnumeratumUtil.valueEnumEntryFormat( MExtServices )

  @inline implicit def univEq: UnivEq[MExtService] = UnivEq.derive

}

