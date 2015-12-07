package models.req

import models.event.MEvent
import models.event.search.MEventsSearchArgs
import models.jsm.init.MTarget
import models.mbill.MBalance
import play.api.Play.current
import play.api.db.DB
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.acl.PersonWrapper
import util.acl.PersonWrapper._
import util.async.AsyncUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.04.15 14:04
 * Description: Модель метаданных, относящихся запросу. Сюда попадают данные, которые необходимы везде и требует
 * асинхронных действий.
 * Вынесена из util.acl из RequestWrappers.scala.
 */

object SioReqMd {

  def empty: SioReqMd = apply()

  /** Простая генерация srm на основе юзера. */
  def fromPwOpt(pwOpt: PwOpt_t, jsInitTargets: Seq[MTarget] = Nil): Future[SioReqMd] = {
    PersonWrapper.findUserName(pwOpt) map { usernameOpt =>
      SioReqMd(
        usernameOpt   = usernameOpt,
        jsInitTargets0 = jsInitTargets
      )
    }
  }

  /** Генерация srm для юзера в рамках личного кабинета. */
  def fromPwOptAdn(pwOpt: PwOpt_t, adnId: String, jsInitTargets: Seq[MTarget] = Nil): Future[SioReqMd] = {
    // Получить кол-во непрочитанных сообщений для узла.
    val newEvtsCntFut: Future[Int] = {
      val args = MEventsSearchArgs(
        ownerId     = Some(adnId),
        onlyUnseen  = true
      )
      MEvent.dynCount(args)
        .map { _.toInt }
    }
    // Получить баланс узла.
    val bbOptFut = Future {
      DB.withConnection { implicit c =>
        MBalance.getByAdnId(adnId)
      }
    }(AsyncUtil.jdbcExecutionContext)
    // Собрать результат.
    for {
      usernameOpt <- PersonWrapper.findUserName(pwOpt)
      bbOpt       <- bbOptFut
      newEvtCnt   <- newEvtsCntFut
    } yield {
      SioReqMd(
        usernameOpt       = usernameOpt,
        billBallanceOpt   = bbOpt,
        nodeUnseenEvtsCnt = Some(newEvtCnt),
        jsInitTargets0     = jsInitTargets
      )
    }
  }

}


/** Интерфейс модели. */
trait ISioReqMd {
  /** Отображаемое имя юзера, если есть. Формируются на основе данных сессии и данных из
    * MNode и моделей [[models.usr.MPersonIdent]]. */
  def usernameOpt       : Option[String]

  /** Текущий денежный баланс текущего узла. */
  def billBallanceOpt   : Option[MBalance]

  /** Кол-во новых событий у узла. */
  def nodeUnseenEvtsCnt : Option[Int]

  /** 2015.apr.29: Поддержка дополнительных целей js-инциализации, передается в action builder через конструктор. */
  def jsInitTargets0     : Seq[MTarget]
}


/** Враппер для опционального значения [[ISioReqMd]]. */
trait SioReqMdOptWrapper extends ISioReqMd {
  /** Завернутое опциональное значение. */
  def sioReqMdOpt: Option[ISioReqMd]

  override def usernameOpt        = sioReqMdOpt.flatMap(_.usernameOpt)
  override def billBallanceOpt    = sioReqMdOpt.flatMap(_.billBallanceOpt)
  override def nodeUnseenEvtsCnt  = sioReqMdOpt.flatMap(_.nodeUnseenEvtsCnt)
  override def jsInitTargets0: Seq[MTarget] = {
    sioReqMdOpt match {
      case Some(v)  => v.jsInitTargets0
      case None     => Nil
    }
  }
}


/** Враппер для [[ISioReqMd]]. */
trait SioReqMdWrapper extends ISioReqMd {
  def sioReqMd: ISioReqMd

  override def usernameOpt        = sioReqMd.usernameOpt
  override def billBallanceOpt    = sioReqMd.billBallanceOpt
  override def nodeUnseenEvtsCnt  = sioReqMd.nodeUnseenEvtsCnt
  override def jsInitTargets0     = sioReqMd.jsInitTargets0
}


/** Дефолтовая реализация экземпляра модели [[ISioReqMd]] */
case class SioReqMd(
  override val usernameOpt       : Option[String]       = None,
  override val billBallanceOpt   : Option[MBalance]     = None,
  override val nodeUnseenEvtsCnt : Option[Int]          = None,
  override val jsInitTargets0    : Seq[MTarget]         = Nil
)
  extends ISioReqMd

