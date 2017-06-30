package io.suggest.www.util.sec

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import io.suggest.sec.m.msession.Keys._
import io.suggest.sec.m.msession.{Keys, LoginTimestamp}
import io.suggest.util.logs.MacroLogsImpl
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.06.14 17:57
  * Description: Функции для сброса сессии при наступлении таймаута, и соотв.утиль для пролонгации сессии.
  *
  * 2014.feb.06: Из-за добавления в сессию securesocial, csrf-token и т.д. нужно аккуратнее работать с сессией,
  * без использования withNewSession().
  *
  * 2017.feb.28: Был трейт. Но он был довольно неудобен, т.к. вызывал необходимость создания лишних трейтов
  * и наследований в ActionBuilder'ах. После рефакторинга получилась класс с утилью, глобальный play-фильтр
  * и локальный Action, делающий тоже самое на уровне отдельных экшенов.
  *
  * Можно использовать так:
  * - Прописать один глобальный play-фильтр [[ExpireSessionFilter]] и забыть про эту подсистему.
  * - Без фильтра: везде втыкать action, как было раньше с трейтом.
  */


/** Утиль для v2 API обработки сессий в состоянии. */
@Singleton
class ExpireSessionUtil @Inject() (
                                    implicit private val ec    : ExecutionContext
                                  )
  extends MacroLogsImpl
{

  /** Не обновлять timestamp сессии, если прошлый выставлен не позднее указанного кол-ва секунд
    * относительно текущего момента. */
  def GUESS_SESSION_AS_FRESH_SECONDS = 30

  /** Ключи в session, которые нужно удалять при любой проблеме с TTL. */
  val filteredKeySet = {
    Keys.onlyLoginIter
      .map(_.name)
      .toSet
  }

  def clearSessionLoginData(session0: Session): Session = {
    session0.copy(
      data = session0
        .data
        .filterKeys { k => !filteredKeySet.contains(k) }
    )
  }

  def hasLoginData(session: Session): Boolean = {
    session.data
      .keysIterator
      .exists { filteredKeySet.contains }
  }

  // Тут был код, патчащий исходный реквест. Но как оказалось, нельзя пропатчить сессию в реквесте: там lazy val.
  // Поэтому проверка TTL вернулась в SessionUtil.


  /** Статическая обработка результата работы экшенов на предмет сессии.
    *
    * @param request HTTP-реквест.
    * @param result Результат выполненного экшена.
    * @return Результат работы с поправленной сессией.
    */
  def processResult(request: RequestHeader, result: Result): Result = {
    val session0 = result.session(request)
    val tstampOpt = LoginTimestamp.fromSession(session0)

    // Отработать отсутствие таймштампа в сессии.
    if (tstampOpt.isEmpty) {

      if ( session0.data.contains(PersonId.name) ) {
        // Сессия была только что выставлена в контроллере. Там же и ttl выставлен.
        val session1 = session0 + (Timestamp.name -> LoginTimestamp.currentTstamp().toString)
        result.withSession(session1)

      } else {
        // Не заниматься ковырянием сессии, если юзер не залогинен.
        result
      }

    } else {

      // Есть timestamp сессии. Разобраться, что с ним надо делать...
      val currTstamp = LoginTimestamp.currentTstamp()
      val newTsOpt = tstampOpt
        // Отфильтровать устаревшие timestamp'ы.
        .filter { _.isTimestampValid(currTstamp) }

      newTsOpt.fold {
        // Таймштамп истёк -- стереть из сессии таймштамп и username.
        LOGGER.trace("invokeBlock(): Erasing expired session for person " + session0.get(PersonId.name))
        val session1 = clearSessionLoginData(session0)
        result.withSession( session1 )

      } { ts =>
        // Уже есть таймштамп, значит пора залить новый (текущий) таймштамп в сессию.
        // Не требуется пересобирать сессию, если она недостаточно устарела. Это снизит нагрузку на сервер при использовании session-фильтров.
        if ( Math.abs(currTstamp - ts.tstamp) <= GUESS_SESSION_AS_FRESH_SECONDS ) {
          // Не трогаем сессию. Она ещё недостаточно устарела.
          result
        } else {
          // Выставить новый timestamp, пересобрав/переподписав сессию:
          val session1 = ts.withTstamp(currTstamp)
            .addToSession(session0)
          result.withSession(session1)
        }
      }
    }
  }


  /** Дедубликация кода запуска экшена на исполнение. */
  private[sec] def _applyAction[R <: RequestHeader](request: R)(f: R => Future[Result]): Future[Result] = {
    for (result <- f(request)) yield {
      processResult(request, result)
    }
  }

}


/** Экшен, который можно использовать для заворачивания реквеста и резульата экшена.
  * Не используется с момента создания, реализован быстренько и на всякий случай.
  * Если так и не будет использоваться, то можно будет его удалить.
  */
@Singleton
class ExpireSessionAction @Inject() (
                                      dab                        : DefaultActionBuilder,
                                      expireSessionUtil          : ExpireSessionUtil
                                    ) {

  /** Завернуть экшен. */
  def apply[A](action: Action[A]): Action[A] = {
    dab.async(action.parser) { request =>
      expireSessionUtil._applyAction(request)(action.apply)
    }
  }

}


/** Глобальный фильтр для запросов и ответов на тему работы с TTL sio-сессияй. */
class ExpireSessionFilter @Inject() (
                                      expireSessionUtil          : ExpireSessionUtil,
                                      override implicit val mat  : Materializer
                                    )
  extends Filter
{

  override def apply(f: (RequestHeader) => Future[Result])(rh0: RequestHeader): Future[Result] = {
    expireSessionUtil._applyAction(rh0)(f)
  }

}

